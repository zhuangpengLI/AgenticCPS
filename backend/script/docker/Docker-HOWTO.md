# 可移植 Docker 部署

目标：在源码机器上构建一次，把整个 `backend/script/docker` 目录复制到部署服务器后直接启动，不需要在服务器安装 JDK、Maven、Node.js 或 pnpm。

## 目录结构

```text
docker/
├── backend/
│   ├── Dockerfile
│   └── qiji-server.jar          # build-package 脚本生成
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── dist/                    # build-package 脚本生成
├── mysql/init/
│   ├── 00-ruoyi-vue-pro.sql    # build-package 脚本生成
│   ├── 05-quartz.sql            # build-package 脚本生成
│   └── 10-cps-all-in-one.sql   # build-package 脚本生成
├── build-package.sh
├── build-package.ps1
├── deploy.sh
├── docker-compose.yml
└── docker.env
```

## 1. 在源码机器生成部署包

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

脚本会完成以下工作：

1. 构建 `backend/qiji-server/target/qiji-server.jar`。
2. 使用 Docker 专用的同源 API 配置构建管理前端。
3. 将 JAR、前端页面、基础 SQL、Quartz SQL 和 CPS 全量 SQL 复制到当前 Docker 目录。
4. 本机安装了 Docker Compose 时，额外校验 Compose 配置。

如果 CI 已经完成后端和前端构建，可使用 `bash build-package.sh --skip-build` 或 `./build-package.ps1 -SkipBuild`，只执行制品归集和校验。

## 2. 上传并启动

把整个 `backend/script/docker` 目录复制到部署服务器，在目录内执行：

```bash
bash deploy.sh
```

常用命令：

```bash
bash deploy.sh status
bash deploy.sh logs server
bash deploy.sh restart
bash deploy.sh down
```

`deploy.sh` 会先检查 JAR、前端首页和初始化 SQL 是否齐全，然后构建轻量运行镜像并启动 MySQL、Redis、后端和管理前端。

## 3. 访问地址与配置

- 管理后台：`http://服务器地址:8080`
- 后端 API：`http://服务器地址:48080`
- MySQL：服务器端口 `3306`
- Redis：服务器端口 `6379`

端口、数据库账号密码、Redis 密码和 JVM 参数统一在 `docker.env` 中修改。生产部署前必须修改所有 `ChangeMe_` 开头的默认密码，并根据网络策略限制 MySQL、Redis 和后端端口的公网访问。

后端固定使用 `prod` profile。凭据传递关系如下：

- `MYSQL_USER` / `MYSQL_PASSWORD` 同时用于创建 MySQL 应用账号，并传给 Java 的主从数据源。
- `REDIS_USERNAME` / `REDIS_PASSWORD` 同时用于启动 Redis 鉴权，并传给 Spring Data Redis。
- 容器内服务端口固定为 MySQL `3306`、Redis `6379`、Java `48080`；`*_HOST_PORT` 只控制宿主机映射，不会破坏容器间连接。
- Java 生产配置位于 `backend/qiji-server/src/main/resources/application-prod.yaml`，敏感字段不在 YAML 中写死，只从容器环境变量读取。

MySQL 初始化 SQL 和应用账号只会在首次创建数据卷时生效。已有数据库升级应执行 `backend/sql/module/cps-update.sql` 对应发布区块；如果已有数据卷曾使用不同账号密码，还需要在数据库内同步修改账号，不能只修改 `docker.env`。

部署服务器需要 Docker Engine、Docker Compose v2，并能拉取 MySQL、Redis、Eclipse Temurin 和 Nginx 基础镜像。
