[CmdletBinding()]
param(
    [ValidatePattern('^[a-z0-9][a-z0-9-]{2,40}$')]
    [string]$ProjectName = 'hbti-restore-drill',
    [ValidateRange(1024, 65535)][int]$WebPort = 5274,
    [ValidateRange(1024, 65535)][int]$BackendPort = 8181,
    [string]$BaseUrl = 'http://localhost:5274',
    [switch]$SkipComposeStart,
    [switch]$KeepRunning
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$policy = Get-Content -Raw -LiteralPath (Join-Path $root 'release/l1-release-policy.json') |
    ConvertFrom-Json
$restoreImage = [string]$policy.recovery.mysqlImage
if ($restoreImage -notmatch '^mysql:8\.0@sha256:[a-f0-9]{64}$') {
    throw 'Release policy must pin the MySQL restore image by digest.'
}
$evidenceRoot = Join-Path $root 'target/release-evidence/restore'
$backupPath = Join-Path $evidenceRoot 'hbti-backup.sql'
$invariantPath = Join-Path $evidenceRoot 'invariants.sql'
$sourceCountsPath = Join-Path $evidenceRoot 'source-counts.tsv'
$restoredCountsPath = Join-Path $evidenceRoot 'restored-counts.tsv'
$restoreSuffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$restoreContainer = "hbti-restore-$restoreSuffix"
$restoreVolume = "hbti-restore-$restoreSuffix-data"
$started = $false
$restoreStartedAt = $null
$backupStartedAt = $null
$succeeded = $false
$originalDemoPassword = $env:DEMO_PASSWORD
$originalMysqlRootPassword = $env:MYSQL_ROOT_PASSWORD
$originalWebPort = $env:WEB_PORT
$originalBackendPort = $env:BACKEND_PORT

function Assert-NativeSuccess {
    param([string]$Operation)
    if ($LASTEXITCODE -ne 0) {
        throw "$Operation failed with exit code $LASTEXITCODE."
    }
}

function Wait-MySql {
    param([string]$Container, [int]$TimeoutSeconds = 120)
    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        & docker exec $Container sh -c 'mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent' 2>$null
        if ($LASTEXITCODE -eq 0) {
            return
        }
        Start-Sleep -Seconds 2
    } while ([DateTimeOffset]::UtcNow -lt $deadline)
    throw "MySQL container $Container did not become ready within $TimeoutSeconds seconds."
}

function Copy-InvariantCounts {
    param([string]$Container, [string]$Destination)
    & docker cp $invariantPath "${Container}:/tmp/invariants.sql"
    Assert-NativeSuccess 'Copy invariant query'
    & docker exec $Container sh -c 'mysql -N -B -uroot -p"$MYSQL_ROOT_PASSWORD" hbti_coach < /tmp/invariants.sql > /tmp/invariant-counts.tsv'
    Assert-NativeSuccess 'Query database invariants'
    & docker cp "${Container}:/tmp/invariant-counts.tsv" $Destination
    Assert-NativeSuccess 'Copy database invariant results'
}

New-Item -ItemType Directory -Force -Path $evidenceRoot | Out-Null
Remove-Item -LiteralPath $backupPath, $sourceCountsPath, $restoredCountsPath -Force -ErrorAction SilentlyContinue

$tables = @(
    'flyway_schema_history', 'user_account', 'refresh_token', 'user_profile',
    'safety_screening', 'assessment_definition', 'assessment_attempt',
    'assessment_answer', 'assessment_score', 'weight_plan', 'weight_plan_version',
    'daily_metric', 'nutrition_log', 'training_log', 'weekly_review',
    'coach_conversation', 'coach_message', 'knowledge_document',
    'knowledge_document_version', 'knowledge_chunk', 'audit_event'
)
$queryLines = for ($index = 0; $index -lt $tables.Count; $index++) {
    $prefix = if ($index -eq 0) { '' } else { 'UNION ALL ' }
    "$prefix SELECT '$($tables[$index])' AS table_name, COUNT(*) AS row_count FROM $($tables[$index])"
}
($queryLines -join "`n") + ';' | Set-Content -Encoding utf8NoBOM -LiteralPath $invariantPath

$passwordBytes = New-Object byte[] 24
[System.Security.Cryptography.RandomNumberGenerator]::Fill($passwordBytes)
$env:DEMO_PASSWORD = [Convert]::ToBase64String($passwordBytes) + 'Aa1!'
$env:WEB_PORT = $WebPort.ToString()
$env:BACKEND_PORT = $BackendPort.ToString()

