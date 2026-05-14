# AgenticCPS Docker Compose 从零部署教程

本文档面向第一次部署 AgenticCPS 的新手，推荐使用 `docker-compose` / `docker compose` 方式部署。

按照本文操作，你可以一键启动：

- MySQL 数据库
- Redis 缓存
- AgenticCPS 后端服务
- AgenticCPS 前端管理后台

## 1. 部署方式说明

AgenticCPS 推荐使用 Docker Compose 部署。

相比手动安装 Java、Maven、Node.js、MySQL、Redis，Docker Compose 更适合新手：

- 不需要单独配置复杂运行环境
- MySQL、Redis、后端、前端可以统一启动
- 停止、重启、查看日志都比较简单
- 本地测试和服务器部署都可以使用

## 2. 准备一台机器

你可以选择以下任意一种环境：

| 环境 | 是否推荐 | 说明 |
| --- | --- | --- |
| Windows 本地电脑 | 推荐 | 适合本地学习和测试 |
| Linux 云服务器 | 推荐 | 适合正式部署 |
| macOS | 可用 | 适合开发测试 |

服务器建议配置：

| 配置项 | 建议 |
| --- | --- |
| CPU | 2 核及以上 |
| 内存 | 4GB 及以上 |
| 磁盘 | 20GB 及以上 |
| 系统 | Ubuntu 20.04 / 22.04 / Debian / CentOS / Windows |

## 3. 安装 Docker

### 3.1 Windows 安装 Docker

Windows 推荐安装 Docker Desktop。

下载地址：

```text
https://www.docker.com/products/docker-desktop/
```

安装完成后，打开 Docker Desktop，等待 Docker 正常启动。

检查 Docker 是否安装成功：

```bash
docker --version
docker compose version
```

如果能看到版本号，说明安装成功。

### 3.2 Linux 安装 Docker

以 Ubuntu 为例：

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
```

启动 Docker：

```bash
sudo systemctl start docker
sudo systemctl enable docker
```

检查版本：

```bash
docker --version
docker compose version
```

如果普通用户执行 Docker 命令提示权限不足，可以执行：

```bash
sudo usermod -aG docker $USER
```

然后退出终端，重新登录服务器。

## 4. 获取项目代码

进入你准备存放项目的目录。

例如 Linux 服务器：

```bash
cd /opt
git clone <项目仓库地址> AgenticCPS
cd AgenticCPS
```

如果你是在 Windows 本地，项目可能位于：

```bash
cd F:/ai/AgenticCPS
```

## 5. 进入 Docker Compose 部署目录

进入 Docker 部署目录：

```bash
cd script/deploy/docker
```

查看当前目录文件：

```bash
ls
```

通常会看到类似文件：

```text
docker-compose.yml
docker.env
mysql/
nginx/
```

常见文件说明：

| 文件或目录 | 作用 |
| --- | --- |
| `docker-compose.yml` | Docker Compose 主配置文件 |
| `docker.env` | 环境变量配置 |
| `mysql/` | MySQL 初始化或配置文件 |
| `nginx/` | 前端或反向代理配置 |

## 6. 检查端口占用

AgenticCPS 默认可能会使用以下端口：

| 服务 | 默认端口 |
| --- | --- |
| 前端 | `8080` |
| 后端 | `48080` |
| MySQL | `3306` |
| Redis | `6379` |

如果这些端口已经被占用，需要修改 `docker-compose.yml` 里的端口映射。

例如：

```yaml
ports:
  - "8080:80"
```

表示：

```text
宿主机 8080 端口 -> 容器 80 端口
```

如果服务器已有服务占用 `8080`，可以改成：

```yaml
ports:
  - "18080:80"
```

访问地址就变成：

```text
http://服务器IP:18080
```

## 7. 配置环境变量

打开部署目录下的环境配置文件：

```text
script/deploy/docker/docker.env
```

重点检查以下配置：

```env
MYSQL_ROOT_PASSWORD=你的数据库root密码
MYSQL_DATABASE=ruoyi_vue_pro
MYSQL_USER=你的数据库用户名
MYSQL_PASSWORD=你的数据库密码

