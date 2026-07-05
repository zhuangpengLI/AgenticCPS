# AgenticCPS 项目地图（草稿）

> 生成日期：2026-05-24
> 生成方式：只读扫描仓库结构、配置、POM、前端 `package.json`、CPS 核心代码与既有技术债文档后整理；2026-05-24 补充活动中心、返利工具箱与选品库落地信息；2026-05-26 补充 CPS 主导型 CPX 任务/资讯/平台资料库骨架、后台页面、看板骨架与 OpenAPI 签名校验。
> 约束：仓库当前已有多处未提交改动，见“风险与注意事项”。后续编辑需继续区分既有改动和本次改动。

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
| `controller/admin` | 后台管理：活动中心、返利工具箱、商品广场、平台、推广位、订单、返利配置/记录、冻结、风控、统计、供应商、提现、转账、选品库，以及新增 CPX 任务/资讯/平台对接中心和 CPX 看板汇总。 |
| `controller/app` | 用户端：商品搜索/转链、我的返利账户/记录、返利兑换 Token。 |
| `controller/openapi` | 服务间 OpenAPI：返利余额、冻结、解冻、确认扣减；CPX 曝光、点击、线索、动作/转化事件上报，统一 HMAC 签名与幂等键。 |
| `client` | CPS 平台与供应商适配器，包含大淘客、好单库、官方 API、淘宝/京东/拼多多/抖音/美团/唯品会适配器。 |
| `service` | 核心业务：goods、toolbox、activity、order、rebate、freeze、exchange、risk、statistics、withdraw、transfer、vendor、adzone、selection、cpx。 |
| `dal/dataobject` + `dal/mysql` | CPS 表 DO 与 MyBatis Mapper，包括 `cps_rebate_activity` 活动卡片配置、`cps_selection_theme` 选品主题、`cps_selection_theme_item` 主题商品快照；CPX 新增 `cpx_task`、`cpx_offer`、`cpx_material`、`cpx_article`、`cpx_platform_profile`、`cpx_tracking_link`、`cpx_event`、`cpx_conversion`、`cpx_settlement_record`、`cpx_lead_detail`。 |
| `job` | 定时任务：订单同步、返利结算、冻结解冻、统计聚合。 |
| `mcp/tool` | Agent 可调用工具：搜索、比价、转链、查订单、查返利汇总、AIoT 场景推荐、选品库主题查询与主题商品推荐。 |
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

### 3.1.1 管理后台活动中心

```text
运营人员
  -> frontend/admin-vue3/src/views/cps/activity/square/index.vue
  -> GET /admin-api/cps/rebate-activity/center
  -> CpsRebateActivityController
  -> CpsRebateActivityService.getActivityCenter()
  -> cps_rebate_activity + 启用平台配置
  -> 返回 tabs / billingTypeOptions / cards / pagination

运营人员点击同步
  -> POST /admin-api/cps/rebate-activity/sync
  -> CpsRebateActivitySyncServiceImpl.syncThirdPartyActivities()
  -> HaodankuActivityVendorClient / DtkActivityVendorClient
  -> cps_rebate_activity upsert 后刷新活动中心
```

关键证据：

- `CpsRebateActivityController`：`GET /cps/rebate-activity/center` 聚合活动中心卡片；CRUD 权限沿用 `cps:rebate-activity:query/create/update/delete`。
- `CpsRebateActivityController`：`POST /cps/rebate-activity/sync` 暴露管理端手动同步入口，复用 `cps:rebate-activity:update` 权限，返回新增、更新、跳过数量。
- `CpsRebateActivitySyncServiceImpl`：统一同步第三方活动并落库；当前支持好单库与大淘客活动源，大淘客通过 `DtkActivityVendorClient` 拉取淘宝活动会场。
- `CpsRebateActivityServiceImpl`：只返回启用且在有效时间窗口内的活动，支持平台、`CPS` / `CPA` / `CPS+CPA`、关键词、热门/最新排序和分页。
- `cps_rebate_activity`：活动运营配置表，新增 `billing_type`、`promotion_count`、`source_type`、`external_activity_id`、`tag_text`，并补充活动中心查询索引。
- 前端活动中心页提供同步来源选择、同步页数与同步按钮；同步完成后重新请求活动中心，页面展示仍以落库后的活动卡片为准。
- 前端活动中心卡片 `search` 跳转到商品广场并带入 `platformCode`、`keyword`、`activityTag`；`url` 新窗口打开；`none` 仅展示。
- 平台 tabs 优先来自活动数据与启用平台配置，兜底包含热门、美团、饿了么、抖音、本地生活、飞猪、拼多多、淘宝、京东。

