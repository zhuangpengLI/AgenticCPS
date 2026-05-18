# AgenticCPS 项目地图（草稿）

> 生成日期：2026-05-18  
> 生成方式：只读扫描仓库结构、配置、POM、前端 `package.json`、CPS 核心代码与既有技术债文档后整理。  
> 约束：本次未修改业务代码；仅新增此文档草稿。仓库当前已有多处未提交改动，见“风险与注意事项”。

## 1. 项目入口在哪里

### 后端入口

- 主启动类：`backend/qiji-server/src/main/java/com/qiji/cps/server/QijiServerApplication.java`
  - `main()` 调用 `SpringApplication.run(QijiServerApplication.class, args)`。
  - `@SpringBootApplication(scanBasePackages = {"${qiji.info.base-package}.server", "${qiji.info.base-package}.module"})` 扫描 `com.qiji.cps.server` 与 `com.qiji.cps.module`。
- 应用配置：
  - `backend/qiji-server/src/main/resources/application.yaml`
    - `spring.application.name = qiji-server`
    - 默认激活 `local` profile。
    - `qiji.info.base-package = com.qiji.cps`。
    - Spring AI MCP Server 已开启，并声明 CPS Tools。
  - `backend/qiji-server/src/main/resources/application-local.yaml`
    - 本地端口：`48080`。
    - 本地 MySQL：`jdbc:mysql://127.0.0.1:3306/cps`。
    - 本地 Redis：`127.0.0.1:6379`。
- API 前缀来自 Web 自动配置：
  - `backend/qiji-framework/qiji-spring-boot-starter-web/src/main/java/com/qiji/cps/framework/web/config/WebProperties.java`
  - `controller.app` 默认挂到 `/app-api`。
  - `controller.admin` 默认挂到 `/admin-api`。
  - `controller.openapi` 不在这两个包规则下，CPS OpenAPI 当前自身声明 `/openapi/...` 路径。

### 前端入口

- 管理后台：`frontend/admin-vue3`
  - 入口/构建：Vite + Vue3，`package.json` 脚本包括 `dev`、`ts:check`、`build:prod`、`e2e`。
  - 本地 API：`.env.local` / `.env.dev` 中 `VITE_API_URL=/admin-api`。
- 移动管理端：`frontend/admin-uniapp`
  - UniApp / unibest，Node `>=20`，pnpm `>=9`。
  - `package.json` 脚本包括 `dev`、`dev:h5`、`build`、`build:prod`、`type-check`、`lint`。
- 商城移动端：`frontend/mall-uniapp`
  - Shopro/UniApp 模板，当前 `package.json` 主要只有 `prettier` 脚本，构建更多依赖 HBuilderX / uni-app 运行环境。

### Docker 入口

- `backend/script/docker/docker-compose.yml`
  - 启动 MySQL 8、Redis 6、`qiji-server`、`qiji-admin`。
  - 后端端口映射：`48080:48080`。
  - 管理前端端口映射：`8080:80`。

## 2. 主要模块有哪些

### 后端 Maven 模块

根 POM：`backend/pom.xml`，聚合模块：

| 模块 | 作用 |
|---|---|
| `qiji-server` | Spring Boot 可执行壳，聚合各业务模块并提供 REST API。 |
| `qiji-dependencies` | 依赖版本管理。 |
| `qiji-framework` | Web、Security、MyBatis、Redis、Job、Tenant、MQ、监控、Excel 等 starter。 |
| `qiji-module-system` | 系统管理、权限、租户、字典、登录等基础能力。 |
| `qiji-module-infra` | 基础设施、文件、代码生成、定时任务管理、日志等。 |
| `qiji-module-member` | 会员体系。 |
| `qiji-module-pay` | 支付、钱包、转账相关。 |
| `qiji-module-mall` | 商城模块，含 product / promotion / trade / statistics。 |
| `qiji-module-ai` | AI 模块，Spring AI、模型接入、工具上下文等。 |
| `qiji-module-cps` | CPS 返利核心模块，本项目重点。 |
| `qiji-module-report` / `qiji-module-mp` | 报表、微信公众号等扩展模块。 |

### CPS 模块结构

