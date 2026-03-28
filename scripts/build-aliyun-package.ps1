param(
    [string]$OutputRoot = (Join-Path $PSScriptRoot "..\\release")
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$packageName = "GodLeiBlog-aliyun-ecs-$timestamp"
$stageDir = Join-Path $OutputRoot $packageName
$zipPath = Join-Path $OutputRoot "$packageName.zip"

$frontendDist = Join-Path $repoRoot "GodLeiBlog-Frontend\\dist"
$adminDist = Join-Path $repoRoot "GodLeiBlog-AdminPanel\\dist"
$backendJar = Join-Path $repoRoot "GodLeiBlog-Backend\\blog-server\\target\\blog-server-0.0.1-SNAPSHOT.jar"
$deployRoot = Join-Path $repoRoot "deploy\\aliyun-ecs"
$sqlMain = (Get-ChildItem -LiteralPath $repoRoot -Filter "*.sql" | Select-Object -First 1).FullName
$sqlExtra = Join-Path $repoRoot "scripts\\mysql-site-config-media.sql"

$requiredPaths = @(
    $frontendDist,
    $adminDist,
    $backendJar,
    $deployRoot,
    $sqlMain,
    $sqlExtra
)

foreach ($path in $requiredPaths) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required path not found: $path"
    }
}

if (Test-Path -LiteralPath $stageDir) {
    Remove-Item -LiteralPath $stageDir -Recurse -Force
}

if (Test-Path -LiteralPath $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}

New-Item -ItemType Directory -Force -Path $stageDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stageDir "backend") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stageDir "nginx") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stageDir "scripts") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stageDir "sql") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stageDir "systemd") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stageDir "www") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stageDir "www\\admin") | Out-Null

Copy-Item -LiteralPath $backendJar -Destination (Join-Path $stageDir "backend\\app.jar") -Force
Copy-Item -LiteralPath (Join-Path $deployRoot "backend\\backend.env.example") -Destination (Join-Path $stageDir "backend\\backend.env.example") -Force
Copy-Item -LiteralPath (Join-Path $deployRoot "README.md") -Destination (Join-Path $stageDir "README.md") -Force
Copy-Item -LiteralPath (Join-Path $deployRoot "nginx\\godleiblog.conf") -Destination (Join-Path $stageDir "nginx\\godleiblog.conf") -Force
Copy-Item -LiteralPath (Join-Path $deployRoot "systemd\\godleiblog-backend.service") -Destination (Join-Path $stageDir "systemd\\godleiblog-backend.service") -Force
Copy-Item -LiteralPath $sqlMain -Destination (Join-Path $stageDir "sql\\GodLeiBlog建表.sql") -Force
Copy-Item -LiteralPath $sqlExtra -Destination (Join-Path $stageDir "sql\\mysql-site-config-media.sql") -Force

Copy-Item -LiteralPath (Join-Path $deployRoot "scripts\\install.sh") -Destination (Join-Path $stageDir "scripts\\install.sh") -Force
Copy-Item -LiteralPath (Join-Path $deployRoot "scripts\\start-backend.sh") -Destination (Join-Path $stageDir "scripts\\start-backend.sh") -Force
Copy-Item -LiteralPath (Join-Path $deployRoot "scripts\\stop-backend.sh") -Destination (Join-Path $stageDir "scripts\\stop-backend.sh") -Force
Copy-Item -LiteralPath (Join-Path $deployRoot "scripts\\status-backend.sh") -Destination (Join-Path $stageDir "scripts\\status-backend.sh") -Force

Copy-Item -Path (Join-Path $frontendDist "*") -Destination (Join-Path $stageDir "www") -Recurse -Force
Copy-Item -Path (Join-Path $adminDist "*") -Destination (Join-Path $stageDir "www\\admin") -Recurse -Force

Compress-Archive -Path $stageDir -DestinationPath $zipPath -Force

Write-Output "Package directory: $stageDir"
Write-Output "Package zip: $zipPath"
