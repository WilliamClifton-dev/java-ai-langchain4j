[CmdletBinding()]
param(
    [ValidateSet('LOW', 'MODERATE', 'HIGH', 'CRITICAL')]
    [string]$FailOn = 'HIGH'
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$outputDirectory = Join-Path $repositoryRoot 'target\security'
$dependencyFile = Join-Path $outputDirectory 'runtime-dependencies.txt'
$reportFile = Join-Path $outputDirectory 'osv-report.json'
$severityRank = @{
    UNKNOWN = 5
    LOW = 1
    MODERATE = 2
    HIGH = 3
    CRITICAL = 4
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

Push-Location $repositoryRoot
try {
    & mvn -q dependency:list '-DincludeScope=runtime' "-DoutputFile=$dependencyFile" `
        '-DoutputAbsoluteArtifactFilename=false' '-DappendOutput=false'
    if ($LASTEXITCODE -ne 0) {
        throw "Maven dependency resolution failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

$dependencies = @(Get-Content $dependencyFile | Where-Object {
    $_ -match '^\s+[\w.-]+:[\w.-]+:'
} | ForEach-Object {
    $coordinate = ($_ -replace '^\s+', '' -replace '\s+--.*$', '')
    $parts = $coordinate.Split(':')
    [pscustomobject]@{
        package = "{0}:{1}" -f $parts[0], $parts[1]
        version = $parts[$parts.Length - 2]
    }
})

$queries = @($dependencies | ForEach-Object {
    @{
        package = @{ ecosystem = 'Maven'; name = $_.package }
        version = $_.version
    }
})
$requestBody = @{ queries = $queries } | ConvertTo-Json -Depth 6 -Compress
$batch = Invoke-RestMethod -Uri 'https://api.osv.dev/v1/querybatch' -Method Post `
    -ContentType 'application/json' -Body $requestBody

$matches = for ($index = 0; $index -lt $queries.Count; $index++) {
    foreach ($vulnerability in @($batch.results[$index].vulns | Where-Object { $_.id })) {
        [pscustomobject]@{
            package = $queries[$index].package.name
            version = $queries[$index].version
            id = $vulnerability.id
        }
    }
}

$detailsById = @{}
foreach ($id in @($matches.id | Sort-Object -Unique)) {
    $detailsById[$id] = Invoke-RestMethod -Uri "https://api.osv.dev/v1/vulns/$id" -Method Get
}

$findings = @($matches | ForEach-Object {
    $details = $detailsById[$_.id]
    $severity = if ($details.database_specific.severity) {
        $details.database_specific.severity.ToUpperInvariant()
    }
    else {
        'UNKNOWN'
    }
    [pscustomobject]@{
        id = $_.id
        severity = $severity
        package = $_.package
        version = $_.version
        summary = $details.summary
        fixedVersions = @($details.affected.ranges.events.fixed | Where-Object { $_ } | Sort-Object -Unique)
        modified = $details.modified
    }
})

$report = [ordered]@{
    generatedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
    source = 'https://api.osv.dev/v1/querybatch'
    dependencyScope = 'runtime'
    dependencyCount = $dependencies.Count
    findingCount = $findings.Count
    failOn = $FailOn
    findings = $findings
}
$report | ConvertTo-Json -Depth 10 | Set-Content $reportFile -Encoding utf8

$counts = $findings | Group-Object severity | Sort-Object Name
Write-Host "OSV runtime audit: dependencies=$($dependencies.Count), findings=$($findings.Count)"
foreach ($count in $counts) {
    Write-Host "$($count.Name)=$($count.Count)"
}
Write-Host "Report: $reportFile"

$threshold = $severityRank[$FailOn]
$blocking = @($findings | Where-Object {
    -not $severityRank.ContainsKey($_.severity) -or $severityRank[$_.severity] -ge $threshold
})
if ($blocking.Count -gt 0) {
    $blocking | Sort-Object severity, package, id | Format-Table severity, package, version, id -AutoSize
    throw "OSV audit found $($blocking.Count) finding(s) at or above $FailOn, including unknown severities."
}
