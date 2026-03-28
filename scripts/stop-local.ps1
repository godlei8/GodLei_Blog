$ErrorActionPreference = 'SilentlyContinue'

$ports = 5173, 5174, 8080

foreach ($port in $ports) {
  $procIds = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)
  foreach ($procId in $procIds) {
    Stop-Process -Id $procId -Force
  }
}

Write-Host 'Local frontend, admin, and backend services have been stopped. The existing MySQL 3306 instance was left untouched.'
