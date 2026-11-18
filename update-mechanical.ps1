# Downloads the latest Mechanical release from GitHub into your mods folder and removes the old jar,
# and puts the item register (registry.html) next to this script.
# Run it with update-mechanical.cmd (double-click). The first time it asks for the mods folder and remembers it.

$ErrorActionPreference = 'Stop'
$Repo = 'datreindeer/project_minecraft_mod'
$ConfigFile = Join-Path $PSScriptRoot 'update-mechanical.txt'

if (Test-Path $ConfigFile) {
    $ModsDir = (Get-Content $ConfigFile -Raw).Trim()
} else {
    $Default = Join-Path $env:APPDATA '.minecraft\mods'
    $ModsDir = Read-Host "Mods folder (Enter = $Default)"
    if ([string]::IsNullOrWhiteSpace($ModsDir)) { $ModsDir = $Default }
    $ModsDir = $ModsDir.Trim('"', ' ')
    Set-Content -Path $ConfigFile -Value $ModsDir -Encoding utf8
}
if (-not (Test-Path $ModsDir)) { New-Item -ItemType Directory -Force $ModsDir | Out-Null }

Write-Host "Checking the latest version..."
$Release = Invoke-RestMethod -Uri "https://api.github.com/repos/$Repo/releases/latest" -Headers @{ 'User-Agent' = 'mechanical-updater' }
$Asset = $Release.assets | Where-Object { $_.name -like 'mechanical-*.jar' } | Select-Object -First 1
if (-not $Asset) { throw "The latest release ($($Release.tag_name)) has no mechanical jar." }

$Target = Join-Path $ModsDir $Asset.name
if (Test-Path $Target) {
    Write-Host "Already up to date: $($Asset.name)"
} else {
    $Temp = "$Target.download"
    Invoke-WebRequest -Uri $Asset.browser_download_url -OutFile $Temp -Headers @{ 'User-Agent' = 'mechanical-updater' }
    # only now that the download worked: remove old versions, put the new one in place
    Get-ChildItem -Path $ModsDir -Filter 'mechanical-*.jar' | Remove-Item -Force
    Move-Item -Path $Temp -Destination $Target
    Write-Host "Installed $($Asset.name) in $ModsDir"
}
# the item register (registry.html) goes next to this script
$Register = $Release.assets | Where-Object { $_.name -eq 'registry.html' } | Select-Object -First 1
if ($Register) {
    Invoke-WebRequest -Uri $Register.browser_download_url -OutFile (Join-Path $PSScriptRoot 'registry.html') -Headers @{ 'User-Agent' = 'mechanical-updater' }
    Write-Host "Updated registry.html (open it in a browser to look up items)"
}
Write-Host "(To change the mods folder, delete $ConfigFile)"
Read-Host "Press Enter to close"
