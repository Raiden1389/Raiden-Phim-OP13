<#
.SYNOPSIS
  Trim CHANGELOG.full.md -> CHANGELOG.md (user-facing, last N versions)
  Adapted for Raiden Phim format: ## vX.Y.Z — YYYY-MM-DD (description)
.PARAMETER Keep
  Number of recent versions to keep (default: 5)
.EXAMPLE
  .\scripts\changelog-trim.ps1
  .\scripts\changelog-trim.ps1 -Keep 3
#>
param(
    [int]$Keep = 5
)

$root = Split-Path -Parent $PSScriptRoot
$fullPath = Join-Path $root 'CHANGELOG.full.md'
$targetPath = Join-Path $root 'CHANGELOG.md'

# --- Helpers ---

function Get-SectionType([string]$title) {
    $t = $title.ToLowerInvariant()
    if ($t -match 'files?\s*(modified|changed)') { return 'SKIP' }
    if ($t -match 'modified files') { return 'SKIP' }
    if ($t -match 'breaking|migration') { return 'Breaking/Migration' }
    if ($t -match 'fix|bug|hotfix|patch|harden') { return 'Fixed' }
    if ($t -match 'perf|optimi|speed|performance') { return 'Perf' }
    if ($t -match 'add|new|feature|multi.source|guide|help') { return 'Added' }
    if ($t -match 'remov') { return 'Changed' }
    return 'Changed'
}

function Test-FileListing([string]$line) {
    $l = $line.Trim()
    if ($l -match '^\-\s+`[^`]+\.\w+`\s*[\-]') { return $true }
    if ($l -match '^\-\s+[A-Z][a-zA-Z0-9]+\.\w{2,4}\s*[\-]') { return $true }
    return $false
}

function Parse-Versions([string]$text) {
    # Match: ## vX.Y.Z — YYYY-MM-DD (description)
    $rx = [regex]'(?m)^##\s+v(\d+\.\d+\.\d+)\s+.+\s+(\d{4}-\d{2}-\d{2})\s*(.*)$'
    $versionMatches = $rx.Matches($text)
    if ($versionMatches.Count -eq 0) { return @() }

    $blocks = @()
    for ($i = 0; $i -lt $versionMatches.Count; $i++) {
        $start = $versionMatches[$i].Index
        $end = if ($i + 1 -lt $versionMatches.Count) { $versionMatches[$i + 1].Index } else { $text.Length }
        $blockText = $text.Substring($start, $end - $start).TrimEnd()
        $lines = $blockText -split "`r?`n"
        $heading = $lines[0]
        $body = @()
        if ($lines.Length -gt 1) { $body = $lines[1..($lines.Length - 1)] }

        $blocks += [pscustomobject]@{
            Heading = $heading
            Body    = $body
        }
    }
    return $blocks
}

function Summarize-Block($block) {
    $ordered = @('Added', 'Changed', 'Fixed', 'Perf', 'Breaking/Migration')
    $map = @{}
    foreach ($k in $ordered) { $map[$k] = [System.Collections.Generic.List[string]]::new() }

    $topImpact = $null
    $current = 'Changed'
    $skipSection = $false
    $maxPerSection = 8

    foreach ($raw in $block.Body) {
        $line = $raw.TrimEnd()

        if ([string]::IsNullOrWhiteSpace($line)) { continue }

        # Existing Top Impact line - preserve as-is
        if ($line -like '*Top Impact*:*') {
            $topImpact = $line
            continue
        }

        # Section header (### ...)
        if ($line -match '^###\s+(.+)$') {
            $current = Get-SectionType $Matches[1]
            $skipSection = ($current -eq 'SKIP')
            continue
        }

        # Sub-header bold lines - skip
        if ($line -match '^\*\*.+\*\*:?\s*$') { continue }
        # "File modified:" single line
        if ($line -like '*Files modified*' -or $line -like '*File modified*') {
            $skipSection = $true
            continue
        }

        if ($skipSection) { continue }

        # Bullet lines
        if ($line -match '^\s*-\s+(.+)$') {
            if (Test-FileListing $line) { continue }

            $bullet = $Matches[1].Trim()
            if ($bullet.Length -lt 4) { continue }

            # Strip [Tag] prefixes like **[UI]**, **[Perf]**
            $bullet = $bullet -replace '^\*\*\[[^\]]+\]\*\*\s*', ''

            $bullet = "- $bullet"

            if ($map.ContainsKey($current) -and $map[$current].Count -lt $maxPerSection) {
                $map[$current].Add($bullet)
            }
        }
    }

    $out = [System.Collections.Generic.List[string]]::new()
    $out.Add($block.Heading)
    $out.Add('')

    if ($topImpact) {
        $out.Add($topImpact)
        $out.Add('')
    }

    foreach ($k in $ordered) {
        if (-not $map.ContainsKey($k) -or $map[$k].Count -eq 0) { continue }
        $out.Add("### $k")
        foreach ($b in $map[$k]) { $out.Add($b) }
        $out.Add('')
    }

    return (($out -join "`r`n").TrimEnd())
}

# --- Main ---

if (-not (Test-Path $fullPath)) {
    if (-not (Test-Path $targetPath)) {
        throw 'Neither CHANGELOG.full.md nor CHANGELOG.md found.'
    }
    Copy-Item $targetPath $fullPath -Force
    Write-Host 'Created CHANGELOG.full.md from current CHANGELOG.md'
}

$fullText = Get-Content $fullPath -Raw -Encoding UTF8
$blocks = Parse-Versions $fullText
if ($blocks.Count -eq 0) {
    throw 'No version sections found. Expected format: ## vX.Y.Z — YYYY-MM-DD (description)'
}

$selected = $blocks | Select-Object -First $Keep
$parts = @()
foreach ($b in $selected) {
    $parts += Summarize-Block $b
}

$headerText = "# Raiden Phim - Changelog`r`n`r`n> User-facing changes only. Full details: [CHANGELOG.full.md](CHANGELOG.full.md)`r`n`r`n---`r`n`r`n"
$footerText = "`r`n`r`n---`r`n`r`n> Older versions: see [CHANGELOG.full.md](CHANGELOG.full.md)`r`n"

$body = ($parts -join "`r`n`r`n---`r`n`r`n")
$result = $headerText + $body + $footerText

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($targetPath, $result, $utf8NoBom)

Write-Host "Trimmed CHANGELOG.md: $($selected.Count) version(s) from CHANGELOG.full.md"
