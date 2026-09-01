[CmdletBinding()]
param(
    [ValidateSet('Offline', 'ModelEnabled')]
    [string]$DeploymentMode = 'Offline',
    [ValidateSet('Evidence', 'PublicDeployment')]
    [string]$Purpose = 'Evidence',
    [string]$ProviderEvidence,
    [string]$PlatformAttestation,
    [switch]$AllowDirty,
    [switch]$SkipDependencyAudits
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$policyPath = Join-Path $root 'release/l1-release-policy.json'
$policy = Get-Content -Raw -LiteralPath $policyPath | ConvertFrom-Json
$evidenceRoot = Join-Path $root 'target/release-evidence'
New-Item -ItemType Directory -Force -Path $evidenceRoot | Out-Null

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) {
        throw $Message
    }
}

function Read-PassingReport {
    param([string]$RelativePath, [string]$Name, [string]$Commit)
    $path = Join-Path $root $RelativePath
    Assert-True (Test-Path -LiteralPath $path -PathType Leaf) "$Name report is missing: $RelativePath"
    $report = Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
    Assert-True ($report.status -eq 'PASS') "$Name report does not pass."
    Assert-True ($report.gitCommit -eq $Commit) "$Name report was not generated from commit $Commit."
    return $report
}

Push-Location $root
try {
    $commit = (& git rev-parse HEAD).Trim()
    if (-not $AllowDirty) {
        $dirty = @(& git status --porcelain | Where-Object { $_ -notmatch '^\?\? tmp[/\\]' })
        Assert-True ($dirty.Count -eq 0) 'Release verification requires a clean worktree except user-owned tmp/.'
    }

    & docker compose config --quiet
    Assert-True ($LASTEXITCODE -eq 0) 'Compose configuration is invalid.'
    & git diff --check
    Assert-True ($LASTEXITCODE -eq 0) 'Git whitespace validation failed.'

    $aiArguments = @{ DeploymentMode = $DeploymentMode }
    if (-not [string]::IsNullOrWhiteSpace($ProviderEvidence)) {
        $aiArguments.ProviderEvidence = $ProviderEvidence
    }
    & (Join-Path $root 'scripts/evaluation/run-ai-safety-evaluation.ps1') @aiArguments

    if (-not $SkipDependencyAudits) {
        & (Join-Path $root 'scripts/security/osv-audit.ps1') -FailOn HIGH
        & npm --prefix web audit --audit-level=high
        Assert-True ($LASTEXITCODE -eq 0) 'Frontend dependency audit failed.'
    }

    $ai = Read-PassingReport $policy.requiredReports.aiSafety 'AI safety' $commit
    $demo = Read-PassingReport $policy.requiredReports.demo 'Demo seed' $commit
    $load = Read-PassingReport $policy.requiredReports.load 'L1 load' $commit
    $restore = Read-PassingReport $policy.requiredReports.restore 'Restore' $commit
    $rollback = Read-PassingReport $policy.requiredReports.rollback 'Rollback' $commit

    Assert-True ($ai.deploymentMode -eq $DeploymentMode) 'AI report deployment mode does not match this release.'
    Assert-True ($demo.profileVerified -and $demo.trackedDays -eq 7) 'Demo report lacks the complete API-created workflow.'
    Assert-True ($load.maxInteractiveSessions -eq $policy.capacity.interactiveSessions -and
        $load.observedMaxVus -ge $policy.capacity.interactiveSessions -and
        $load.concurrentSessionSeconds -ge $policy.capacity.interactiveSessionSeconds -and
        $load.interactiveRequests -gt 0 -and $load.interactiveErrorRate -lt 0.01) 'Concurrent interactive-session evidence is incomplete.'
    Assert-True ($load.sustainedRps -eq $policy.capacity.sustainedRps -and
        $load.sustainedSeconds -ge $policy.capacity.sustainedSeconds) 'Sustained load evidence is too short or uses the wrong rate.'
    Assert-True ($load.burstRps -eq $policy.capacity.burstRps -and
        $load.burstSeconds -ge $policy.capacity.burstSeconds) 'Burst load evidence is too short or uses the wrong rate.'
    Assert-True ($load.sustainedP95Ms -le $policy.capacity.p95LimitMs -and
        $load.burstP95Ms -le $policy.capacity.p95LimitMs -and
        $load.interactiveP95Ms -le $policy.capacity.p95LimitMs) 'Load p95 exceeds the L1 limit.'
    Assert-True ($load.sustainedP99Ms -le $policy.capacity.p99LimitMs -and
        $load.burstP99Ms -le $policy.capacity.p99LimitMs -and
        $load.interactiveP99Ms -le $policy.capacity.p99LimitMs) 'Load p99 exceeds the L1 limit.'
    Assert-True ($load.sustainedErrorRate -lt 0.01 -and $load.burstErrorRate -lt 0.01 -and
        $load.sustainedDroppedIterations -eq 0 -and $load.burstDroppedIterations -eq 0) 'Load errors or dropped iterations exceed the L1 limit.'
    Assert-True ($restore.mysqlImage -eq $policy.recovery.mysqlImage -and
        $restore.freshVolume -and $restore.observedSnapshotDataLossRows -eq 0 -and
        $restore.rpoGatePassed -and $restore.rtoGatePassed -and
        -not $restore.rawBackupRetainedInEvidence) 'Restore evidence violates the recovery boundary.'
    Assert-True ($rollback.candidateRejectedBeforeCutover -and
        $rollback.readinessVerifiedAfterRollback -and $rollback.recoveredKnownGoodImage) 'Rollback evidence is incomplete.'

    $requiredDocs = @(
        'docs/operations/data-retention-and-backup.md',
        'docs/operations/model-outage.md',
        'docs/operations/database-recovery.md',
        'docs/operations/security-incident.md',
        'docs/operations/release-and-rollback.md',
        'docs/RELEASE_CHECKLIST.md'
    )
    foreach ($document in $requiredDocs) {
        Assert-True (Test-Path -LiteralPath (Join-Path $root $document) -PathType Leaf) "Required runbook is missing: $document"
    }

    $pinnedSources = @('Dockerfile', 'web/Dockerfile', 'docker-compose.yml')
    foreach ($source in $pinnedSources) {
        $content = Get-Content -Raw -LiteralPath (Join-Path $root $source)
        Assert-True ($content -match '@sha256:[a-f0-9]{64}') "$source does not pin external images by digest."
    }

    $platform = $null
    if ($Purpose -eq 'PublicDeployment') {
        Assert-True (-not [string]::IsNullOrWhiteSpace($PlatformAttestation)) 'PublicDeployment requires -PlatformAttestation.'
        $platform = Get-Content -Raw -LiteralPath (Resolve-Path -LiteralPath $PlatformAttestation) | ConvertFrom-Json
        Assert-True ($platform.releaseCommit -eq $commit) 'Platform attestation commit is stale.'
        Assert-True ($platform.tlsConfigured -and $platform.managedSecretsConfigured -and
            $platform.hourlyEncryptedOffHostBackupConfigured -and
            $platform.alertDeliveryTested -and $platform.sloCollectionConfigured) 'Platform controls are not fully attested.'
        Assert-True ($platform.backupExpiryDays -eq $policy.recovery.backupExpiryDays) 'Platform backup expiry differs from policy.'
        Assert-True ($platform.backendImageDigest -match '^sha256:[a-f0-9]{64}$' -and
            $platform.webImageDigest -match '^sha256:[a-f0-9]{64}$') 'Published application image digests are missing.'
    }

    $artifactPaths = @($policy.requiredReports.PSObject.Properties.Value)
    $artifacts = @($artifactPaths | ForEach-Object {
        $path = Join-Path $root $_
        [ordered]@{
            path = $_
            sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash.ToLowerInvariant()
        }
    })
    $manifest = [ordered]@{
        schemaVersion = '1.0.0'
        status = 'PASS'
        releaseLevel = $policy.releaseLevel
        purpose = $Purpose
        deploymentMode = $DeploymentMode
        verifiedAt = [DateTimeOffset]::UtcNow.ToString('o')
        gitCommit = $commit
        platformAttested = $Purpose -eq 'PublicDeployment'
        enterpriseOrL2Claim = $false
        artifacts = $artifacts
    }
    $manifest | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8NoBOM `
        -LiteralPath (Join-Path $evidenceRoot 'release-manifest.json')
    Write-Host "L1 $Purpose release evidence passed for $DeploymentMode mode at commit $commit."
}
finally {
    Pop-Location
}
