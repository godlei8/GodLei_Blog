$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

$root = Split-Path -Parent $PSScriptRoot
$backendDir = Get-ChildItem -Path $root -Directory -Filter '*Blog-Backend' | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
$frontendDir = Get-ChildItem -Path $root -Directory -Filter '*Blog-Frontend' | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
$adminDir = Get-ChildItem -Path $root -Directory -Filter '*Blog-AdminPanel' | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
$backendJar = Join-Path $backendDir 'blog-server\target\blog-server-0.0.1-SNAPSHOT.jar'
$logDir = Join-Path $root '.run'

$dbHost = if ($env:HM_LOCAL_DB_HOST) { $env:HM_LOCAL_DB_HOST } else { 'localhost' }
$dbName = if ($env:HM_LOCAL_DB_NAME) { $env:HM_LOCAL_DB_NAME } else { 'godleiblog' }
$dbUser = if ($env:HM_LOCAL_DB_USERNAME) { $env:HM_LOCAL_DB_USERNAME } else { 'root' }
$dbPassword = if ($env:HM_LOCAL_DB_PASSWORD) {
  $env:HM_LOCAL_DB_PASSWORD
} elseif ($env:HM_DB_PASSWORD) {
  $env:HM_DB_PASSWORD
} else {
  ''
}
$dbPort = if ($env:HM_LOCAL_DB_PORT) { [int]$env:HM_LOCAL_DB_PORT } else { 3306 }

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Resolve-ExecutablePath {
  param(
    [string]$CommandName,
    [string[]]$CandidatePaths = @()
  )

  foreach ($candidate in $CandidatePaths) {
    if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath $candidate)) {
      return (Resolve-Path -LiteralPath $candidate).Path
    }
  }

  $command = Get-Command $CommandName -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($command) {
    return $command.Source
  }

  $checked = ($CandidatePaths | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join ', '
  throw "Could not locate executable '$CommandName'. Checked: $checked"
}