### 3.1.2 管理后台返利工具箱

管理后台返利工具箱是面向运营的统一工作台，用于把万能转链、口令解析、归属检测、优惠券查询、返利商品广场、淘礼金计划、推广文案编辑和批量复制收敛到一个入口。

```text
运营人员
  -> frontend/admin-vue3/src/views/cps/toolbox/index.vue
  -> POST /admin-api/cps/goods/parse 或 /batch-transfer
  -> CpsGoodsRebateQueryController
  -> CpsGoodsToolboxService
  -> CpsContentParser / CpsGoodsRebateQueryService / 平台解析能力
  -> 返回解析结果或逐条转链结果
```

关键证据：

- `CpsGoodsRebateQueryController`：新增 `POST /cps/goods/parse` 和 `POST /cps/goods/batch-transfer`，权限分别为 `cps:toolbox:query`、`cps:toolbox:link`。
- `CpsGoodsToolboxServiceImpl`：解析仅识别商品信息，不生成推广链接或转链记录；批量转链逐条调用既有 `CpsGoodsRebateQueryService.queryRebate()`。
- 批量转链忽略空行，最多支持 20 条非空内容，保留原始输入序号，单条失败不阻断整批。
- P1/P2 接口包含 `POST /cps/goods/ownership-check`、`POST /cps/goods/coupon-query`、`POST /cps/goods/cash-gift/plan`；淘礼金当前只生成运营计划和预算检查，不调用真实发放接口。
- 前端工具箱包含万能转链、口令解析、归属检测、优惠券查询、商品广场、淘礼金计划与推广文案编辑区，商品广场入口可通过 `/cps/toolbox?tool=goods-square` 直达。
- 菜单与权限 SQL 放在 `backend/sql/mysql/cps-all-in-one.sql`；不要把 CPS 菜单、权限或种子数据写回 `ruoyi-vue-pro.sql`。

### 3.1.3 选品库主题与商品快照

```text
运营后台
  -> CPS联盟 / 选品库
  -> CpsSelectionThemeController
  -> CpsSelectionThemeService
  -> 复用 CpsGoodsSquareService 第三方拉取
  -> 规则评分 + 文案推荐
  -> cps_selection_theme / cps_selection_theme_item
  -> MCP 工具只读查询已发布主题与启用商品
```

第一版只提供管理后台与 MCP 查询，不做用户端/App 展示页；商品价格、券、佣金、销量只作为第三方快照与运营选品依据，不参与返利账户、订单归因、Token 兑换等资金链路。

关键证据：

- `CpsSelectionThemeController`：提供主题 CRUD、发布/下线、AI 推荐、第三方拉取、商品导入、排序、状态切换接口；`POST /admin-api/cps/selection-theme/vendor-theme-sync` 可按供应商同步选品主题（`dataoke` 大淘客选品库、`haodanku` 好单库特色栏目），同步生成主题默认发布，旧 `POST /admin-api/cps/selection-theme/dataoke-theme-sync` 保持兼容，并可同步主题下商品快照。
- `CpsSelectionThemeServiceImpl`：保存主题规则 JSON 与商品快照，发布前校验启用商品，导入时按 `themeId + platformCode + vendorCode + goodsId + goodsSign` 去重更新。
- `CpsSelectionAiRecommendService`：规则评分决定排序，LLM/文案能力不可用时仍可返回稳定推荐；文案不得覆盖商品 ID、价格、佣金等第三方事实字段。
- `cps_selection_theme` / `cps_selection_theme_item`：选品主题主表与主题商品快照表，均带租户、软删、状态与排序索引。

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
- `cps_query_orders`
- `cps_get_rebate_summary`
- `cps_recommend_by_scene`
- `cps_list_selection_themes`
- `cps_recommend_from_selection_theme`

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
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsRebateTokenExchangeServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"

# 活动中心聚合查询测试
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsRebateActivityServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"

# 选品库主题、AI 推荐、MCP 工具测试
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=CpsSelectionThemeServiceImplTest,CpsSelectionAiRecommendServiceTest,CpsSelectionThemeMcpToolFunctionTest" "-Dsurefire.failIfNoSpecifiedTests=false"