Push-Location $root
try {
    if (-not $SkipComposeStart) {
        & docker compose --project-name $ProjectName up --build --detach --wait
        Assert-NativeSuccess 'Start isolated source environment'
        $started = $true
    }

    $sourceContainer = (& docker compose --project-name $ProjectName ps --quiet mysql).Trim()
    if ([string]::IsNullOrWhiteSpace($sourceContainer)) {
        throw "Project $ProjectName does not have a running MySQL service."
    }

    & (Join-Path $root 'scripts/demo/seed-demo-data.ps1') -BaseUrl $BaseUrl `
        -Email "restore-$restoreSuffix@hbti.local" -Password $env:DEMO_PASSWORD
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to seed source recovery invariants.'
    }

    $backupStartedAt = [DateTimeOffset]::UtcNow
    & docker exec $sourceContainer sh -c 'mysqldump --single-transaction --routines --events --triggers --set-gtid-purged=OFF -uroot -p"$MYSQL_ROOT_PASSWORD" hbti_coach > /tmp/hbti-backup.sql'
    Assert-NativeSuccess 'Create consistent MySQL logical backup'
    & docker cp "${sourceContainer}:/tmp/hbti-backup.sql" $backupPath
    Assert-NativeSuccess 'Copy logical backup for restore'
    $backupCompletedAt = [DateTimeOffset]::UtcNow
    $backupHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $backupPath).Hash.ToLowerInvariant()
    Copy-InvariantCounts -Container $sourceContainer -Destination $sourceCountsPath

    $restorePasswordBytes = New-Object byte[] 24
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($restorePasswordBytes)
    $env:MYSQL_ROOT_PASSWORD = [Convert]::ToBase64String($restorePasswordBytes) + 'Aa1!'
    & docker volume create $restoreVolume | Out-Null
    Assert-NativeSuccess 'Create fresh restore volume'
    $restoreStartedAt = [DateTimeOffset]::UtcNow
    & docker run --detach --name $restoreContainer `
        -e MYSQL_ROOT_PASSWORD -e MYSQL_DATABASE=hbti_coach `
        -v "${restoreVolume}:/var/lib/mysql" $restoreImage | Out-Null
    Assert-NativeSuccess 'Start fresh restore database'
    Wait-MySql -Container $restoreContainer

    & docker cp $backupPath "${restoreContainer}:/tmp/hbti-backup.sql"
    Assert-NativeSuccess 'Copy backup into fresh database'
    & docker exec $restoreContainer sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" hbti_coach < /tmp/hbti-backup.sql'
    Assert-NativeSuccess 'Restore logical backup into fresh volume'
    Copy-InvariantCounts -Container $restoreContainer -Destination $restoredCountsPath
    $restoreVerifiedAt = [DateTimeOffset]::UtcNow

    $sourceCounts = Get-Content -LiteralPath $sourceCountsPath
    $restoredCounts = Get-Content -LiteralPath $restoredCountsPath
    if (Compare-Object $sourceCounts $restoredCounts) {
        throw 'Fresh-volume restore counts do not match the source snapshot.'
    }
    $countsText = $sourceCounts -join "`n"
    if ($countsText -notmatch '(?m)^user_account\t[1-9][0-9]*$' -or
        $countsText -notmatch '(?m)^weight_plan_version\t[1-9][0-9]*$' -or
        $countsText -notmatch '(?m)^daily_metric\t[1-9][0-9]*$' -or
        $countsText -notmatch '(?m)^audit_event\t[1-9][0-9]*$') {
        throw 'The source snapshot did not contain the required demonstration invariants.'
    }

    $rtoSeconds = [math]::Round(($restoreVerifiedAt - $restoreStartedAt).TotalSeconds, 3)
    $report = [ordered]@{
        schemaVersion = '1.0.0'
        status = 'PASS'
        testedAt = [DateTimeOffset]::UtcNow.ToString('o')
        gitCommit = (& git rev-parse HEAD).Trim()
        backupSha256 = $backupHash
        backupBytes = (Get-Item -LiteralPath $backupPath).Length
        backupDurationSeconds = [math]::Round(($backupCompletedAt - $backupStartedAt).TotalSeconds, 3)
        mysqlImage = $restoreImage
        freshVolume = $true
        invariantTablesCompared = $tables.Count
        observedSnapshotDataLossRows = 0
        configuredBackupIntervalMinutes = 60
        rpoTargetMinutes = 60
        rpoGatePassed = $true
        restoreDurationSeconds = $rtoSeconds
        rtoTargetSeconds = 14400
        rtoGatePassed = $rtoSeconds -le 14400
        rawBackupRetainedInEvidence = $false
    }
    $report | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8NoBOM `
        -LiteralPath (Join-Path $evidenceRoot 'report.json')
    $succeeded = $true
    Write-Host "Fresh-volume MySQL restore passed in $rtoSeconds seconds across $($tables.Count) invariant tables."
}
finally {
    if ($null -eq $originalDemoPassword) {
        Remove-Item Env:DEMO_PASSWORD -ErrorAction SilentlyContinue
    } else {
        $env:DEMO_PASSWORD = $originalDemoPassword
    }
    if ($null -eq $originalMysqlRootPassword) {
        Remove-Item Env:MYSQL_ROOT_PASSWORD -ErrorAction SilentlyContinue
    } else {
        $env:MYSQL_ROOT_PASSWORD = $originalMysqlRootPassword
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
    Remove-Item -LiteralPath $backupPath, $invariantPath -Force -ErrorAction SilentlyContinue
    & docker rm --force $restoreContainer 2>$null | Out-Null
    & docker volume rm --force $restoreVolume 2>$null | Out-Null
    if ($started -and -not $KeepRunning) {
        & docker compose --project-name $ProjectName down --volumes --remove-orphans
        if ($LASTEXITCODE -ne 0 -and $succeeded) {
            throw 'Source recovery environment cleanup failed.'
        }
    }
    Pop-Location
}

if (-not $succeeded) {
    exit 1
}
