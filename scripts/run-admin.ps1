$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$adminDir = Join-Path $root 'GodLeiBlog-AdminPanel'
$npmExe = Get-Command 'npm.cmd' -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source

if (-not $npmExe) {
  throw "Could not locate npm.cmd in PATH."
}

if (-not (Test-Path -LiteralPath $adminDir)) {
  throw "Could not find admin directory: $adminDir"
}

Push-Location $adminDir
try {
  & $npmExe 'run' 'dev' '--' '--host' '0.0.0.0' '--port' '5174'
} finally {
  Pop-Location
}
