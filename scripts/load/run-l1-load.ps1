[CmdletBinding()]
param(
    [string]$ProjectName = 'hbti-l1-load',
    [string]$BaseUrl = 'http://web:8080',
    [ValidateRange(1, 300)][int]$ConcurrentSeconds = 10,
    [ValidateRange(1, 3600)][int]$SustainedSeconds = 60,
    [ValidateRange(1, 300)][int]$BurstSeconds = 10,
    [ValidateRange(1024, 65535)][int]$WebPort = 5273,
    [ValidateRange(1024, 65535)][int]$BackendPort = 8180,
    [switch]$SkipComposeStart,
    [switch]$KeepRunning
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$loadScript = (Resolve-Path (Join-Path $PSScriptRoot 'l1.js')).Path
$evidenceRoot = Join-Path $root 'target/release-evidence/load'
New-Item -ItemType Directory -Force -Path $evidenceRoot | Out-Null
$network = "${ProjectName}_default"
$started = $false
$exitCode = 1
$originalLoadPassword = $env:LOAD_PASSWORD
$originalRunId = $env:RUN_ID
$originalWebPort = $env:WEB_PORT
$originalBackendPort = $env:BACKEND_PORT

$passwordBytes = New-Object byte[] 24
[System.Security.Cryptography.RandomNumberGenerator]::Fill($passwordBytes)
$env:LOAD_PASSWORD = [Convert]::ToBase64String($passwordBytes) + 'Aa1!'
$env:RUN_ID = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds().ToString()
$env:WEB_PORT = $WebPort.ToString()
$env:BACKEND_PORT = $BackendPort.ToString()

Push-Location $root
try {
    if (-not $SkipComposeStart) {
        & docker compose --project-name $ProjectName up --build --detach --wait
        if ($LASTEXITCODE -ne 0) {
            throw 'Unable to start the isolated L1 load environment.'
        }
        $started = $true
    }

    & docker network inspect $network --format '{{.Name}}' | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Compose network $network does not exist."
    }
    & docker compose --project-name $ProjectName ps --format json |
        Set-Content -Encoding utf8NoBOM -LiteralPath (Join-Path $evidenceRoot 'compose-ps-before.json')

    $containerArguments = @(
        'run', '--rm', '--network', $network,
        '-v', "${loadScript}:/scripts/l1.js:ro",
        '-v', "${evidenceRoot}:/evidence",
        '-e', 'LOAD_PASSWORD',
        '-e', 'RUN_ID',
        '-e', "BASE_URL=$BaseUrl",
        '-e', 'ACCOUNT_COUNT=20',
        '-e', 'CONCURRENT_SESSIONS=20',
        '-e', "CONCURRENT_DURATION=${ConcurrentSeconds}s",
        '-e', "CONCURRENT_SECONDS=$ConcurrentSeconds",
        '-e', 'SUSTAINED_RPS=20',
        '-e', "SUSTAINED_DURATION=${SustainedSeconds}s",
        '-e', "SUSTAINED_SECONDS=$SustainedSeconds",
        '-e', 'BURST_RPS=100',
        '-e', "BURST_DURATION=${BurstSeconds}s",
        'grafana/k6:0.54.0@sha256:1f40432b1cbe7234e977f96c362c9bc550a2d2b583d014dd8669fe40d3e9e755',
        'run', '/scripts/l1.js'
    )
    & docker @containerArguments 2>&1 |
        Tee-Object -FilePath (Join-Path $evidenceRoot 'k6-output.txt')
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        throw "k6 failed or an L1 threshold was breached (exit $exitCode)."
    }

    $summary = Get-Content -Raw -LiteralPath (Join-Path $evidenceRoot 'k6-summary.json') | ConvertFrom-Json
    $report = [ordered]@{
        schemaVersion = '1.0.0'
        status = 'PASS'
        testedAt = [DateTimeOffset]::UtcNow.ToString('o')
        gitCommit = (& git rev-parse HEAD).Trim()
        k6Image = 'grafana/k6:0.54.0@sha256:1f40432b1cbe7234e977f96c362c9bc550a2d2b583d014dd8669fe40d3e9e755'
        registeredUsers = 20
        maxInteractiveSessions = 20
        observedMaxVus = $summary.metrics.vus_max.values.max
        concurrentSessionSeconds = $ConcurrentSeconds
        interactiveRequests = $summary.metrics.hbti_interactive_requests.values.count
        sustainedRps = 20
        sustainedSeconds = $SustainedSeconds
        burstRps = 100
        burstSeconds = $BurstSeconds
        p95LimitMs = 500
        p99LimitMs = 1500
        allThresholdsPassed = $true
        sustainedP95Ms = $summary.metrics.hbti_sustained_duration.values.'p(95)'
        sustainedP99Ms = $summary.metrics.hbti_sustained_duration.values.'p(99)'
        burstP95Ms = $summary.metrics.hbti_burst_duration.values.'p(95)'
        burstP99Ms = $summary.metrics.hbti_burst_duration.values.'p(99)'
        sustainedErrorRate = $summary.metrics.hbti_sustained_errors.values.rate
        burstErrorRate = $summary.metrics.hbti_burst_errors.values.rate
        sustainedDroppedIterations = $summary.metrics.'dropped_iterations{scenario:sustained}'.values.count
        burstDroppedIterations = $summary.metrics.'dropped_iterations{scenario:burst}'.values.count
        interactiveP95Ms = $summary.metrics.hbti_interactive_duration.values.'p(95)'
        interactiveP99Ms = $summary.metrics.hbti_interactive_duration.values.'p(99)'
        interactiveErrorRate = $summary.metrics.hbti_interactive_errors.values.rate
    }
    $report | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8NoBOM `
        -LiteralPath (Join-Path $evidenceRoot 'report.json')
    Write-Host 'L1 sustained and burst load thresholds passed.'
}
catch {
    & docker compose --project-name $ProjectName ps 2>&1 |
        Set-Content -Encoding utf8NoBOM -LiteralPath (Join-Path $evidenceRoot 'compose-ps-failure.txt')
    & docker compose --project-name $ProjectName logs --no-color --tail 300 2>&1 |
        Set-Content -Encoding utf8NoBOM -LiteralPath (Join-Path $evidenceRoot 'compose-logs-failure.txt')
    throw
}
finally {
    if ($null -eq $originalLoadPassword) {
        Remove-Item Env:LOAD_PASSWORD -ErrorAction SilentlyContinue
    } else {
        $env:LOAD_PASSWORD = $originalLoadPassword
    }
    if ($null -eq $originalRunId) {
        Remove-Item Env:RUN_ID -ErrorAction SilentlyContinue
    } else {
        $env:RUN_ID = $originalRunId
    }
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
    if ($started -and -not $KeepRunning) {
        & docker compose --project-name $ProjectName down --volumes --remove-orphans
        if ($LASTEXITCODE -ne 0 -and $exitCode -eq 0) {
            throw 'Load environment cleanup failed.'
        }
    }
    Pop-Location
}
