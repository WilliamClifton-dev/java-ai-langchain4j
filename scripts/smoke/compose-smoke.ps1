[CmdletBinding()]
param(
    [ValidateRange(30, 900)]
    [int]$TimeoutSeconds = 240,
    [ValidatePattern('^[a-z0-9][a-z0-9_-]+$')]
    [string]$ProjectName = 'hbti-smoke',
    [ValidatePattern('^target[/\\][A-Za-z0-9._-]+(?:[/\\][A-Za-z0-9._-]+)*$')]
    [string]$EvidenceDirectory = 'target/compose-smoke',
    [switch]$KeepRunning
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$evidencePath = Join-Path $repositoryRoot $EvidenceDirectory
$services = @('mysql', 'redis', 'backend', 'web')
$succeeded = $false

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    & docker compose --project-name $ProjectName @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose $($Arguments -join ' ') failed with exit code $LASTEXITCODE"
    }
}

function Assert-ServiceHealthy {
    param([string]$Service)

    $containerId = (& docker compose --project-name $ProjectName ps --quiet $Service).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($containerId)) {
        throw "Compose service '$Service' has no running container"
    }

    $status = (& docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $containerId).Trim()
    if ($LASTEXITCODE -ne 0 -or $status -ne 'healthy') {
        throw "Compose service '$Service' is '$status', expected 'healthy'"
    }

    "${Service}=healthy" | Add-Content (Join-Path $evidencePath 'service-health.txt')
}

function Assert-ServiceRunsAsNonRoot {
    param([string]$Service)

    $containerId = (& docker compose --project-name $ProjectName ps --quiet $Service).Trim()
    $userId = (& docker exec $containerId id -u).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($userId) -or $userId -eq '0') {
        throw "Compose service '$Service' runs as uid '$userId', expected a non-root uid"
    }

    "${Service}=uid:${userId}" | Add-Content (Join-Path $evidencePath 'runtime-users.txt')
}

function Get-ResponseText {
    param($Response)

    if ($Response.Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Response.Content)
    }
    return [string]$Response.Content
}

function Assert-UpResponse {
    param(
        [string]$Uri,
        [string]$EvidenceFile
    )

    $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 15
    if ($response.StatusCode -ne 200) {
        throw "$Uri returned HTTP $($response.StatusCode)"
    }
    $content = Get-ResponseText $response
    $content | Set-Content (Join-Path $evidencePath $EvidenceFile) -Encoding utf8
    $health = $content | ConvertFrom-Json
    if ($health.status -ne 'UP') {
        throw "$Uri returned health status '$($health.status)', expected 'UP'"
    }
}

Push-Location $repositoryRoot
try {
    New-Item -ItemType Directory -Path $evidencePath -Force | Out-Null
    Remove-Item (Join-Path $evidencePath '*') -Force -ErrorAction SilentlyContinue

    Invoke-Compose config --quiet
    Invoke-Compose up --build --detach --wait --wait-timeout $TimeoutSeconds

    foreach ($service in $services) {
        Assert-ServiceHealthy $service
    }
    Assert-ServiceRunsAsNonRoot 'backend'
    Assert-ServiceRunsAsNonRoot 'web'

    (& docker compose --project-name $ProjectName ps) |
        Set-Content (Join-Path $evidencePath 'compose-ps.txt') -Encoding utf8

    Assert-UpResponse 'http://127.0.0.1:8080/actuator/health/readiness' 'backend-readiness.json'

    $webHealth = Invoke-WebRequest -Uri 'http://127.0.0.1:5173/healthz' -UseBasicParsing -TimeoutSec 15
    $webHealthContent = Get-ResponseText $webHealth
    if ($webHealth.StatusCode -ne 200 -or $webHealthContent.Trim() -ne 'ok') {
        throw "Web health assertion failed"
    }
    $webHealthContent | Set-Content (Join-Path $evidencePath 'web-health.txt') -Encoding utf8

    Assert-UpResponse 'http://127.0.0.1:5173/actuator/health/readiness' 'proxied-readiness.json'

    $index = Invoke-WebRequest -Uri 'http://127.0.0.1:5173/' -UseBasicParsing -TimeoutSec 15
    $indexContent = Get-ResponseText $index
    if ($index.StatusCode -ne 200 -or $indexContent -notmatch '<div id="root"></div>') {
        throw "Web root did not return the production SPA shell"
    }
    $indexContent | Set-Content (Join-Path $evidencePath 'web-index.html') -Encoding utf8

    "Compose smoke passed: 4 healthy services, backend readiness UP, same-origin proxy UP, SPA shell served."
    $succeeded = $true
}
catch {
    $_ | Out-String | Set-Content (Join-Path $evidencePath 'failure.txt') -Encoding utf8
    (& docker compose --project-name $ProjectName ps --all 2>&1) |
        Set-Content (Join-Path $evidencePath 'compose-ps.txt') -Encoding utf8
    (& docker compose --project-name $ProjectName logs --no-color 2>&1) |
        Set-Content (Join-Path $evidencePath 'compose-logs.txt') -Encoding utf8
    throw
}
finally {
    try {
        if (-not $KeepRunning) {
            Invoke-Compose down --volumes --remove-orphans
        }
    }
    finally {
        Pop-Location
    }
}

if (-not $succeeded) {
    exit 1
}
