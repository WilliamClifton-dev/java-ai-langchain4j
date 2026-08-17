[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:5173',
    [string]$Email = "demo-$([DateTimeOffset]::UtcNow.ToString('yyyyMMdd-HHmmss'))@hbti.local",
    [string]$Password = $env:DEMO_PASSWORD,
    [switch]$ResetExisting,
    [switch]$DeleteAfter
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if ([string]::IsNullOrWhiteSpace($Password) -or
    [System.Text.Encoding]::UTF8.GetByteCount($Password) -lt 12) {
    throw 'Set DEMO_PASSWORD (at least 12 UTF-8 bytes) or pass -Password. It is never persisted in evidence.'
}

$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$evidenceRoot = Join-Path $root 'target/release-evidence/demo'
New-Item -ItemType Directory -Force -Path $evidenceRoot | Out-Null
$base = $BaseUrl.TrimEnd('/')
$session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$csrf = $null

function Get-StatusCode {
    param($ErrorRecord)
    if ($null -ne $ErrorRecord.Exception.Response) {
        return [int]$ErrorRecord.Exception.Response.StatusCode
    }
    return 0
}

function Initialize-Csrf {
    $script:csrf = Invoke-RestMethod -Method Get -Uri "$base/api/v1/auth/csrf" -WebSession $session
}

function Invoke-HbtiApi {
    param(
        [Parameter(Mandatory)][string]$Method,
        [Parameter(Mandatory)][string]$Path,
        $Body,
        [hashtable]$Headers = @{}
    )
    $requestHeaders = @{}
    foreach ($entry in $Headers.GetEnumerator()) {
        $requestHeaders[$entry.Key] = $entry.Value
    }
    if ($Method -in @('Post', 'Put', 'Delete')) {
        $requestHeaders[$csrf.headerName] = $csrf.token
    }
    $arguments = @{
        Method = $Method
        Uri = "$base$Path"
        WebSession = $session
        Headers = $requestHeaders
    }
    if ($null -ne $Body) {
        $arguments.ContentType = 'application/json'
        $arguments.Body = $Body | ConvertTo-Json -Depth 20 -Compress
    }
    return Invoke-RestMethod @arguments
}

function Register-Or-Login {
    $credentials = @{ email = $Email; password = $Password }
    try {
        return Invoke-HbtiApi -Method Post -Path '/api/v1/auth/register' -Body $credentials
    }
    catch {
        if ((Get-StatusCode $_) -ne 409) {
            throw
        }
        return Invoke-HbtiApi -Method Post -Path '/api/v1/auth/login' -Body $credentials
    }
}

Initialize-Csrf
$account = Register-Or-Login

if ($ResetExisting) {
    Invoke-HbtiApi -Method Delete -Path '/api/v1/account' -Body @{ confirmation = 'DELETE_MY_ACCOUNT' } | Out-Null
    $session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
    Initialize-Csrf
    $account = Register-Or-Login
}

$profile = Invoke-HbtiApi -Method Put -Path '/api/v1/profile' -Body @{
    dateOfBirth = '1992-04-20'
    calculationSex = 'FEMALE'
    heightCm = 165
    currentWeightKg = 70
    targetWeightKg = 64
    activityLevel = 'MODERATE'
    timeZone = 'Asia/Hong_Kong'
}
$screening = Invoke-HbtiApi -Method Post -Path '/api/v1/profile/screenings' -Body @{
    pregnantOrBreastfeeding = $false
    eatingDisorderHistory = $false
    medicalGuidanceRequired = $false
    weightAffectingMedication = $false
    concerningSymptoms = $false
}
if (-not $screening.automaticPlanningAllowed) {
    throw 'The bounded demonstration screening unexpectedly blocked deterministic planning.'
}

$definition = Invoke-HbtiApi -Method Get -Path '/api/v1/assessments/hbti/definitions/1.0.0'
$answers = @($definition.items | ForEach-Object {
    @{ itemKey = $_.itemKey; value = 1 + (($_.ordinal + 1) % 5) }
})
$assessment = Invoke-HbtiApi -Method Post -Path '/api/v1/assessments/hbti/submissions' `
    -Headers @{ 'Idempotency-Key' = 'demo-v1-assessment' } `
    -Body @{ definitionVersion = $definition.version; answers = $answers }

$draft = Invoke-HbtiApi -Method Post -Path '/api/v1/plans/drafts' `
    -Headers @{ 'Idempotency-Key' = 'demo-v1-plan-draft' } -Body @{ goal = 'LOSS' }
$draft = Invoke-HbtiApi -Method Post `
    -Path "/api/v1/plans/$($draft.planId)/versions/$($draft.id)/validation" -Body $null
$draft = Invoke-HbtiApi -Method Post `
    -Path "/api/v1/plans/$($draft.planId)/versions/$($draft.id)/confirmation" -Body $null
$activePlan = Invoke-HbtiApi -Method Post `
    -Path "/api/v1/plans/$($draft.planId)/versions/$($draft.id)/activation" `
    -Headers @{ 'Idempotency-Key' = 'demo-v1-plan-activation' } -Body $null

$today = (Get-Date).Date
$daySummaries = @()
for ($offset = 6; $offset -ge 0; $offset--) {
    $date = $today.AddDays(-$offset).ToString('yyyy-MM-dd')
    $suffix = $date.Replace('-', '')
    Invoke-HbtiApi -Method Post -Path '/api/v1/tracking/daily-metrics' `
        -Headers @{ 'Idempotency-Key' = "demo-v1-metric-$suffix" } -Body @{
            localDate = $date
            weightKg = 70 - ((6 - $offset) * 0.1)
            steps = 7600 + ((6 - $offset) * 150)
            activityMinutes = 35
            sleepMinutes = 450
            sleepQuality = 4
        } | Out-Null
    Invoke-HbtiApi -Method Post -Path '/api/v1/tracking/nutrition' `
        -Headers @{ 'Idempotency-Key' = "demo-v1-nutrition-$suffix" } -Body @{
            localDate = $date
            energyKcal = [int](($activePlan.energyMinKcalPerDay + $activePlan.energyMaxKcalPerDay) / 2)
            proteinG = 105
            carbohydrateG = 210
            fatG = 60
        } | Out-Null
    if ($offset % 2 -eq 0) {
        Invoke-HbtiApi -Method Post -Path '/api/v1/tracking/training' `
            -Headers @{ 'Idempotency-Key' = "demo-v1-training-$suffix" } -Body @{
                localDate = $date
                trainingType = 'STRENGTH'
                durationMinutes = 45
                intensity = 'MODERATE'
            } | Out-Null
    }
    $daySummaries += Invoke-HbtiApi -Method Get -Path "/api/v1/tracking/days/$date"
}

