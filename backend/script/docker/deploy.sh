#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

compose() {
  docker compose --env-file docker.env "$@"
}

validate_artifacts() {
  local missing=0
  for artifact in \
    "backend/qiji-server.jar" \
    "frontend/dist/index.html" \
    "mysql/init/00-ruoyi-vue-pro.sql" \
    "mysql/init/05-quartz.sql" \
    "mysql/init/10-cps-all-in-one.sql"; do
    if [[ ! -f "${artifact}" ]]; then
      echo "缺少部署文件: ${artifact}" >&2
      missing=1
    fi
  done
  if [[ "${missing}" -ne 0 ]]; then
    echo "请先在源码仓库执行 build-package.sh 或 build-package.ps1，再复制整个 docker 目录。" >&2
    exit 1
  fi
}

action="${1:-up}"
case "${action}" in
  up|start)
    validate_artifacts
    compose config --quiet
    compose up -d --build
    compose ps
    ;;
  restart)
    validate_artifacts
    compose config --quiet
    compose down
    compose up -d --build
    compose ps
    ;;
  redeploy|refresh)
    validate_artifacts
    compose config --quiet
    # JAR 和 dist 通过 bind mount 注入容器，只重建应用服务，保留数据库/Redis 数据卷。
    compose up -d --build --force-recreate server admin
    compose ps
    ;;
  down|stop)
    compose down
    ;;
  status|ps)
    compose ps
    ;;
  logs)
    shift
    compose logs -f "$@"
    ;;
  *)
    echo "用法: bash deploy.sh [up|redeploy|restart|down|status|logs [service]]" >&2
    exit 2
    ;;
esac