CPS 聚合 POM：`backend/qiji-module-cps/pom.xml`。

- `qiji-module-cps-api`
  - 暴露枚举、错误码、API DTO 等给其他模块使用。
- `qiji-module-cps-biz`
  - CPS 业务实现，依赖 `member`、`pay`、`system`、tenant、security、mybatis、redis、job、ai 等。

`qiji-module-cps-biz` 主要包：

| 包 | 当前职责 |
|---|---|
| `controller/admin` | 后台管理：平台、推广位、订单、返利配置/记录、冻结、风控、统计、供应商、提现、转账等。 |
| `controller/app` | 用户端：商品搜索/转链、我的返利账户/记录、返利兑换 Token。 |
| `controller/openapi` | 服务间 OpenAPI：返利余额、冻结、解冻、确认扣减。 |
| `client` | CPS 平台与供应商适配器，包含大淘客、好单库、官方 API、淘宝/京东/拼多多/抖音/美团/唯品会适配器。 |
| `service` | 核心业务：goods、order、rebate、freeze、exchange、risk、statistics、withdraw、transfer、vendor、adzone。 |
| `dal/dataobject` + `dal/mysql` | CPS 表 DO 与 MyBatis Mapper。 |
| `job` | 定时任务：订单同步、返利结算、冻结解冻、统计聚合。 |
| `mcp/tool` | 5 个 Agent 可调用工具：搜索、比价、转链、查订单、查返利汇总。 |
| `config` | CPS 缓存、aitoken 兑换配置。 |

## 3. 数据流是什么

### 3.1 商品搜索与转链

```text
用户 / Agent
  -> 前端或 MCP Tool
  -> AppCpsGoodsController 或 cps_search_goods / cps_generate_link
  -> CpsGoodsService
  -> CpsPlatformClientFactory
  -> 平台适配器 / 供应商客户端（dataoke / haodanku / official ...）
  -> 外部 CPS 平台 API
  -> 返回商品、优惠券、佣金、推广链接
```

关键证据：

- `AppCpsGoodsController`：`GET /cps/goods/search` 搜索，`POST /cps/goods/link` 转链；转链时使用 `SecurityFrameworkUtils.getLoginUserId()` 作为会员 ID。
- `CpsSearchGoodsToolFunction`：`@Component("cps_search_goods")`，支持指定平台或全平台聚合搜索。
- `CpsGenerateLinkToolFunction`：`@Component("cps_generate_link")`，通过 ToolContext 或请求参数取得 memberId 后调用转链。
- `CpsPlatformClient`：定义 `searchGoods`、`generatePromotionLink`、`queryOrders`、`testConnection`。
- `CpsPlatformClientFactory`：启动时注册平台客户端与 `vendorCode:platformCode` 供应商客户端，并从平台配置选择 active vendor。

### 3.2 订单同步与返利结算

```text
Quartz Job
  -> CpsOrderSyncJob
  -> CpsPlatformService.getEnabledPlatformList()
  -> CpsPlatformClient.queryOrders()
  -> CpsOrderService.batchSaveOrUpdateOrders()
  -> cps_order / cps_order_sync_log
  -> CpsRebateSettleJob / CpsRebateSettleService
  -> cps_rebate_record / cps_rebate_account
```

关键证据：

- `CpsOrderSyncJob`：按启用平台循环同步订单，按时间窗口拉取，最多翻页 20 页，每页 50 条，并写入同步日志。
- `CpsOrderServiceImpl`：负责批量新增/更新平台订单。
- `CpsRebateSettleServiceImpl`：负责返利记录与账户余额更新。

### 3.3 CPS 返利兑换 aitoken Token（P0 闭环）

```text
用户 App
  -> POST /app-api/cps/rebate/token-exchange/preview
  -> POST /app-api/cps/rebate/token-exchange/submit
  -> CpsRebateTokenExchangeService.submit()
  -> 创建本地兑换单
  -> 冻结 cps_rebate_account.available_balance 到 frozen_balance
  -> CpsAitokenExchangeClient 调 aitoken OpenAPI
  -> 成功：confirmDeduct 扣减冻结返利，兑换单 SUCCESS
  -> 失败：unfreeze 解冻，兑换单 FAILED
  -> 超时/异常：兑换单 PROCESSING，等待补偿
```