$verifiedProfile = Invoke-HbtiApi -Method Get -Path '/api/v1/profile'
$verifiedScreening = Invoke-HbtiApi -Method Get -Path '/api/v1/profile/screenings/current'
$verifiedAssessment = Invoke-HbtiApi -Method Get -Path '/api/v1/assessments/hbti/results/current'
$verifiedPlan = Invoke-HbtiApi -Method Get -Path '/api/v1/plans/active'

$report = [ordered]@{
    schemaVersion = '1.0.0'
    status = 'PASS'
    seededAt = [DateTimeOffset]::UtcNow.ToString('o')
    gitCommit = (& git -C $root rev-parse HEAD).Trim()
    baseUrl = $base
    email = $Email
    credentialSource = 'DEMO_PASSWORD or explicit -Password; secret omitted'
    accountId = $account.user.id
    profileVerified = $verifiedProfile.userId -eq $account.user.id
    screeningGate = $verifiedScreening.status
    hbtiDefinitionVersion = $verifiedAssessment.definitionVersion
    hbtiTypeIsSecondary = $true
    activePlanId = $verifiedPlan.planId
    trackedDays = $daySummaries.Count
    deletedAfterVerification = [bool]$DeleteAfter
}

if (-not $report.profileVerified -or $report.screeningGate -ne 'ELIGIBLE' -or
    $report.hbtiDefinitionVersion -ne '1.0.0' -or $report.trackedDays -ne 7) {
    throw 'Demonstration data verification failed.'
}

if ($DeleteAfter) {
    Invoke-HbtiApi -Method Delete -Path '/api/v1/account' -Body @{ confirmation = 'DELETE_MY_ACCOUNT' } | Out-Null
}

$report | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8NoBOM `
    -LiteralPath (Join-Path $evidenceRoot 'report.json')
Write-Host "Demonstration data verified for $Email. Password was not written to evidence."
