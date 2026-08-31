#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${BACKEND_ROOT}/.." && pwd)"
FRONTEND_ROOT="${PROJECT_ROOT}/frontend/admin-vue3"
MALL_ROOT="${PROJECT_ROOT}/frontend/mall-uniapp"

H5_CLI="${SHOPRO_H5_CLI:-${HBUILDERX_CLI_PATH:-}}"

if [[ "${1:-}" == "--skip-build" ]]; then
  echo "[1/5] 跳过构建，使用已有制品"
else
  echo "[1/5] 构建后端 JAR"
  mvn -f "${BACKEND_ROOT}/pom.xml" clean package -pl qiji-server -am -DskipTests

  echo "[2/5] 构建管理前端"
  (
    cd "${FRONTEND_ROOT}"
    pnpm install --frozen-lockfile
    pnpm build:docker
  )

  if [[ -n "${SHOPRO_H5_DIST:-}" ]]; then
    echo "[3/5] 使用 SHOPRO_H5_DIST 指定的 mall-uniapp H5 制品"
  else
    echo "[3/5] 构建 mall-uniapp H5"
    if [[ -z "${H5_CLI}" ]]; then
      H5_CLI="$(command -v cli.exe || command -v cli.cmd || command -v cli || true)"
    fi
    if [[ -z "${H5_CLI}" ]]; then
      echo "未找到 HBuilderX CLI。请设置 SHOPRO_H5_CLI/HBUILDERX_CLI_PATH 或将 cli 加入 PATH。" >&2
      exit 1
    fi
    "${H5_CLI}" publish web --project "${MALL_ROOT}" --webHosting false --ssr false
  fi
fi

BACKEND_JAR="${BACKEND_ROOT}/qiji-server/target/qiji-server.jar"
FRONTEND_DIST="${FRONTEND_ROOT}/dist-prod"
if [[ -n "${SHOPRO_H5_DIST:-}" ]]; then
  MALL_H5_DIST="${SHOPRO_H5_DIST}"
elif [[ -f "${MALL_ROOT}/unpackage/dist/build/web/index.html" ]]; then
  MALL_H5_DIST="${MALL_ROOT}/unpackage/dist/build/web"
else
  MALL_H5_DIST="${MALL_ROOT}/unpackage/dist/build/h5"
fi

if [[ ! -f "${BACKEND_JAR}" ]]; then
  echo "未找到后端构建产物: ${BACKEND_JAR}" >&2
  exit 1
fi
if [[ ! -f "${FRONTEND_DIST}/index.html" ]]; then
  echo "未找到前端构建产物: ${FRONTEND_DIST}/index.html" >&2
  exit 1
fi
if [[ ! -f "${MALL_H5_DIST}/index.html" ]]; then
  echo "未找到 mall-uniapp H5 构建产物: ${MALL_H5_DIST}/index.html。请使用 HBuilderX 发行 H5，或设置 SHOPRO_H5_DIST 指向 H5 dist 目录。" >&2
  exit 1
fi

echo "[4/5] 归集后端、前端和数据库初始化文件"
cp -f "${BACKEND_JAR}" "${SCRIPT_DIR}/backend/qiji-server.jar"
rm -rf "${SCRIPT_DIR}/frontend/dist"
rm -rf "${SCRIPT_DIR}/frontend/mall-h5"
mkdir -p "${SCRIPT_DIR}/frontend/dist" "${SCRIPT_DIR}/frontend/mall-h5" "${SCRIPT_DIR}/mysql/init"
cp -R "${FRONTEND_DIST}/." "${SCRIPT_DIR}/frontend/dist/"
cp -R "${MALL_H5_DIST}/." "${SCRIPT_DIR}/frontend/mall-h5/"
cp -f "${BACKEND_ROOT}/sql/mysql/ruoyi-vue-pro.sql" "${SCRIPT_DIR}/mysql/init/00-ruoyi-vue-pro.sql"
cp -f "${BACKEND_ROOT}/sql/mysql/quartz.sql" "${SCRIPT_DIR}/mysql/init/05-quartz.sql"
cp -f "${BACKEND_ROOT}/sql/module/cps-all-in-one.sql" "${SCRIPT_DIR}/mysql/init/10-cps-all-in-one.sql"
cp -f "${BACKEND_ROOT}/sql/module/ai-all.sql" "${SCRIPT_DIR}/mysql/init/15-ai-all.sql"

echo "[5/5] 校验部署目录"
if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  (
    cd "${SCRIPT_DIR}"
    docker compose --env-file docker.env config --quiet
  )
else
  echo "未检测到 Docker Compose，跳过 Compose 配置校验。"
fi

echo "部署包准备完成: ${SCRIPT_DIR}"
echo "移动端 H5 输出目录: ${SCRIPT_DIR}/frontend/mall-h5"
echo "复制整个 docker 目录到服务器后，执行: bash deploy.sh"