服务间 OpenAPI：

```text
GET  /openapi/cps/rebate/balance
POST /openapi/cps/rebate/freeze
POST /openapi/cps/rebate/unfreeze
POST /openapi/cps/rebate/confirm-deduct
```

鉴权/幂等头：`X-App-Id`、`X-Tenant-Id`、`X-Timestamp`、`X-Nonce`、`X-Signature`、`X-Idempotency-Key`。

关键证据：

- `AppCpsRebateController`：用户端预估、提交、查询兑换订单，均使用登录上下文 memberId。
- `OpenApiCpsRebateController`：服务间余额/冻结/解冻/确认扣减，调用 `signatureService.verify()`。
- `CpsRebateTokenExchangeServiceImpl`：本地订单、冻结、调用 aitoken、成功扣减、失败解冻、异常 PROCESSING。
- `CpsAitokenExchangeClient`：调用 `/api/v1/openapi/token/exchange/preview`、`/submit`，并构造统一签名头。

### 3.4 MCP / Agent 调用流

```text
AI Agent
  -> Spring AI MCP Server
  -> Tool Function Bean
  -> CPS Service
  -> DB / 外部 CPS API
  -> 结构化 Tool Response
```

当前配置中 MCP Server 已开启，声明的工具包括：

- `cps_search_goods`
- `cps_compare_prices`
- `cps_generate_link`
- `cps_get_order_status`（代码实际类名为 `CpsQueryOrdersToolFunction`）
- `cps_rebate_summary`（代码实际类名为 `CpsGetRebateSummaryToolFunction`）

注意：配置中的工具显示名与实际 Bean 名需要进一步核对，避免 Agent 工具列表和代码注册不一致。

## 4. 测试怎么跑

### 后端 Maven 测试

在 `backend` 目录执行：

```bash
# 全量测试
mvn test

# 指定测试类
mvn test -Dtest=CpsRebateTokenExchangeServiceImplTest

# 指定 CPS biz 模块测试（推荐做 CPS 改动时优先跑）
mvn test -pl qiji-module-cps/qiji-module-cps-biz -Dtest=CpsRebateTokenExchangeServiceImplTest
```

测试基类规范见：`agent_improvement/memory/testing-specification.md`。

常见选择：

| 场景 | 基类 |
|---|---|
| 纯逻辑 / Controller | `BaseMockitoUnitTest` |
| 需要本模块 Mapper / H2 | `BaseDbUnitTest` |
| 需要 Redis | `BaseRedisUnitTest` |
| 同时需要 DB + Redis | `BaseDbAndRedisUnitTest` |

### 前端测试 / 检查

在 `frontend/admin-vue3` 执行：

```bash
pnpm ts:check
pnpm lint:eslint
pnpm e2e:install
pnpm dev:e2e
pnpm e2e
```

`admin-uniapp` 当前有：

```bash
pnpm type-check
pnpm lint
```

`mall-uniapp` 当前 `package.json` 主要提供：

```bash
npm run prettier
```

## 5. 构建怎么跑

### 后端

在 `backend` 目录执行：

```bash
# 编译
mvn clean compile

# 打包可执行 jar
mvn clean package -DskipTests

# 本地启动后端
mvn spring-boot:run -pl qiji-server -Dspring-boot.run.profiles=local
```

### 管理后台前端

在 `frontend/admin-vue3` 执行：

```bash
pnpm install
pnpm dev
pnpm build:prod
```

### admin-uniapp

在 `frontend/admin-uniapp` 执行：

```bash
pnpm install
pnpm dev:h5
pnpm build:prod
```

### Docker

在 `backend/script/docker` 执行：

```bash
docker-compose up -d
docker-compose logs -f server
docker-compose down
```

## 6. 哪些地方看起来风险高

> 这里按“证据明确程度 + 资金/权限影响”排序。部分风险来自既有 `docs/cps-tech-debt-inventory.md`，本次扫描也看到相同方向的代码证据。

