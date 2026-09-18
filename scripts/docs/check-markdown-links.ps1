param(
    [string]$Root = (Join-Path $PSScriptRoot '..\..\docs')
)

$ErrorActionPreference = 'Stop'
$rootPath = (Resolve-Path -LiteralPath $Root).Path
$failures = [System.Collections.Generic.List[string]]::new()

foreach ($file in Get-ChildItem -LiteralPath $rootPath -Recurse -Filter '*.md') {
    $text = Get-Content -LiteralPath $file.FullName -Raw
    foreach ($match in [regex]::Matches($text, '\[[^\]]+\]\(([^)]+)\)')) {
        $target = $match.Groups[1].Value.Trim()
        if ([string]::IsNullOrWhiteSpace($target) -or $target.StartsWith('#') -or $target -match '^(?i)(https?|mailto):') {
            continue
        }
        $target = $target.Split('#')[0].Split('?')[0]
        if ([string]::IsNullOrWhiteSpace($target)) { continue }
        $candidate = if ([IO.Path]::IsPathRooted($target)) { $target } else { Join-Path $file.DirectoryName $target }
        if (-not (Test-Path -LiteralPath $candidate)) {
            $line = ($text.Substring(0, $match.Index) -split "`n").Count
            $failures.Add("$($file.FullName):$line -> $target")
        }
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output "Markdown local link check passed under $rootPath."
