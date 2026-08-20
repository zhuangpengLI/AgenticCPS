param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ScriptDir = $PSScriptRoot
$BackendRoot = (Resolve-Path (Join-Path $ScriptDir "../..")).Path
$ProjectRoot = (Resolve-Path (Join-Path $BackendRoot "..")).Path
$FrontendRoot = Join-Path $ProjectRoot "frontend/admin-vue3"

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)][string]$Command,
        [Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments
    )
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "命令执行失败（退出码 $LASTEXITCODE）：$Command $Arguments"
    }
}

if ($SkipBuild) {
    Write-Host "[1/4] 跳过构建，使用已有制品"
} else {
    Write-Host "[1/4] 构建后端 JAR"
    Invoke-Checked mvn -f (Join-Path $BackendRoot "pom.xml") clean package -pl qiji-server -am -DskipTests

    Write-Host "[2/4] 构建管理前端"
    Push-Location $FrontendRoot
    try {
        Invoke-Checked pnpm install --frozen-lockfile
        Invoke-Checked pnpm build:docker
    } finally {
        Pop-Location
    }
}

$BackendJar = Join-Path $BackendRoot "qiji-server/target/qiji-server.jar"
$FrontendDist = Join-Path $FrontendRoot "dist-prod"
if (-not (Test-Path -LiteralPath $BackendJar -PathType Leaf)) {
    throw "未找到后端构建产物: $BackendJar"
}
if (-not (Test-Path -LiteralPath (Join-Path $FrontendDist "index.html") -PathType Leaf)) {
    throw "未找到前端构建产物: $FrontendDist/index.html"
}

Write-Host "[3/4] 归集后端、前端和数据库初始化文件"
$DeployBackend = Join-Path $ScriptDir "backend"
$DeployFrontendDist = Join-Path $ScriptDir "frontend/dist"
$DeployMysqlInit = Join-Path $ScriptDir "mysql/init"
New-Item -ItemType Directory -Force -Path $DeployBackend, $DeployMysqlInit | Out-Null
if (Test-Path -LiteralPath $DeployFrontendDist) {
    Remove-Item -LiteralPath $DeployFrontendDist -Recurse -Force
}
Copy-Item -LiteralPath $FrontendDist -Destination $DeployFrontendDist -Recurse
Copy-Item -LiteralPath $BackendJar -Destination (Join-Path $DeployBackend "qiji-server.jar") -Force
Copy-Item -LiteralPath (Join-Path $BackendRoot "sql/mysql/ruoyi-vue-pro.sql") -Destination (Join-Path $DeployMysqlInit "00-ruoyi-vue-pro.sql") -Force
Copy-Item -LiteralPath (Join-Path $BackendRoot "sql/mysql/quartz.sql") -Destination (Join-Path $DeployMysqlInit "05-quartz.sql") -Force
Copy-Item -LiteralPath (Join-Path $BackendRoot "sql/module/cps-all-in-one.sql") -Destination (Join-Path $DeployMysqlInit "10-cps-all-in-one.sql") -Force
Copy-Item -LiteralPath (Join-Path $BackendRoot "sql/module/ai-all.sql") -Destination (Join-Path $DeployMysqlInit "15-ai-all.sql") -Force

Write-Host "[4/4] 校验部署目录"
if (Get-Command docker -ErrorAction SilentlyContinue) {
    & docker compose version *> $null
    if ($LASTEXITCODE -eq 0) {
        Push-Location $ScriptDir
        try {
            Invoke-Checked docker compose --env-file docker.env config --quiet
        } finally {
            Pop-Location
        }
    } else {
        Write-Host "未检测到 Docker Compose，跳过 Compose 配置校验。"
    }
} else {
    Write-Host "未检测到 Docker Compose，跳过 Compose 配置校验。"
}

Write-Host "部署包准备完成: $ScriptDir"
Write-Host "复制整个 docker 目录到服务器后，执行: bash deploy.sh"
