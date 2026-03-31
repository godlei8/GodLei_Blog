$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'import-local-env.ps1')
$backendDir = Join-Path $root 'GodLeiBlog-Backend'
$serverDir = Join-Path $backendDir 'blog-server'
$mvnw = Join-Path $backendDir 'mvnw.cmd'
$backendJar = Join-Path $serverDir 'target\blog-server-0.0.1-SNAPSHOT.jar'
$javaExe = Get-Command 'java.exe' -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source

if (-not (Test-Path -LiteralPath $mvnw)) {
  throw "Could not find Maven wrapper: $mvnw"
}

if (-not (Test-Path -LiteralPath $serverDir)) {
  throw "Could not find backend module directory: $serverDir"
}

if (-not $javaExe) {
  throw "Could not locate java.exe in PATH."
}

if (-not $env:SPRING_PROFILES_ACTIVE) {
  $env:SPRING_PROFILES_ACTIVE = 'local'
}

if (-not $env:HM_LOCAL_DB_HOST) {
  $env:HM_LOCAL_DB_HOST = 'localhost'
}

if (-not $env:HM_LOCAL_DB_USERNAME) {
  $env:HM_LOCAL_DB_USERNAME = 'root'
}

if (-not $env:HM_LOCAL_DB_PASSWORD -and $env:HM_DB_PASSWORD) {
  $env:HM_LOCAL_DB_PASSWORD = $env:HM_DB_PASSWORD
}

if (-not $env:HM_LOCAL_DATASOURCE_URL) {
  $env:HM_LOCAL_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/godleiblog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
}

if (-not $env:HM_LOCAL_UPLOAD_DIR) {
  $env:HM_LOCAL_UPLOAD_DIR = Join-Path $serverDir 'uploads'
}

if (-not $env:BLOG_STORAGE_MODE) {
  $env:BLOG_STORAGE_MODE = 'local'
}

Push-Location $serverDir
try {
  Push-Location $backendDir
  try {
    & $mvnw '-pl' 'blog-server' '-am' 'package' '-DskipTests'
  } finally {
    Pop-Location
  }

  if (-not (Test-Path -LiteralPath $backendJar)) {
    throw "Could not find backend jar after packaging: $backendJar"
  }

  & $javaExe '-Dfile.encoding=UTF-8' '-Duser.timezone=Asia/Shanghai' '-jar' $backendJar '--spring.profiles.active=local'
} finally {
  Pop-Location
}
