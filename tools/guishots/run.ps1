# Runs a screenshot script in the dev client (run guiShots) and prints the report.
# Usage: pwsh tools/guishots/run.ps1 <script.txt>   (pictures land in run-guishots/screenshots/)
param([Parameter(Mandatory = $true)][string]$Script)

$ErrorActionPreference = 'Stop'
$mod = Resolve-Path (Join-Path $PSScriptRoot '../..')
$game = Join-Path $mod 'run-guishots'
$shots = Join-Path $game 'screenshots'
New-Item -ItemType Directory -Force $shots | Out-Null
Remove-Item (Join-Path $shots 'guishots-report.txt') -ErrorAction SilentlyContinue
Set-Content -Path (Join-Path $game 'job.txt') -Value $Script -NoNewline

Push-Location $mod
try {
    & ./gradlew.bat runGuiShots *> (Join-Path $game 'launch.log')
} finally {
    Pop-Location
}
Get-Content (Join-Path $shots 'guishots-report.txt') -ErrorAction SilentlyContinue
