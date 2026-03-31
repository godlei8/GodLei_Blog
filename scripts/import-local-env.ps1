$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$localEnvFile = Join-Path $root '.run\local-dev-env.ps1'

if (Test-Path -LiteralPath $localEnvFile) {
  . $localEnvFile
}
