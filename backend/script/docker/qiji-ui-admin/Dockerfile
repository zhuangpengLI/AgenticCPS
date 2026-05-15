FROM node:20-alpine AS builder

WORKDIR /app

ARG VITE_APP_TITLE=AgenticCPS管理系统
ARG VITE_BASE_URL=
ARG VITE_API_URL=/admin-api
ARG VITE_BASE_PATH=/
ARG VITE_OUT_DIR=dist-prod
ARG VITE_APP_TENANT_ENABLE=true
ARG VITE_APP_CAPTCHA_ENABLE=true
ARG VITE_APP_DOCALERT_ENABLE=true
ARG VITE_APP_BAIDU_CODE=

ENV VITE_APP_TITLE=${VITE_APP_TITLE}
ENV VITE_BASE_URL=${VITE_BASE_URL}
ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_BASE_PATH=${VITE_BASE_PATH}
ENV VITE_OUT_DIR=${VITE_OUT_DIR}
ENV VITE_APP_TENANT_ENABLE=${VITE_APP_TENANT_ENABLE}
ENV VITE_APP_CAPTCHA_ENABLE=${VITE_APP_CAPTCHA_ENABLE}
ENV VITE_APP_DOCALERT_ENABLE=${VITE_APP_DOCALERT_ENABLE}
ENV VITE_APP_BAIDU_CODE=${VITE_APP_BAIDU_CODE}

COPY frontend/admin-vue3/package.json frontend/admin-vue3/pnpm-lock.yaml ./
RUN corepack enable && pnpm install --frozen-lockfile

COPY frontend/admin-vue3/ ./
RUN pnpm build:prod \
    && mkdir -p /tmp/admin-dist \
    && cp -r "${VITE_OUT_DIR}/." /tmp/admin-dist/

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /tmp/admin-dist/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