function Test-PortListening {
  param([int]$Port)
  return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Get-MySqlArguments {
  param([string]$Database = '')

  $arguments = @(
    '--protocol=TCP',
    "--host=$dbHost",
    "--port=$dbPort",
    "--user=$dbUser",
    '--default-character-set=utf8mb4'
  )

  if ($Database) {
    $arguments += "--database=$Database"
  }

  return $arguments
}

function Test-DatabaseLogin {
  param([string]$Database = '')

  $arguments = Get-MySqlArguments -Database $Database
  $arguments += @('-e', 'SELECT 1;')
  & $mysqlExe @arguments *> $null
  return $LASTEXITCODE -eq 0
}

function Ensure-AppDatabase {
  $arguments = Get-MySqlArguments
  $arguments += @(
    '-e',
    "CREATE DATABASE IF NOT EXISTS $dbName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  )
  & $mysqlExe @arguments
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to create or verify database '$dbName' using '$dbUser' on $dbHost`:$dbPort."
  }
}

function Test-SchemaReady {
  $sql = "SELECT 1 FROM information_schema.tables WHERE table_schema = '$dbName' AND table_name = 'blog_post';"
  $arguments = Get-MySqlArguments -Database 'information_schema'
  $arguments += @('-Nse', $sql)
  $result = & $mysqlExe @arguments 2>$null
  return $LASTEXITCODE -eq 0 -and (($result -join '') -eq '1')
}

function Initialize-BlogSchema {
  if (-not (Test-Path -LiteralPath $schemaFile)) {
    throw "Could not find schema file: $schemaFile"
  }

  $arguments = Get-MySqlArguments
  Get-Content -Raw -LiteralPath $schemaFile -Encoding utf8 | & $mysqlExe @arguments
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to initialize schema from '$schemaFile'."
  }
}

if (-not $backendDir -or -not $frontendDir -or -not $adminDir) {
  throw 'Could not locate blog module directories under the workspace root.'
}

$schemaFile = Get-ChildItem -Path $root -File -Filter '*.sql' |
  Where-Object { $_.Name -ne 'mysql-init.sql' } |
  Sort-Object Name |
  Select-Object -First 1 -ExpandProperty FullName

if (-not $schemaFile) {
  throw 'Could not locate the database schema SQL file under the workspace root.'
}

$mysqlExe = Resolve-ExecutablePath -CommandName 'mysql.exe' -CandidatePaths @(
  $env:HM_MYSQL_EXE,
  $(if ($env:HM_MYSQL_BIN_DIR) { Join-Path $env:HM_MYSQL_BIN_DIR 'mysql.exe' }),
  'D:\Kf\mysql\bin\mysql.exe'
)

$javaExe = Resolve-ExecutablePath -CommandName 'java.exe' -CandidatePaths @(
  $env:HM_JAVA_EXE,
  $(if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' }),
  'D:\Kf\jdk21\bin\java.exe'
)

$npmExe = Resolve-ExecutablePath -CommandName 'npm.cmd'

if (-not [string]::IsNullOrEmpty($dbPassword)) {
  # Avoid mysql.exe stderr warnings about command-line passwords, which some
  # PowerShell hosts surface as terminating errors.
  $env:MYSQL_PWD = $dbPassword
}

if (-not (Test-PortListening $dbPort)) {
  throw "MySQL is not listening on $dbHost`:$dbPort. Please start your existing MySQL instance first."
}

if (-not (Test-DatabaseLogin)) {
  throw "Could not log in to MySQL as '$dbUser' on $dbHost`:$dbPort. Set HM_LOCAL_DB_PASSWORD if your root account has a password."
}

Ensure-AppDatabase

if (-not (Test-DatabaseLogin -Database $dbName)) {
  throw "Could not connect to database '$dbName' as '$dbUser'."
}

if (-not (Test-SchemaReady)) {
  Initialize-BlogSchema
}

$env:HM_LOCAL_DB_HOST = $dbHost
$env:HM_LOCAL_DATASOURCE_URL = "jdbc:mysql://${dbHost}:$dbPort/$dbName"
$env:HM_LOCAL_DB_USERNAME = $dbUser
$env:HM_LOCAL_DB_PASSWORD = $dbPassword

if (-not (Test-PortListening 8080)) {
  Push-Location $backendDir
  try {
    & '.\mvnw.cmd' '-pl' 'blog-server' '-am' 'package' '-DskipTests' *> (Join-Path $logDir 'backend-build.log')
    if ($LASTEXITCODE -ne 0) {
      throw 'Backend Maven package step failed. See .run\backend-build.log for details.'
    }
  } finally {
    Pop-Location
  }

  Start-Process -FilePath $javaExe `
    -ArgumentList '-jar', $backendJar, '--spring.profiles.active=local' `
    -WorkingDirectory (Join-Path $backendDir 'blog-server') `
    -RedirectStandardOutput (Join-Path $logDir 'backend-dev.out.log') `
    -RedirectStandardError (Join-Path $logDir 'backend-dev.err.log') | Out-Null
}

if (-not (Test-PortListening 5173)) {
  Start-Process -FilePath $npmExe `
    -ArgumentList 'run', 'dev', '--', '--host', '0.0.0.0', '--port', '5173' `
    -WorkingDirectory $frontendDir `
    -RedirectStandardOutput (Join-Path $logDir 'frontend-dev.out.log') `
    -RedirectStandardError (Join-Path $logDir 'frontend-dev.err.log') | Out-Null
}

if (-not (Test-PortListening 5174)) {
  Start-Process -FilePath $npmExe `
    -ArgumentList 'run', 'dev', '--', '--host', '0.0.0.0', '--port', '5174' `
    -WorkingDirectory $adminDir `
    -RedirectStandardOutput (Join-Path $logDir 'admin-dev.out.log') `
    -RedirectStandardError (Join-Path $logDir 'admin-dev.err.log') | Out-Null
}

Write-Host 'Local services are starting:'
Write-Host '  Frontend: http://localhost:5173/'
Write-Host '  Admin:    http://localhost:5174/admin/'
Write-Host '  Backend:  http://localhost:8080/'
Write-Host "  MySQL:    $dbHost`:$dbPort ($dbName / $dbUser)"