REDIS_PASSWORD=你的Redis密码
```

新手本地测试可以先保持默认配置。

如果是生产环境，必须修改默认密码。建议至少修改：

```env
MYSQL_ROOT_PASSWORD=请改成强密码
MYSQL_PASSWORD=请改成强密码
REDIS_PASSWORD=请改成强密码
```

强密码建议包含：

```text
大写字母 + 小写字母 + 数字 + 特殊符号
```

## 8. 构建后端 Jar 包

当前后端 Dockerfile 会复制已经构建好的 Jar 文件：

`	ext
backend/qiji-server/target/qiji-server.jar
`

所以第一次启动 Docker Compose 前，请先在项目根目录执行后端打包：

`ash
cd backend
mvn clean package -DskipTests
cd ../script/deploy/docker
`

说明：

`	ext
后端 Jar 包需要提前构建；前端管理后台不需要手动打包，qiji-admin 镜像构建时会自动执行 pnpm 构建。
`

## 9. 前端 Nginx 容器说明

当前 Docker Compose 已经包含前端管理后台容器：

`	ext
qiji-admin
`

它的工作方式是：

`	ext
1. 使用 Node.js + pnpm 构建 frontend/admin-vue3
2. 生成前端静态文件
3. 使用 Nginx 托管静态文件
4. 浏览器访问 8080 端口时进入前端页面
5. 前端请求 /admin-api/ 和 /app-api/ 时，由 Nginx 转发到 qiji-server:48080
`

对应配置文件：

`	ext
script/deploy/docker/qiji-ui-admin/Dockerfile
script/deploy/docker/qiji-ui-admin/nginx.conf
`

默认前端访问地址：

`	ext
http://localhost:8080
`

默认后端接口仍然暴露在：

`	ext
http://localhost:48080
`

如果是正式服务器部署，建议用户访问前端入口 8080，接口由前端 Nginx 自动转发。

## 10. 一键启动服务

确认当前目录是：

```text
script/deploy/docker
```

启动所有服务：

```bash
docker compose up -d
```

如果你的环境使用旧版 Compose，也可以执行：

```bash
docker-compose up -d
```

参数说明：

| 参数 | 说明 |
| --- | --- |
| `up` | 启动服务 |
| `-d` | 后台运行 |

## 11. 查看服务状态

执行：

```bash
docker compose ps
```

正常情况下，可以看到多个容器处于运行状态。

类似：

```text
NAME                STATUS
mysql               running
redis               running
server              running
admin               running
```

如果某个服务没有启动成功，可以查看日志。

## 12. 查看日志

查看全部服务日志：

```bash
docker compose logs -f
```

查看后端日志：

```bash
docker compose logs -f server
```

查看 MySQL 日志：

```bash
docker compose logs -f mysql
```

查看 Redis 日志：

```bash
docker compose logs -f redis
```

看到后端日志中出现类似内容，表示后端启动成功：

```text
Started YudaoServerApplication
```

## 13. 访问系统

### 13.1 本地部署访问

如果你是在自己电脑上部署，浏览器打开：

```text
http://localhost:8080
```

后端接口地址：

```text
http://localhost:48080
```

### 13.2 服务器部署访问

如果你是在云服务器上部署，浏览器打开：

```text
http://服务器IP:8080
```

例如：

```text
http://192.168.1.100:8080
```

后端接口地址：

```text
http://服务器IP:48080
```

## 14. 登录系统

默认管理员账号通常为：

```text
账号：admin
密码：admin
```

登录后，建议立即修改默认密码。

## 15. 云服务器开放端口

如果部署在云服务器，需要在安全组或防火墙中开放端口。

至少开放：

| 端口 | 用途 |
| --- | --- |
| `8080` | 前端访问 |
| `48080` | 后端接口访问 |

如果 MySQL 和 Redis 只给容器内部使用，不建议对外开放：

| 端口 | 建议 |
| --- | --- |
| `3306` | 不建议公网开放 |
| `6379` | 不建议公网开放 |

## 16. 停止服务

进入 Docker 部署目录：

```bash
cd script/deploy/docker
```

停止服务：

```bash
docker compose down
```

该命令会停止并删除容器，但默认不会删除数据库数据卷。

## 17. 重启服务

重启全部服务：

```bash
docker compose restart
```

只重启后端：

```bash
docker compose restart server
```

只重启前端：

```bash
docker compose restart admin
```

## 18. 更新项目后重新部署

如果代码更新了，可以按以下步骤重新部署。

进入项目根目录：

```bash
cd /opt/AgenticCPS
```

拉取最新代码：

```bash
git pull
```

进入 Docker 部署目录：

```bash
cd script/deploy/docker
```

重新构建并启动：

```bash
docker compose up -d --build
```

查看状态：

```bash
docker compose ps
```

查看后端日志：

```bash
docker compose logs -f server
```

## 19. 数据库数据说明

Docker Compose 部署时，MySQL 数据通常会保存在 Docker volume 或指定目录中。

只执行：

```bash
docker compose down
```

一般不会删除数据库数据。

但是执行：

```bash
docker compose down -v
```

会删除数据卷。

注意：

```text
docker compose down -v 会删除数据库数据，新手不要随便执行。
```

## 20. 备份数据库

建议生产环境定期备份数据库。

查看 MySQL 容器名称：

```bash
docker compose ps
```

假设 MySQL 容器名是 `mysql`，可以执行：

```bash
docker exec mysql mysqldump -uroot -p ruoyi_vue_pro > backup.sql
```

如果容器名称不同，请替换命令中的 `mysql`。

## 21. 恢复数据库

将备份文件恢复到数据库：

```bash
docker exec -i mysql mysql -uroot -p ruoyi_vue_pro < backup.sql
```

恢复前请确认：

- 数据库名称正确
- 备份文件正确
- 当前数据库是否可以被覆盖

## 22. 常见问题

### 22.1 docker compose 命令不存在

先检查：

```bash
docker compose version
```

如果不支持，尝试：

```bash
docker-compose version
```

如果 `docker-compose` 可用，则后续命令可以把：

```bash
docker compose
```

替换成：

```bash
docker-compose
```

### 22.2 端口被占用

报错示例：

```text
port is already allocated
```

说明端口已经被其他程序占用。

解决方法：

1. 找到占用端口的程序并关闭
2. 或修改 `docker-compose.yml` 端口映射

例如把：

```yaml
ports:
  - "8080:80"
