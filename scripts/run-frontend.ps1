$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$frontendDir = Join-Path $root 'GodLeiBlog-Frontend'
$npmExe = Get-Command 'npm.cmd' -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source

if (-not $npmExe) {
  throw "Could not locate npm.cmd in PATH."
}

if (-not (Test-Path -LiteralPath $frontendDir)) {
  throw "Could not find frontend directory: $frontendDir"
}

Push-Location $frontendDir
try {
  & $npmExe 'run' 'dev' '--' '--host' '0.0.0.0' '--port' '5173'
} finally {
  Pop-Location
}
