# AgenticCPS Docker Compose 从零部署教程

本文介绍可移植部署方式：源码机器负责构建，部署服务器只需要 Docker。构建完成后，把整个 `backend/script/docker` 目录上传到服务器即可一键启动 MySQL、Redis、后端服务和前端管理后台。

## 1. 环境要求

源码机器需要：

- JDK 17 或 21
- Maven 3.8+
- Node.js 16+
- pnpm 8.6+

部署服务器需要：

- Docker Engine
- Docker Compose v2，可通过 `docker compose version` 验证
- 能拉取 MySQL、Redis、Eclipse Temurin 和 Nginx 基础镜像

建议服务器至少 2 核 CPU、4 GB 内存和 20 GB 可用磁盘。

## 2. 在源码机器生成部署目录

Linux、macOS 或 Git Bash：

```bash
cd backend/script/docker
bash build-package.sh
```

Windows PowerShell：

```powershell
cd backend/script/docker
./build-package.ps1
```

构建脚本会：

1. 打包后端并复制为 `backend/qiji-server.jar`。
2. 使用同源 API 配置打包前端并复制到 `frontend/dist/`。
3. 复制基础库、Quartz 和 CPS 新库初始化 SQL 到 `mysql/init/`。
4. 本机存在 Docker Compose 时校验 `docker-compose.yml`。

如果流水线已经生成 JAR 和 `dist-prod`，可使用 `bash build-package.sh --skip-build` 或 `./build-package.ps1 -SkipBuild` 只归集现有制品。

成功后的关键文件如下：

```text
backend/script/docker/
├── backend/qiji-server.jar
├── frontend/dist/index.html
├── mysql/init/00-ruoyi-vue-pro.sql
├── mysql/init/05-quartz.sql
├── mysql/init/10-cps-all-in-one.sql
├── docker-compose.yml
├── docker.env
└── deploy.sh
```

缺少上述任一文件时，不要上传部署。

## 3. 修改服务器配置

编辑 `backend/script/docker/docker.env`：

```dotenv
ADMIN_HOST_PORT=8080
SERVER_HOST_PORT=48080
MYSQL_HOST_PORT=3306
REDIS_HOST_PORT=6379
MYSQL_DATABASE=ruoyi-vue-pro
MYSQL_ROOT_PASSWORD=请替换为强密码
MYSQL_USER=qiji
MYSQL_PASSWORD=请替换为应用账号强密码
REDIS_USERNAME=default
REDIS_PASSWORD=请替换为Redis强密码
SPRING_PROFILES_ACTIVE=prod
```

生产环境必须修改 `docker.env` 中所有 `ChangeMe_` 开头的密码。MySQL 容器和 Java 数据源共用 `MYSQL_USER` / `MYSQL_PASSWORD`，Redis 容器和 Spring Data Redis 共用 `REDIS_USERNAME` / `REDIS_PASSWORD`，不需要重复维护两套凭据。

`*_HOST_PORT` 只控制服务器对外端口；容器内部仍使用固定服务名和端口，因此修改宿主机端口不会影响后端连接。若 MySQL、Redis 和后端 API 不需要公网直连，应通过服务器防火墙限制其端口，只开放管理前端端口。

前端 API 地址在构建时固定为同源 `/admin-api`，由前端 Nginx 代理到 `qiji-server:48080`；上传服务器后不需要再修改前端文件。

## 4. 上传部署目录

可以使用 SCP、SFTP、Rsync 或服务器管理面板上传整个目录。例如：

```bash
ssh user@server 'mkdir -p /opt/agentic-cps'
scp -r backend/script/docker user@server:/opt/agentic-cps/
```

必须上传整个目录，不能只上传 Compose 文件。

## 5. 一键启动

登录部署服务器后执行：

```bash
cd /opt/agentic-cps/docker
bash deploy.sh
```

脚本会先检查 JAR、前端首页和 SQL，再执行 Compose 配置校验、构建运行镜像并后台启动服务。

常用运维命令：

```bash
# 查看服务状态
bash deploy.sh status

# 查看全部日志
bash deploy.sh logs

# 只查看后端日志
bash deploy.sh logs server

# 重建并重启
bash deploy.sh restart

# 替换 backend/qiji-server.jar 或 frontend/dist 后发布
bash deploy.sh redeploy

# 停止服务，保留数据库和 Redis 数据卷
bash deploy.sh down
```

默认访问地址：

- 管理后台：`http://服务器地址:8080`
- 后端 API：`http://服务器地址:48080`
- MySQL：`服务器地址:3306`
- Redis：`服务器地址:6379`

## 6. 数据库初始化与升级

`mysql/init/` 下的 SQL、`MYSQL_USER` 和 `MYSQL_PASSWORD` 只会在 MySQL 数据卷首次创建时生效。首次启动后，后续重启不会重复初始化或自动修改已有数据库账号。

后端 JAR 和前端页面是部署目录的只读挂载：`backend/qiji-server.jar` 映射到容器 `/opt/qiji-server/app.jar`，`frontend/dist/` 映射到 `/usr/share/nginx/html`。重新上传制品后执行 `bash deploy.sh redeploy`，无需手工进入容器或删除数据卷。

已有数据库升级不能通过替换 `10-cps-all-in-one.sql` 完成，应按发布说明执行源码中的 `backend/sql/module/cps-update.sql` 对应增量区块，并在操作前备份数据库。

如果明确要销毁本地测试数据并重新初始化，需要手动删除 Compose 数据卷；这是不可恢复操作，不属于日常重启流程。

## 7. 常见问题

### 提示缺少部署文件

回到源码机器重新运行 `build-package.sh` 或 `build-package.ps1`，确认构建成功后再次完整上传 Docker 目录。

### 端口被占用

修改 `docker.env` 中对应的宿主机端口，然后执行 `bash deploy.sh restart`。

### 后端等待数据库

Compose 已为 MySQL 和 Redis 配置健康检查，只有依赖服务健康后才启动后端。可通过 `bash deploy.sh logs mysql` 和 `bash deploy.sh logs server` 定位问题。

### 外部 MCP 不可用导致后端启动失败

生产配置默认关闭启动阶段的 MCP ToolCallback 聚合：外部 MCP 会在应用就绪后异步连接，远程 SSE 不可用不会阻塞 Java 包启动。可在 `docker.env` 中调整：

```dotenv
AI_MCP_CLIENT_TOOLCALLBACK_ENABLED=false
HAINA_MCP_ENABLED=true
```

修改后执行 `bash deploy.sh redeploy`。海纳 API Key 应通过服务器环境变量或安全配置注入，不要提交到仓库。

### 前端打开后接口失败

确认 `qiji-server` 容器已启动，并检查 `bash deploy.sh logs server`。Docker 专用前端构建不会使用 `localhost:48080`，浏览器请求会经当前前端域名的 `/admin-api` 转发。
