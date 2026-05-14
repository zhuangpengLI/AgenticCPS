# Docker Build & Up

目标: 快速部署体验系统，帮助了解系统之间的依赖关系。
依赖：Docker Compose v2。

## 功能文件列表

```text
.
├── Docker-HOWTO.md                 
├── docker-compose.yml              
├── docker.env                      <-- 提供docker-compose环境变量配置
└── qiji-ui-admin
    ├── Dockerfile
    ├── Dockerfile.dockerignore
    └── nginx.conf                  <-- Nginx 静态资源托管、gzip压缩、api转发
```

后端镜像使用 `backend/qiji-server/Dockerfile` 构建，前端镜像使用 `frontend/admin-vue3` 源码构建。

## 构建 jar 包

```shell
cd ../../../backend
mvn clean package -DskipTests
cd ../script/deploy/docker
```

## 构建启动服务

```shell
docker compose --env-file docker.env up -d
```

首次运行会自动构建容器。可以通过`docker compose build [service]`来手动构建所有或某个docker镜像。

前端管理后台由 `qiji-admin` 容器提供，构建阶段使用 Node.js + pnpm 打包 `frontend/admin-vue3`，运行阶段使用 Nginx 托管静态资源，并将 `/admin-api/`、`/app-api/` 反向代理到 `qiji-server:48080`。

`--env-file docker.env`为可选参数，只是展示了通过`.env`文件配置容器启动的环境变量，`docker-compose.yml`本身已经提供足够的默认参数来正常运行系统。

## 服务器的宿主机端口映射

- admin ui: http://localhost:8080
- api server: http://localhost:48080
- mysql: root/123456, port: 3306
- redis: port: 6379
