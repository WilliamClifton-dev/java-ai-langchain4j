[CmdletBinding()]
param(
    [ValidateSet('Offline', 'ModelEnabled')]
    [string]$DeploymentMode = 'Offline',
    [string]$ProviderEvidence
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$manifestPath = Join-Path $root 'evaluation/ai-safety/v1/manifest.json'
$promptRoot = Join-Path $root 'src/main/resources/prompts/hbti'
$evidenceRoot = Join-Path $root 'target/release-evidence/ai-safety'
New-Item -ItemType Directory -Force -Path $evidenceRoot | Out-Null

function Get-BundleHash {
    param([string]$Directory)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [System.Collections.Generic.List[byte]]::new()
        Get-ChildItem -LiteralPath $Directory -Recurse -File |
            Sort-Object FullName |
            ForEach-Object {
                $relative = [System.IO.Path]::GetRelativePath($Directory, $_.FullName).Replace('\', '/')
                $bytes.AddRange([System.Text.Encoding]::UTF8.GetBytes($relative + "`n"))
                $bytes.AddRange([System.IO.File]::ReadAllBytes($_.FullName))
                $bytes.Add(10)
            }
        return [Convert]::ToHexString($sha.ComputeHash($bytes.ToArray())).ToLowerInvariant()
    }
    finally {
        $sha.Dispose()
    }
}

Push-Location $root
try {
    & mvn --batch-mode --no-transfer-progress '-Dtest=AiSafetyEvaluationManifestTest,KnowledgeRetrievalEvaluationTest,KnowledgeRetrievalCapacityEvaluationTest,ReviewedKnowledgeRetrieverTest' test |
        Tee-Object -FilePath (Join-Path $evidenceRoot 'contract-tests.txt')
    if ($LASTEXITCODE -ne 0) {
        throw 'Mandatory offline AI/RAG evaluation failed.'
    }

    $manifestHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $manifestPath).Hash.ToLowerInvariant()
    $promptHash = Get-BundleHash -Directory $promptRoot
    $ragPerformancePath = Join-Path $evidenceRoot 'rag-performance.json'
    if (-not (Test-Path -LiteralPath $ragPerformancePath -PathType Leaf)) {
        throw 'RAG capacity evaluation did not produce evidence.'
    }
    $ragPerformance = Get-Content -Raw -LiteralPath $ragPerformancePath | ConvertFrom-Json
    if ($ragPerformance.status -ne 'PASS' -or $ragPerformance.candidateChunks -ne 500 -or
        $ragPerformance.concurrency -ne 10 -or $ragPerformance.p95Millis -gt 2000) {
        throw 'RAG capacity evidence does not satisfy the L1 bounded-corpus policy.'
    }
    $provider = $null

    if ($DeploymentMode -eq 'ModelEnabled') {
        if ([string]::IsNullOrWhiteSpace($ProviderEvidence)) {
            throw 'ModelEnabled releases require -ProviderEvidence.'
        }
        $providerPath = (Resolve-Path -LiteralPath $ProviderEvidence).Path
        $provider = Get-Content -Raw -LiteralPath $providerPath | ConvertFrom-Json
        $providerName = [string]$provider.provider
        $modelName = [string]$provider.model
        if ($provider.evaluationVersion -ne '1.0.0' -or
            $provider.manifestSha256 -ne $manifestHash -or
            $provider.promptBundleSha256 -ne $promptHash -or
            [string]::IsNullOrWhiteSpace($providerName) -or $providerName.Length -gt 200 -or
            [string]::IsNullOrWhiteSpace($modelName) -or $modelName.Length -gt 200 -or
            $provider.allMandatoryCasesPassed -isnot [bool] -or
            -not $provider.allMandatoryCasesPassed -or
            [decimal]$provider.maxRequestCostUsd -lt 0 -or
            [decimal]$provider.maxRequestCostUsd -gt 0.02 -or
            [int]$provider.maxInputTokensObserved -lt 1 -or
            [int]$provider.maxInputTokensObserved -gt 8000 -or
            [int]$provider.maxOutputTokensConfigured -lt 1 -or
            [int]$provider.maxOutputTokensConfigured -gt 1500 -or
            [int]$provider.maxSequentialToolsConfigured -lt 1 -or
            [int]$provider.maxSequentialToolsConfigured -gt 10 -or
            [int]$provider.maxConcurrentGenerationsConfigured -lt 1 -or
            [int]$provider.maxConcurrentGenerationsConfigured -gt 5) {
            throw 'Provider evidence is stale, incomplete, failing, or exceeds the $0.02 request-cost boundary.'
        }
        $requiredIds = (Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json).cases.id
        $caseResults = @($provider.caseResults)
        $passedIds = @($caseResults | Where-Object {
                $_.passed -is [bool] -and $_.passed
            } | ForEach-Object id)
        $missing = @($requiredIds | Where-Object { $_ -notin $passedIds })
        $unexpected = @($caseResults.id | Where-Object { $_ -notin $requiredIds })
        $duplicateIds = @($caseResults.id | Group-Object | Where-Object Count -gt 1 | ForEach-Object Name)
        if ($missing.Count -gt 0 -or $unexpected.Count -gt 0 -or $duplicateIds.Count -gt 0 -or
            $caseResults.Count -ne $requiredIds.Count) {
            throw "Provider case results must match mandatory cases exactly. Missing: $($missing -join ', '); unexpected: $($unexpected -join ', '); duplicate: $($duplicateIds -join ', ')."
        }
    }

    $report = [ordered]@{
        schemaVersion = '1.0.0'
        status = 'PASS'
        deploymentMode = $DeploymentMode
        evaluatedAt = [DateTimeOffset]::UtcNow.ToString('o')
        gitCommit = (& git rev-parse HEAD).Trim()
        evaluationVersion = '1.0.0'
        manifestSha256 = $manifestHash
        promptBundleSha256 = $promptHash
        offlineContractTestsPassed = $true
        providerEvidenceRequired = $DeploymentMode -eq 'ModelEnabled'
        ragCandidateChunks = $ragPerformance.candidateChunks
        ragConcurrency = $ragPerformance.concurrency
        ragP95Millis = $ragPerformance.p95Millis
        provider = if ($null -eq $provider) { $null } else { $providerName }
        model = if ($null -eq $provider) { $null } else { $modelName }
    }
    $report | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8NoBOM -LiteralPath (Join-Path $evidenceRoot 'report.json')
    Write-Host "AI/RAG evaluation passed for $DeploymentMode deployment."
}
finally {
    Pop-Location
}