| 风险 | 等级 | 证据 / 影响 |
|---|---:|---|
| 当前工作区已有大量未提交改动 | 高 | `git status --short` 显示 AGENTS、README、CPS exchange/openapi、前端构建配置、E2E 等均有修改/新增。后续改动前需要区分已有改动和本次改动，避免覆盖他人工作。 |
| MCP 转链 memberId 来源存在越权归因风险 | 高 | `CpsGenerateLinkToolFunction` 允许 request.memberId 优先，ToolContext 只在 request 为空时兜底；订单归因/返利归属应优先可信上下文。 |
| 资金链路复杂且跨系统 | 高 | 返利兑换 Token 涉及本地订单、冻结/扣减/解冻、aitoken OpenAPI、幂等、异常 PROCESSING；任何状态机/补偿缺口都可能影响资产一致性。 |
| OpenAPI 签名缺少重放窗口/nonce 存储证据 | 高 | `CpsOpenApiSignatureService` 校验 HMAC、timestamp、nonce、signature，但当前扫描未看到 timestamp 窗口与 nonce 防重放落库/缓存。 |
| 本地配置含开发密钥/默认密码/第三方测试 secret | 高 | `application-local.yaml` 含 MySQL、Redis、RabbitMQ、Wx、OAuth 等本地/测试凭据；不得直接用于生产。 |
| 官方 vendor 多处可能未完全实现 | 中高 | 既有技术债文档记录 official client 多处 `return null` / 待实现；active vendor 切换时可能表现为静默空结果。 |
| 订单同步状态机和幂等边界 | 中高 | 订单同步按平台回写订单；既有技术债指出平台状态可能直接覆盖本地状态，需要防乱序/回退/重复入账。 |
| 统计 SQL 可能不走索引 | 中 | `CpsOrderMapper.xml` 对 `create_time` 使用 `DATE(create_time)`，大表统计可能导致索引失效。 |
| MCP 审计闭环不足 | 中 | 存在 `CpsMcpAccessLogDO` / Mapper，但 5 个 Tool 中未看到统一访问日志写入。 |
| 金额类型规则漂移 | 中 | 项目规范要求金额用 Integer 分，但 CPS 多处资金字段使用 `BigDecimal`；不建议立刻大改，但新增资金字段应先统一单位策略。 |
| Windows/PowerShell 编码风险 | 中 | AGENTS 明确禁止用 PowerShell 读写中文文件；后续所有文件写入应使用 Python UTF-8 并验证解码。 |

## 7. 快速定位清单

### CPS 关键代码

- 商品搜索/转链 Controller：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/app/goods/AppCpsGoodsController.java`
- 返利账户/兑换 Controller：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/app/rebate/AppCpsRebateController.java`
- 返利 OpenAPI Controller：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/openapi/rebate/OpenApiCpsRebateController.java`
- 平台策略接口：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/CpsPlatformClient.java`
- 平台/供应商工厂：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/CpsPlatformClientFactory.java`
- 商品服务：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsServiceImpl.java`
- 订单服务：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/order/CpsOrderServiceImpl.java`
- 返利结算：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateSettleServiceImpl.java`
- 返利兑换 Token：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/exchange/CpsRebateTokenExchangeServiceImpl.java`
- aitoken 客户端：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/exchange/CpsAitokenExchangeClient.java`
- OpenAPI 签名：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/exchange/CpsOpenApiSignatureService.java`
- 订单同步 Job：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/job/CpsOrderSyncJob.java`
- MCP Tools：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/`

### 关键文档

- 生态 P0 闭环：`docs/agentic-ecosystem-p0-rebate-token-exchange.md`
- CPS 技术债：`docs/cps-tech-debt-inventory.md`
- 测试规范：`agent_improvement/memory/testing-specification.md`
- 代码生成规则：`agent_improvement/memory/codegen-rules.md`
- CPS 需求/PRD：`docs/CPS系统需求文档.md`、`docs/CPS系统PRD文档.md`

## 本次未做的事

- 未运行全量测试或构建；本文档的“测试/构建怎么跑”来自仓库脚本与配置扫描。
- 未验证数据库脚本是否与当前 DO/Mapper 完全一致。
- 未验证 MCP Server 实际启动后的工具列表是否与配置声明完全一致。
- 未修改任何业务代码。