# 返利工具箱、返利查询、商品广场测试
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=CpsGoodsToolboxServiceImplTest,CpsGoodsRebateQueryServiceImplTest,CpsGoodsSquareServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"
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
pnpm e2e:midscene
```

`pnpm e2e:midscene` 需要本机提供 `MIDSCENE_MODEL_BASE_URL`、`MIDSCENE_MODEL_API_KEY`、`MIDSCENE_MODEL_NAME`、`MIDSCENE_MODEL_FAMILY`。Midscene.js 只作为视觉/语义辅助，验收仍以 Playwright `expect`、类型检查和后端测试为准。

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
| MCP 审计闭环不足 | 中 | 存在 `CpsMcpAccessLogDO` / Mapper；新选品库 Tool 已接入审计，但历史基础 Tool 仍需逐步补齐统一访问日志写入与脱敏结构。 |
| 金额类型规则漂移 | 中 | 项目规范要求金额用 Integer 分，但 CPS 多处资金字段使用 `BigDecimal`；不建议立刻大改，但新增资金字段应先统一单位策略。 |
| Windows/PowerShell 编码风险 | 中 | AGENTS 明确禁止用 PowerShell 读写中文文件；后续所有文件写入应使用 Python UTF-8 并验证解码。 |

## 7. 快速定位清单

### Agentic 工程化入口

- 工作协议：`AGENTS.md`
- 总览与快速开始：`README.md`
- Codex/Superpowers/OMX/TDD/E2E SOP：`docs/codex-agentic-development-workflow.md`
- E2E issue 复现流程：`docs/e2e-agent-issue-workflow.md`
- Playwright 配置：`frontend/admin-vue3/playwright.config.ts`
- Midscene fixture：`frontend/admin-vue3/e2e/fixture.ts`
- Midscene smoke：`frontend/admin-vue3/e2e/midscene-smoke.spec.ts`

### CPS 关键代码

- 活动中心 Controller：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/activity/CpsRebateActivityController.java`
- 活动中心服务：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/activity/CpsRebateActivityServiceImpl.java`
- 活动中心前端：`frontend/admin-vue3/src/views/cps/activity/square/index.vue`
- 返利工具箱 Controller：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/CpsGoodsRebateQueryController.java`
- 返利工具箱服务：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsToolboxServiceImpl.java`
- 返利工具箱前端：`frontend/admin-vue3/src/views/cps/toolbox/index.vue`
- 选品库 Controller：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/selection/CpsSelectionThemeController.java`
- 选品库服务：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/selection/CpsSelectionThemeServiceImpl.java`
- 选品库 AI 推荐：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/selection/CpsSelectionAiRecommendService.java`
- 选品库前端：`frontend/admin-vue3/src/views/cps/selection/theme/index.vue`
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
- CPS 主 R 打样 SOP：`docs/cps-refactor-sop.md`
- CPS 订单同步打样报告：`docs/cps-order-sync-pilot-pre-pr.md`
- CPS Pre-PR 模板：`docs/cps-pre-pr-template.md`
- 测试规范：`agent_improvement/memory/testing-specification.md`
- 代码生成规则：`agent_improvement/memory/codegen-rules.md`
- CPS always 级 AI 规则：`agent_improvement/memory/cps-ai-coding-rules.md`
- CPS 需求/PRD：`docs/CPS系统需求文档.md`、`docs/CPS系统PRD文档.md`
- CPS MySQL 模块脚本：`backend/sql/mysql/cps-all-in-one.sql`，集中维护 CPS 表、种子数据、菜单与权限；`ruoyi-vue-pro.sql` 保持系统基础 SQL。

## 本次未做的事

- 未运行全量测试或前端生产构建；活动中心、返利工具箱与选品库均已通过目标后端测试，前端全量类型检查仍受仓库既有无关类型错误影响。
- 未全面验证数据库脚本是否与所有当前 DO/Mapper 完全一致；活动中心、返利工具箱菜单权限与选品库表字段已按本轮实现静态同步，CPS SQL 集中在 MySQL `cps-all-in-one.sql`。
- 未验证 MCP Server 实际启动后的工具列表是否与配置声明完全一致。
- 本次刷新仅更新文档说明，不改动业务代码。
