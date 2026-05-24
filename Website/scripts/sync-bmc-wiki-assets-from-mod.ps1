# Copies bmcmod PNG textures from the mod sources into Website/assets/bmc-wiki/bmcmod/
# Run from repo root after changing mod textures. Requires: src pou créer le wiki/.../textures/

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$wikiFolder = Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
  Where-Object { $_.Name -like 'src*wiki' } |
  Select-Object -First 1
if (-not $wikiFolder) {
  Write-Error "Could not find mod source folder (expected name like 'src*wiki') under: $root"
}
$modTextures = Join-Path $wikiFolder.FullName 'src\main\resources\assets\bmcmod\textures'
$dstRoot = Join-Path $root 'Website\assets\bmc-wiki\bmcmod'

if (-not (Test-Path -LiteralPath $modTextures)) {
  Write-Error "Mod textures folder not found: $modTextures"
}

foreach ($sub in @('block', 'item')) {
  $src = Join-Path $modTextures $sub
  if (-not (Test-Path $src)) { continue }
  $dst = Join-Path $dstRoot $sub
  New-Item -ItemType Directory -Force -Path $dst | Out-Null
  Copy-Item (Join-Path $src '*.png') $dst -Force
}

$nBlock = (Get-ChildItem (Join-Path $dstRoot 'block') -Filter '*.png' -ErrorAction SilentlyContinue).Count
$nItem = (Get-ChildItem (Join-Path $dstRoot 'item') -Filter '*.png' -ErrorAction SilentlyContinue).Count
Write-Host "Synced bmcmod textures -> $dstRoot (block=$nBlock, item=$nItem)"