```

改成：

```yaml
ports:
  - "18080:80"
```

然后重新启动：

```bash
docker compose up -d
```

访问地址改为：

```text
http://localhost:18080
```

### 22.3 后端一直启动失败

查看后端日志：

```bash
docker compose logs -f server
```

重点检查：

- 数据库是否启动成功
- Redis 是否启动成功
- 数据库账号密码是否正确
- MySQL 初始化 SQL 是否执行成功
- 后端配置里的数据库地址是否是容器服务名，而不是 `localhost`

在 Docker 容器内部，连接 MySQL 通常不能写：

```text
localhost
```

而应该写 Compose 服务名，例如：

```text
mysql
```

Redis 也是类似，通常使用：

```text
redis
```

### 22.4 前端页面打不开

请检查：

```bash
docker compose ps
```

确认前端容器是否运行。

再查看前端日志：

```bash
docker compose logs -f admin
```

同时确认访问端口是否正确：

```text
http://localhost:8080
```

如果修改过端口，例如改成 `18080`，则访问：

```text
http://localhost:18080
```

### 22.5 登录失败

请检查：

- 后端容器是否正常运行
- 数据库是否初始化成功
- 浏览器控制台是否有接口报错
- 前端请求的后端地址是否正确
- 默认账号密码是否被修改

默认账号通常是：

```text
账号：admin
密码：admin
```

## 23. 生产环境建议

如果准备正式上线，请至少完成以下事项：

```text
[ ] 修改默认管理员密码
[ ] 修改 MySQL root 密码
[ ] 修改业务数据库密码
[ ] 设置 Redis 密码
[ ] 不要公网开放 MySQL 3306
[ ] 不要公网开放 Redis 6379
[ ] 配置服务器防火墙
[ ] 配置域名
[ ] 配置 HTTPS
[ ] 配置数据库定时备份
[ ] 检查日志目录和磁盘空间
[ ] 配置 CPS 平台 API Key
```

生产环境不要继续使用：

```text
账号：admin
密码：admin
```

## 24. 常用命令汇总

启动：

```bash
docker compose up -d
```

停止：

```bash
docker compose down
```

重启：

```bash
docker compose restart
```

查看状态：

```bash
docker compose ps
```

查看全部日志：

```bash
docker compose logs -f
```

查看后端日志：

```bash
docker compose logs -f server
```

重新构建并启动：

```bash
docker compose up -d --build
```

删除容器但保留数据：

```bash
docker compose down
```

删除容器并删除数据卷：

```bash
docker compose down -v
```

注意：

```text
docker compose down -v 会删除数据库数据，请谨慎使用。
```

## 25. 新手部署检查清单

部署完成后，按下面清单检查：

```text
[ ] Docker 已安装
[ ] Docker Compose 已安装
[ ] 项目代码已拉取
[ ] 已进入 script/deploy/docker 目录
[ ] docker.env 已检查
[ ] 后端 Jar 包已构建
[ ] docker compose up -d 已执行
[ ] docker compose ps 显示服务运行中
[ ] 后端日志无明显报错
[ ] 浏览器可以打开前端页面
[ ] 可以看到登录页
[ ] 可以使用管理员账号登录
[ ] 登录后已修改默认密码
```

## 26. 推荐部署流程总结

从零部署时，按这个顺序执行即可：

```text
1. 准备服务器或本地电脑
2. 安装 Docker
3. 安装 Docker Compose
4. 拉取 AgenticCPS 项目代码
5. 进入 script/deploy/docker 目录
6. 检查 docker.env 配置
7. 执行 mvn clean package -DskipTests 构建后端 Jar
8. 执行 docker compose up -d
9. 执行 docker compose ps 查看状态
10. 执行 docker compose logs -f server 查看后端日志
11. 浏览器访问 http://服务器IP:8080
12. 使用 admin/admin 登录
13. 登录后修改默认密码
```
