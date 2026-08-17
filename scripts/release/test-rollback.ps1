[CmdletBinding()]
param(
    [ValidatePattern('^[a-z0-9][a-z0-9-]{2,40}$')]
    [string]$ProjectName = 'hbti-rollback-drill',
    [ValidateRange(1024, 65535)][int]$WebPort = 5275,
    [ValidateRange(1024, 65535)][int]$BackendPort = 8182,
    [string]$BaseUrl = 'http://localhost:5275',
    [switch]$SkipComposeStart,
    [switch]$KeepRunning
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$evidenceRoot = Join-Path $root 'target/release-evidence/rollback'
$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$candidateContainer = "hbti-invalid-candidate-$suffix"
$network = "${ProjectName}_default"
$started = $false
$succeeded = $false
$originalMysqlPassword = $env:MYSQL_PASSWORD
$originalWebPort = $env:WEB_PORT
$originalBackendPort = $env:BACKEND_PORT
if ([string]::IsNullOrWhiteSpace($env:MYSQL_PASSWORD)) {
    $env:MYSQL_PASSWORD = 'hbti-local-password'
}
$env:WEB_PORT = $WebPort.ToString()
$env:BACKEND_PORT = $BackendPort.ToString()

function Assert-NativeSuccess {
    param([string]$Operation)
    if ($LASTEXITCODE -ne 0) {
        throw "$Operation failed with exit code $LASTEXITCODE."
    }
}

function Assert-Readiness {
    $health = Invoke-RestMethod -Method Get -Uri "$($BaseUrl.TrimEnd('/'))/actuator/health/readiness" -TimeoutSec 15
    if ($health.status -ne 'UP') {
        throw "Known-good readiness is '$($health.status)', expected UP."
    }
}

function Wait-ContainerExit {
    param([string]$Container, [int]$TimeoutSeconds = 60)
    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        $running = (& docker inspect --format '{{.State.Running}}' $Container).Trim()
        Assert-NativeSuccess 'Inspect invalid candidate state'
        if ($running -eq 'false') {
            return
        }
        Start-Sleep -Seconds 1
    } while ([DateTimeOffset]::UtcNow -lt $deadline)
    throw 'Invalid candidate unexpectedly remained running.'
}

function Wait-BackendHealthy {
    param([int]$TimeoutSeconds = 120)
    $backend = (& docker compose --project-name $ProjectName ps --quiet backend).Trim()
    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        $health = (& docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $backend).Trim()
        if ($LASTEXITCODE -eq 0 -and $health -eq 'healthy') {
            return
        }
        Start-Sleep -Seconds 2
    } while ([DateTimeOffset]::UtcNow -lt $deadline)
    throw 'Known-good backend did not recover health before the rollback deadline.'
}

New-Item -ItemType Directory -Force -Path $evidenceRoot | Out-Null

Push-Location $root
try {
    if (-not $SkipComposeStart) {
        & docker compose --project-name $ProjectName up --build --detach --wait
        Assert-NativeSuccess 'Start isolated rollback environment'
        $started = $true
    }

    Assert-Readiness
    $backendContainer = (& docker compose --project-name $ProjectName ps --quiet backend).Trim()
    if ([string]::IsNullOrWhiteSpace($backendContainer)) {
        throw 'Known-good backend container is missing.'
    }
    $knownGoodImage = (& docker inspect --format '{{.Image}}' $backendContainer).Trim()
    Assert-NativeSuccess 'Resolve known-good image digest'

    $candidateStartedAt = [DateTimeOffset]::UtcNow
    & docker run --detach --name $candidateContainer --network $network `
        -e APP_PROFILE=offline `
        -e 'MYSQL_URL=jdbc:mysql://mysql:3306/hbti_coach?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong' `
        -e MYSQL_USERNAME=root -e MYSQL_PASSWORD `
        -e REDIS_URL=redis://redis:6379 `
        -e AUTH_SIGNING_KEY=invalid -e AUTH_SECURE_COOKIES=false `
        -e "CORS_ALLOWED_ORIGINS=$BaseUrl" `
        $knownGoodImage | Out-Null
    Assert-NativeSuccess 'Start deliberately invalid candidate'
    Wait-ContainerExit -Container $candidateContainer
    $candidateRejectedAt = [DateTimeOffset]::UtcNow
    $candidateExitCode = [int]((& docker inspect --format '{{.State.ExitCode}}' $candidateContainer).Trim())
    $candidateLogs = (& docker logs $candidateContainer 2>&1) -join "`n"
    $candidateLogs | Set-Content -Encoding utf8NoBOM -LiteralPath (Join-Path $evidenceRoot 'invalid-candidate.log')
    if ($candidateExitCode -eq 0 -or $candidateLogs -notmatch 'signing-key must contain at least 32 UTF-8 bytes') {
        throw 'Candidate was not rejected by the expected signing-key configuration gate.'
    }

    Assert-Readiness
    $rollbackStartedAt = [DateTimeOffset]::UtcNow
    & docker compose --project-name $ProjectName restart backend
    Assert-NativeSuccess 'Redeploy known-good backend configuration'
    Wait-BackendHealthy
    Assert-Readiness
    $rollbackVerifiedAt = [DateTimeOffset]::UtcNow

    $currentBackend = (& docker compose --project-name $ProjectName ps --quiet backend).Trim()
    $recoveredImage = (& docker inspect --format '{{.Image}}' $currentBackend).Trim()
    if ($recoveredImage -ne $knownGoodImage) {
        throw 'Rollback did not recover the recorded known-good image.'
    }

    $report = [ordered]@{
        schemaVersion = '1.0.0'
        status = 'PASS'
        testedAt = [DateTimeOffset]::UtcNow.ToString('o')
        gitCommit = (& git rev-parse HEAD).Trim()
        knownGoodImageId = $knownGoodImage
        invalidCandidateExitCode = $candidateExitCode
        candidateRejectedBeforeCutover = $true
        trafficCutoverAttempted = $false
        candidateRejectionSeconds = [math]::Round(($candidateRejectedAt - $candidateStartedAt).TotalSeconds, 3)
        rollbackRecoverySeconds = [math]::Round(($rollbackVerifiedAt - $rollbackStartedAt).TotalSeconds, 3)
        readinessVerifiedAfterRollback = $true
        recoveredKnownGoodImage = $recoveredImage -eq $knownGoodImage
    }
    $report | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8NoBOM `
        -LiteralPath (Join-Path $evidenceRoot 'report.json')
    $succeeded = $true
    Write-Host 'Invalid candidate was rejected before cutover and known-good readiness recovered.'
}
finally {
    & docker rm --force $candidateContainer 2>$null | Out-Null
    if ($null -eq $originalWebPort) {
        Remove-Item Env:WEB_PORT -ErrorAction SilentlyContinue
    } else {
        $env:WEB_PORT = $originalWebPort
    }
    if ($null -eq $originalBackendPort) {
        Remove-Item Env:BACKEND_PORT -ErrorAction SilentlyContinue
    } else {
        $env:BACKEND_PORT = $originalBackendPort
    }
    if ($null -eq $originalMysqlPassword) {
        Remove-Item Env:MYSQL_PASSWORD -ErrorAction SilentlyContinue
    } else {
        $env:MYSQL_PASSWORD = $originalMysqlPassword
    }
    if ($started -and -not $KeepRunning) {
        & docker compose --project-name $ProjectName down --volumes --remove-orphans
        if ($LASTEXITCODE -ne 0 -and $succeeded) {
            throw 'Rollback environment cleanup failed.'
        }
    }
    Pop-Location
}

if (-not $succeeded) {
    exit 1
}
