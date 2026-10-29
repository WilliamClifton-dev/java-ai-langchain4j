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
$exceptionsPath = Join-Path $repositoryRoot 'config/security/osv-reachability-exceptions.json'
$severityRank = @{
    UNKNOWN = 5
    LOW = 1
    MODERATE = 2
    HIGH = 3
    CRITICAL = 4
}

function Get-OptionalProperty {
    param($InputObject, [string]$Name)
    if ($null -eq $InputObject) {
        return $null
    }
    $property = $InputObject.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }
    return $property.Value
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
$results = @(Get-OptionalProperty $batch 'results')
if ($results.Count -ne $queries.Count) {
    throw "OSV returned $($results.Count) result(s) for $($queries.Count) queries."
}

$matches = @(for ($index = 0; $index -lt $queries.Count; $index++) {
    $vulnerabilities = @(Get-OptionalProperty $results[$index] 'vulns')
    foreach ($vulnerability in $vulnerabilities) {
        $id = [string](Get-OptionalProperty $vulnerability 'id')
        if ([string]::IsNullOrWhiteSpace($id)) {
            continue
        }
        [pscustomobject]@{
            package = $queries[$index].package.name
            version = $queries[$index].version
            id = $id
        }
    }
})

$detailsById = @{}
foreach ($id in @($matches | ForEach-Object { $_.id } | Sort-Object -Unique)) {
    $detailsById[$id] = Invoke-RestMethod -Uri "https://api.osv.dev/v1/vulns/$id" -Method Get
}

$findings = @($matches | ForEach-Object {
    $details = $detailsById[$_.id]
    $databaseSpecific = Get-OptionalProperty $details 'database_specific'
    $rawSeverity = [string](Get-OptionalProperty $databaseSpecific 'severity')
    $severity = if (-not [string]::IsNullOrWhiteSpace($rawSeverity)) {
        $rawSeverity.ToUpperInvariant()
    }
    else {
        'UNKNOWN'
    }
    $fixedVersions = @(
        foreach ($affected in @(Get-OptionalProperty $details 'affected')) {
            foreach ($range in @(Get-OptionalProperty $affected 'ranges')) {
                foreach ($event in @(Get-OptionalProperty $range 'events')) {
                    $fixed = [string](Get-OptionalProperty $event 'fixed')
                    if (-not [string]::IsNullOrWhiteSpace($fixed)) {
                        $fixed
                    }
                }
            }
        }
    ) | Sort-Object -Unique
    [pscustomobject]@{
        id = $_.id
        severity = $severity
        package = $_.package
        version = $_.version
        summary = [string](Get-OptionalProperty $details 'summary')
        fixedVersions = @($fixedVersions)
        modified = [string](Get-OptionalProperty $details 'modified')
    }
})

$exceptions = @(Get-Content -Raw -LiteralPath $exceptionsPath | ConvertFrom-Json)
$today = (Get-Date).ToUniversalTime().Date
$findings = @($findings | ForEach-Object {
    $finding = $_
    $exception = @($exceptions | Where-Object {
        $_.id -eq $finding.id -and $_.package -eq $finding.package -and
        -not [string]::IsNullOrWhiteSpace($_.rationale) -and
        -not [string]::IsNullOrWhiteSpace($_.advisoryUrl) -and
        -not [string]::IsNullOrWhiteSpace($_.requiredConditionAbsent) -and
        [DateTime]::Parse($_.reviewBy).ToUniversalTime().Date -ge $today
    }) | Select-Object -First 1
    $finding | Add-Member -NotePropertyName disposition -NotePropertyValue `
        $(if ($null -ne $exception) { 'documented_non_reachable' } else { 'blocking' }) -PassThru
})

$report = [ordered]@{
    generatedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
    source = 'https://api.osv.dev/v1/querybatch'
    dependencyScope = 'runtime'
    dependencyCount = $dependencies.Count
    findingCount = $findings.Count
    blockingFindingCount = @($findings | Where-Object disposition -eq 'blocking').Count
    documentedExceptions = @($findings | Where-Object disposition -eq 'documented_non_reachable')
    failOn = $FailOn
    findings = $findings
}
$report | ConvertTo-Json -Depth 10 | Set-Content $reportFile -Encoding utf8

$counts = $findings | Group-Object severity | Sort-Object Name
Write-Host "OSV runtime audit: dependencies=$($dependencies.Count), findings=$($findings.Count)"
Write-Host "Blocking findings: $(@($findings | Where-Object disposition -eq 'blocking').Count); documented non-reachable: $(@($findings | Where-Object disposition -eq 'documented_non_reachable').Count)"
foreach ($count in $counts) {
    Write-Host "$($count.Name)=$($count.Count)"
}
foreach ($finding in @($findings | Where-Object disposition -eq 'documented_non_reachable')) {
    Write-Host "Documented exception: $($finding.id) ($($finding.package) $($finding.version))"
}
Write-Host "Report: $reportFile"

$threshold = $severityRank[$FailOn]
$blocking = @($findings | Where-Object {
    $_.disposition -eq 'blocking' -and (
    -not $severityRank.ContainsKey($_.severity) -or $severityRank[$_.severity] -ge $threshold
    )
})
if ($blocking.Count -gt 0) {
    $blocking | Sort-Object severity, package, id | Format-Table severity, package, version, id -AutoSize
    throw "OSV audit found $($blocking.Count) finding(s) at or above $FailOn, including unknown severities."
}
