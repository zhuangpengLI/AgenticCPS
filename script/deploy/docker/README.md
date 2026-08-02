# Docker 部署目录已迁移

Docker 部署入口统一为：

```text
backend/script/docker
```

请在新目录执行 `build-package.sh` 或 `build-package.ps1` 生成可移植部署包。不要继续维护本目录的旧 Compose 副本，避免两套部署配置漂移。
