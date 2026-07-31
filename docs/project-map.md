# AgenticCPS 项目地图（草稿）

> 资金与归因基线：订单返利、冻结/解冻、退款欠款与 Token 兑换统一通过 `CpsRebateAssetService` 写入账户并追加 `cps_rebate_asset_ledger`；`CpsRebateAssetMigrationService` 只允许在 `migration_ready=false` 且 V2 未启用时回填历史账户期初流水。迁移预检按租户归档重复键、NULL 对账和孤儿资金记录等十类风险，`migration_ready` 必须绑定最新 ready 批次，首次启用还会在同一事务内重检后才能不可逆开启；可信归因审计与固定窗口同步 checkpoint 说明见 [cps-funds-attribution-safety-baseline.md](cps-funds-attribution-safety-baseline.md)。

> 生成日期：2026-07-31
> 生成方式：只读扫描仓库结构、配置、POM、前端 `package.json`、CPS 核心代码与既有技术债文档后整理；2026-05-24 补充活动中心、返利工具箱与选品库落地信息；2026-05-26 补充 CPS 主导型 CPX 任务/资讯/平台资料库骨架、后台页面、看板骨架与 OpenAPI 签名校验；2026-07-10 补充滴滴联盟 DUnion SDK 子模块、官方适配器和后台诊断接口；2026-07-14 补充 P3 SDK 标准化描述符、能力矩阵、配置 Schema、连接治理策略、结构化不支持能力异常和 official skeleton 启用门禁；2026-07-15 补充 P4 增长分析、风险监控、实验分流、Token 事件对账和 billing 边界校验入口；2026-07-26 补充 CPS 平台配置中心统一入口、草稿检测发布生命周期和验证命令；2026-07-31 补充好单库淘宝会场与闪购转链、SID 订单归因、会员活动转链和订单号人工申领审核闭环及真实本地烟测边界。
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

### CPS 平台配置中心统一入口

- 管理后台菜单路径：`联盟配置 → 平台配置中心`。
- 前端路由：`/cps-config/platform-onboarding`，页面入口：`frontend/admin-vue3/src/views/cps/platformOnboarding/index.vue`；五步工作台在同目录的 `workspace.vue`。
- 后端接口根路径：`/admin-api/cps/platform-onboarding`，Controller 位于 `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/onboarding/`。
- 工作流：保存当前租户的加密草稿 → 对草稿版本和稳定配置指纹做结构/能力/连接检测 → 检测失败仍可保存为待完善或禁用 → 检测通过后发布；已有平台只有发布事务提交后才切换运行态。
- 运行时真相仍是四张表：`cps_platform`、`cps_api_vendor`、`cps_adzone`、`cps_rebate_config`。`cps_platform_onboarding_draft` 仅保存编辑态快照，敏感凭证通过 `EncryptTypeHandler` 等统一加密能力落库。
- 发布按供应商、推广位、返利规则、平台顺序在同一事务内更新，平台写最后；提交后才失效平台、供应商和返利缓存。草稿版本冲突、指纹不一致或任一写入失败都不能产生部分运行态。
- 原 API 供应商、平台、推广位、返利菜单已经隐藏，旧 Controller/API 和权限保留用于兼容与回滚。备用供应商目前仅配置优先级和可用性检测，运行时自动故障切换不在本期实现。
- 目标验证：`mvn test -pl qiji-module-cps/qiji-module-cps-biz -am` 运行平台配置中心后端测试，`pnpm exec eslint ...platformOnboarding...` 和 `pnpm exec playwright test e2e/cps-platform-onboarding.spec.ts` 运行前端定向检查，`python -m pytest script/test/test_admin_cps_platform_onboarding_ui_contract.py -q` 运行 UI 契约测试。

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
- `qiji-module-cps-sdk-dunion`
  - 仓内加固的滴滴联盟 DUnion Java SDK，保留 `cn.didi.union` 包和签名算法，提供可配置地址/超时、HTTP 与业务错误处理、参数编码和脱敏日志。
- `qiji-module-cps-biz`
  - CPS 业务实现，依赖 `qiji-module-cps-sdk-dunion`、`member`、`pay`、`system`、tenant、security、mybatis、redis、job、ai 等。

`qiji-module-cps-biz` 主要包：

| 包 | 当前职责 |
|---|---|
| `controller/admin` | 后台管理：活动中心、返利工具箱、商品广场、平台、推广位、订单、返利配置/记录、冻结、风控、统计、供应商、提现、转账、选品库、滴滴联盟素材/连接测试/归因诊断，以及平台配置中心、CPX 任务/资讯/平台对接中心和 CPX 看板汇总。 |
| `controller/app` | 用户端：商品搜索/转链、活动会员转链、我的订单列表/详情与订单号申领、我的返利账户/记录、返利兑换 Token。 |
| `controller/openapi` | 服务间 OpenAPI：返利余额、冻结、解冻、确认扣减；CPX 曝光、点击、线索、动作/转化事件上报，统一 HMAC 签名与幂等键。 |
| `client` | CPS 平台与供应商适配器，包含大淘客、好单库、官方 API、淘宝/京东/拼多多/抖音/美团/饿了么/唯品会适配器，以及 `didi + official` 的 `DidiOfficialVendorClient` 和 `DidiPlatformClientAdapter`。 |
| `service` | 核心业务：goods、toolbox、activity、order、rebate、freeze、exchange、risk、statistics、withdraw、transfer、vendor、adzone、selection、cpx。 |
| `dal/dataobject` + `dal/mysql` | CPS 表 DO 与 MyBatis Mapper，包括 `cps_rebate_activity` 活动卡片配置、`cps_selection_theme` 选品主题、`cps_selection_theme_item` 主题商品快照和加密的 `cps_platform_onboarding_draft` 草稿；CPX 新增 `cpx_task`、`cpx_offer`、`cpx_material`、`cpx_article`、`cpx_platform_profile`、`cpx_tracking_link`、`cpx_event`、`cpx_conversion`、`cpx_settlement_record`、`cpx_lead_detail`。 |
| `job` | 定时任务：订单同步、返利结算、冻结解冻、统计聚合。 |
| `mcp/tool` | Agent 可调用工具：搜索、比价、转链、查订单、查返利汇总、推广策略建议、返利规则咨询、AIoT 场景推荐、购买决策、选品库主题查询与主题商品推荐、Token 兑换查询/创建，以及 CPX 任务/转化/内容工具。 |
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

- `AppCpsGoodsController`：`GET /cps/goods/search` 搜索，`GET /cps/goods/compare` 跨平台比价，`GET /cps/goods/detail` 商品详情，`POST /cps/goods/parse` 链接/口令解析，`POST /cps/goods/link` 转链；转链时使用 `SecurityFrameworkUtils.getLoginUserId()` 作为会员 ID。
- `CpsSearchGoodsToolFunction`：`@Component("cps_search_goods")`，支持指定平台或全平台聚合搜索。
- `CpsGenerateLinkToolFunction`：`@Component("cps_generate_link")`，通过 ToolContext 或请求参数取得 memberId 后调用转链。
- `CpsPlatformClient`：定义 `searchGoods`、`generatePromotionLink`、`queryOrders`、`testConnection`，并声明平台级能力；未支持的平台口令解析默认返回 `CAPABILITY_UNSUPPORTED`。
- `CpsApiVendorClient.describe()`：输出 `CpsVendorDescriptor`，统一包含 `vendorCode/platformCode/vendorType`、能力集合、`CpsVendorConfigSchema`、`CpsVendorGovernancePolicy`、SDK 模块和版本。
- `CpsPlatformClientFactory`：启动时注册平台客户端与 `vendorCode:platformCode` 供应商客户端，并从平台配置选择 active vendor；`getRegisteredVendorDescriptors()` 输出注册供应商能力矩阵，供后台诊断和 contract test 使用。
- `CpsApiVendorServiceImpl`：后台供应商 create/update 在写库前检查 official descriptor；只声明 `CONNECTION_TEST`、缺少业务能力的 official 配置会以 `VENDOR_CAPABILITY_NOT_READY` 拒绝启用。
- `CpsPlatformController`：除平台配置 CRUD 外提供 `GET /admin-api/cps/platform/test-connection`，按 `platformCode` 调用已注册 `CpsPlatformClient.testConnection()`，返回 `supported/success/failureReason`；管理端平台列表 `frontend/admin-vue3/src/views/cps/platform/index.vue` 通过 `frontend/admin-vue3/src/api/cps/platform.ts` 暴露“连接测试”操作。
- `CpsAdzoneController`：提供推广位 CRUD、按平台列表和 `POST /admin-api/cps/adzone/batch-create`；批量创建复用 `CpsAdzoneService.createAdzone()` 的 PID、渠道和会员专属校验，逐条返回成功/失败原因。管理端 `frontend/admin-vue3/src/views/cps/adzone/index.vue` 支持粘贴批量创建本地推广位配置；真实联盟平台侧 PID 创建与核对仍属于生产验收。
- 滴滴联盟适配器不支持商品搜索：全平台搜索自动跳过 `didi`，直接指定滴滴搜索返回结构化 `CAPABILITY_UNSUPPORTED`，包含 `platform/vendor/capability`；标准转链把 `goodsId` 作为 `activityId`，默认生成 H5 链接。
- 大淘客淘宝高效转链归因规则见 `docs/dataoke-high-efficiency-link-attribution.md`：`externalId` 只用于绑定 `specialId` 的外部标记，渠道订单依赖渠道专属 PID + `relationId` + `orderScene=2`，会员订单依赖会员专属 PID + `specialId` + `orderScene=3`。
- 大淘客搜索页与商品广场实现规则见 `docs/dataoke-search-page-implementation.md`：搜索页由热搜记录、搜索联想词、大淘客搜索、超级搜索和联盟搜索组合；当前默认优先 dataoke 大淘客搜索，超级搜索/联盟搜索适合作为扩展召回或补量策略。

### 3.1.1 大淘客搜索页与商品广场

```text
运营人员
  -> frontend/admin-vue3/src/views/cps/goods/square/index.vue
  -> GET /admin-api/cps/goods-square/hot-keywords 或 /suggestions
  -> CpsGoodsSquareController
  -> CpsGoodsSquareServiceImpl
  -> DtkTaobaoVendorClient
  -> 大淘客热搜记录 / 搜索联想词

运营人员提交搜索
  -> GET /admin-api/cps/goods-square/search
  -> CpsGoodsSquareServiceImpl.searchGoods()
  -> CpsGoodsService
  -> CpsPlatformClientFactory
  -> DtkTaobaoVendorClient / 其它平台供应商
  -> 统一 CpsGoodsItem / 商品广场 VO
```

关键证据：

- `CpsGoodsSquareController`：提供 `/meta`、`/hot-keywords`、`/suggestions`、`/vendor-goods`、`/search`、`/search-by-image`、`/link`。
- `AppCpsGoodsController`：提供 `/app-api/cps/goods/search`、`/app-api/cps/goods/compare`、`/app-api/cps/goods/detail`、`/app-api/cps/goods/parse` 和 `/app-api/cps/goods/link`；会员端商品页 `frontend/mall-uniapp/pages/commission/goods.vue` 与详情页 `frontend/mall-uniapp/pages/commission/goods-detail.vue` 通过 `sheep/api/cps/goods.js` 接通搜索、跨平台比价、详情、链接/口令解析和转链，会员归因只使用登录上下文。
- `CpsGoodsSquareServiceImpl`：组织商品广场元数据、热搜、联想词、关键词搜索、图片搜索和转链记录。
- `CpsGoodsMasterController` / `CpsGoodsMasterServiceImpl`：阶段 1 营销商品主档入口，提供 `/admin-api/cps/goods-master/page`、`/get`、`/import-selection-item`、`/source/page`、`/price-snapshot/page`；从 `cps_selection_theme_item` 导入时生成统一商品主档、来源映射和价格快照。
- `DtkTaobaoVendorClient`：淘宝 dataoke 默认关键词搜索走 `/goods/get-dtk-search-goods`，热搜走 `/category/get-top100`，联想词走 `/goods/search-suggestion`，图片搜索走 `openapiv2.dataoke.com/open-api/goods/search-by-image`。
- 搜索结果只作为导购展示、运营选品和转链前置展示；返利入账、冻结/扣减、订单归因和 Token 兑换仍以后续订单与归因链路为准。
- `cps_goods_master`、`cps_goods_source_mapping`、`cps_goods_price_snapshot` 只作为营销检索、运营、推荐和转链前展示数据；价格、券、佣金字段是第三方快照，不是订单佣金、返利比例、冻结或资产入账事实来源。
- 后续若接入官方“超级搜索”或“联盟搜索”补量，应作为可配置搜索模式或召回策略扩展，保留来源标识和降级逻辑，不覆盖默认大淘客搜索字段。

### 3.1.2 管理后台活动中心

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
- `AppCpsRebateActivityController`：提供 `POST /app-api/cps/rebate-activity/promotion`，会员身份只取登录上下文；好单库淘宝会场使用官方支持的 `relation_id + 渠道专属 PID`，并要求供应商认证令牌或扩展配置提供授权淘宝账号名 `tb_name`；好单库闪购/饿了么优先使用平台专属配置，缺失时复用同一好单库淘宝账号，生成 1-15 位随机 SID 并写入有效转链记录。
- `CpsRebateActivityServiceImpl`：活动推广响应显式区分外部推广成功、站内落地回退和失败；不会把 `localhost` 管理端地址伪装为可分发的活动链接，也不会向好单库淘宝会场发送其接口不支持的 `special_id`。
- `HdkElemeVendorClient` / `ElemePlatformClientAdapter`：复用好单库 `elm_activity_ratesurl` 和闪购订单接口，解析 H5、淘口令、小程序、Scheme、`trade_id`、`channel_code`、退款与结算字段；随机 SID 不编码会员或租户身份。
- `cps_rebate_activity`：活动运营配置表，新增 `billing_type`、`promotion_count`、`source_type`、`external_activity_id`、`tag_text`，并补充活动中心查询索引。
- 前端活动中心页提供同步来源选择、同步页数与同步按钮；同步完成后重新请求活动中心，页面展示仍以落库后的活动卡片为准。
- 前端活动中心卡片 `search` 跳转到商品广场并带入 `platformCode`、`keyword`、`activityTag`；`url` 新窗口打开；`none` 仅展示。
- 平台 tabs 优先来自活动数据与启用平台配置，兜底包含热门、美团、饿了么、抖音、本地生活、飞猪、拼多多、淘宝、京东。

### 3.1.3 管理后台返利工具箱

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
- Standalone `goods/rebate-query` admin menu is removed from CPS menu seed and existing DB migration; rebate query and transfer operations should enter through the existing rebate toolbox menu.
- 批量转链忽略空行，最多支持 20 条非空内容，保留原始输入序号，单条失败不阻断整批。
- P1/P2 接口包含 `POST /cps/goods/ownership-check`、`POST /cps/goods/coupon-query`、`POST /cps/goods/cash-gift/plan`；淘礼金当前只生成运营计划和预算检查，不调用真实发放接口。
- 前端工具箱包含万能转链、口令解析、归属检测、优惠券查询、商品广场、淘礼金计划与推广文案编辑区，商品广场入口可通过 `/cps/toolbox?tool=goods-square` 直达。
- 菜单与权限全量 SQL 放在 `backend/sql/module/cps-all-in-one.sql`，现有库增量更新放在 `backend/sql/module/cps-update.sql`，每段更新必须记录修改时间；不要在 `backend/sql/mysql` 新建 CPS all-in-one 或临时 CPS SQL，避免脚本不同步；不要把 CPS 菜单、权限或种子数据写回 `ruoyi-vue-pro.sql`。

### 3.1.4 选品库主题与商品快照

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
- `CpsGoodsMasterServiceImpl.importSelectionItem()` 可将选品主题商品快照导入阶段 1 商品主档，按 `platformCode + vendorCode + goodsId + goodsSign` 复用来源映射并追加价格快照。

### 3.1.5 阶段 1 商品主档与券池

```text
运营后台 / 已发布选品快照
  -> CpsGoodsMasterController / CpsCouponPoolController / CpsMarketingShortLinkController / CpsMarketingClickEventController / CpsMarketingFunnelController
  -> CpsGoodsMasterServiceImpl / CpsCouponPoolServiceImpl / CpsMarketingShortLinkServiceImpl / CpsMarketingClickEventServiceImpl / CpsMarketingFunnelServiceImpl
  -> cps_goods_master / cps_goods_source_mapping / cps_goods_price_snapshot / cps_coupon_pool / cps_marketing_short_link / cps_marketing_click_event

App 短码访问
  -> /app-api/cps/s/{shortCode}
  -> AppCpsMarketingShortLinkRedirectController
  -> CpsMarketingShortLinkServiceImpl.resolveTargetUrl() + CpsMarketingClickEventServiceImpl.recordClick()
  -> 302 到既有目标链接

会员端营销入口
  -> /app-api/cps/marketing/activity-center / selection-themes / selection-theme-items
  -> AppCpsMarketingController
  -> AppCpsMarketingServiceImpl
  -> cps_rebate_activity / cps_selection_theme / cps_selection_theme_item
```

阶段 1 商品主档、来源映射、价格快照、券池、营销短链和 `cps_marketing_click_event` 属于营销检索、展示与归因分析层。它们可以服务商品聚合、活动/主题展示、转链前选择、短码分发、基础漏斗和 AI 推荐，但不作为结算事实，不建立第二套供应商转链服务，也不是订单佣金、返利比例、冻结或资产入账事实来源。

关键证据：

- `CpsGoodsMasterController`：提供 `/admin-api/cps/goods-master/page`、`/get`、`/import-selection-item`、`/source/page`、`/price-snapshot/page`，用于查询主档、来源映射、价格快照并从选品主题商品导入。
- `CpsGoodsMasterServiceImpl`：按 `platformCode + vendorCode + goodsId + goodsSign` 复用来源映射；导入时创建或复用商品主档，并追加一条价格快照。
- `CpsCouponPoolController`：提供 `/admin-api/cps/coupon-pool/page`、`/save`、`/usable-list`，用于运营维护券池和按商品查询可用券。
- `CpsCouponPoolServiceImpl.listUsableCoupons()`：只返回 `VALID`、已到开始时间、未过结束时间且 `stockRemain` 为空或大于 0 的券；不注入订单、返利、冻结、提现或 Token 兑换服务。
- `cps_coupon_pool`：保存 `coupon_id`、券金额、门槛、有效期、库存、状态、来源类型、活动/主题关联和最近同步时间；其数据只作为营销可用券筛选依据。
- `CpsMarketingShortLinkController`：提供 `/admin-api/cps/marketing-short-link/create` 和 `/page`，用于创建和查询自有营销短链。
- `AppCpsMarketingShortLinkRedirectController`：提供 `/app-api/cps/s/{shortCode}` 跳转入口，短码有效时返回 302，过期或停用时不跳转到目标链接。
- `CpsMarketingShortLinkServiceImpl`：使用 `SecureRandom` 生成不可枚举短码，按请求摘要幂等复用；会员归因只保存 SHA-256 摘要，不保存明文会员 ID；服务不注入平台转链、订单或资金变更服务。
- `cps_marketing_short_link`：保存短码、目标链接、平台、供应商、已有转链记录、活动、素材、渠道、归因摘要、访问次数和最近访问时间；它包装已有目标链接，不复制供应商转链逻辑。
- `CpsMarketingClickEventServiceImpl`：为短链点击生成 `clickId`，保存渠道、短码、活动、素材、IP/UA/设备/归因摘要和 `dedupeKey`；不保存明文归因身份。
- `CpsMarketingFunnelServiceImpl`：只读短链、点击、已有转链记录和订单状态，聚合曝光/点击/转链/下单/结算/返利可处理基础漏斗，不写订单、返利、冻结、提现或 Token 兑换。
- `AppCpsMarketingServiceImpl`：会员端复用活动中心和选品主题库，只返回已启用活动、已发布有效主题和启用商品快照；会员身份来自登录上下文，不接收前端传入身份字段。

### 3.1.6 滴滴联盟 DUnion 集成

```text
App / MCP 可信会员上下文
  -> CpsGoodsService.generatePromotionLink()
  -> DidiPlatformClientAdapter
  -> DidiOfficialVendorClient
  -> qiji-module-cps-sdk-dunion
  -> DUnion H5 取链接口
  -> 现有转链记录与 sourceId 归因

后台运营
  -> /admin-api/cps/didi-union/*
  -> 素材生成 / 连接测试 / 订单归因诊断
  -> DUnion SDK
```

后台接口与权限：

| 接口 | 权限 | 边界 |
|---|---|---|
| `POST /admin-api/cps/didi-union/material/generate` | `cps:toolbox:link` | 生成 H5/小程序链接、二维码、海报或券码；服务端使用不可归因的 `ops-UUID` |
| `GET /admin-api/cps/didi-union/connection-test` | `cps:api-vendor:query` | 验证当前租户启用的 `didi + official` 配置和上游连通性 |
| `GET /admin-api/cps/didi-union/order-attribution` | `cps:order:query` | 调用 `selfQueryOrder` 诊断，不修改订单或返利资产 |

配置由目标租户在平台管理和 API 供应商管理中创建，不在 SQL 中写固定租户种子。`appKey` 映射 App-Key，`appSecret` 映射 accessKey，`defaultAdzoneId` 映射推广位 ID，`apiBaseUrl` 映射 SDK 基础地址；`extraConfig.timeoutMs` 允许 `1000-30000` 毫秒，默认 `5000` 毫秒。

滴滴订单复用现有同步与返利链路。`sourceId` 映射 `externalId`，CPA 与 CPS 返佣按分求和后转元；退款、风控、取消和结算失败映射退款/失效，只有 SDK 状态 7 映射已结算，其余处理中状态最高映射已付款，避免提前入账。生产 API 不开放模拟订单回调。

完整配置、字段映射和排错说明见 `docs/didi-union-sdk-integration.md`。

### 3.1.5 P3 SDK 标准化基线

```text
vendor client
  -> CpsApiVendorClient.describe()
  -> CpsVendorDescriptor
  -> capability matrix / config schema / governance policy / structured exception
```

关键约束：

- `CpsVendorCapability` 是 `vendor + platform` 能力声明来源，覆盖搜索、解析、转链、查券、查单、活动、图片搜索、选品库和连接测试。
- `CpsVendorConfigSchema` 定义启用前配置校验和敏感字段脱敏摘要，标准字段包括 `appKey/appSecret/apiBaseUrl/authToken/defaultAdzoneId/timeoutMs/rateLimitPerMinute/retryMaxAttempts`。
- `CpsVendorGovernancePolicy` / `CpsVendorRetryPolicy` 定义本地连接治理契约：超时、限流、重试、熔断、token refresh 支持标记、指标和脱敏诊断；默认重试策略为 `idempotentOnly=true`。
- 未实现能力统一使用 `CAPABILITY_UNSUPPORTED`，不得以 `null`、空列表或空结果冒充成功。
- 淘宝、京东、拼多多、抖音 official skeleton 在真实沙箱/测试账号迁移前只声明 `CONNECTION_TEST`；后台供应商保存链路拒绝启用缺少业务能力的 official 配置，防止未完成 client 成为 active vendor。

### 3.1.6 P4 增长分析和生态协同

P4 本地交付聚焦分析与校验，不直接写订单、返利、冻结、退款或 Token 主账本。

```text
Admin growth analytics API
  -> CpsGrowthAnalyticsController
  -> CpsGrowthAnalyticsService
  -> ROI / risk / experiment / Token event reconciliation / billing boundary decision
```

后台接口统一挂在 `/admin-api/cps/growth-analytics`，权限为 `cps:growth-analytics:query`：

| 接口 | 作用 | 边界 |
|---|---|---|
| `POST /roi` | 按曝光、点击、转链、订单、结算、佣金、返利成本和退款回冲计算净收益 | 只读分析，不写营销事件、订单或资产流水 |
| `POST /risk-summary` | 按租户阈值输出未归因、转链、同步、退款欠款和资产差异告警 | 告警编码不直接触发财务状态变更 |
| `POST /experiment/assign` | 对素材或主题排序实验做稳定哈希分流 | 返回脱敏 subject hash，`settlementMutationAllowed=false` |
| `POST /token-reconciliation` | 按租户、业务单号和幂等键识别 CPS 与 TokenHub 事件差异 | 本地只提供对账判定，真实 TokenHub 日切需外部环境验收 |
| `POST /billing-boundary/validate` | 校验 billing-service 是否越界写 CPS 资产或规则 | 只允许消费已确认 CPS 资产事件，拒绝写返利账户、规则、归因和冻结退款策略 |

实施计划与验证证据见 `docs/superpowers/plans/2026-07-15-stage-four-growth-analytics.md`。

### 3.2 订单同步与返利结算

```text
Quartz Job
  -> CpsOrderSyncJob
  -> CpsPlatformService.getEnabledPlatformList()
  -> CpsPlatformClient.queryOrderPage()
  -> CpsOrderSyncPageService.persistPage()
  -> CpsOrderService.saveOrUpdateOrder()
  -> cps_order / cps_order_status_event / cps_order_attribution_log / cps_order_sync_checkpoint / cps_order_sync_failure / cps_platform_bill_row / cps_platform_bill_diff / cps_order_sync_log
  -> CpsRebateSettleJob / CpsRebateSettleService
  -> cps_rebate_record / cps_rebate_account
```

关键证据：

- `CpsOrderPageResult` / `CpsOrderPaginationMode`：订单同步使用显式分页契约，适配器返回 `items`、`PAGE/CURSOR`、`nextCursor`、`nextPageNo` 和 `hasMore`，不再从最后一条订单推断游标。
- `CpsOrderSyncJob`：按启用平台循环同步订单；checkpoint key 由平台、vendor、订单场景和查询类型组成，淘宝 1/2/3 场景独立维护；只有整页 `persistPage()` 成功后才推进 `nextCursor/nextPageNo` 或 `watermarkTime`；查询、持久化、分页异常和页数上限失败会写入 `cps_order_sync_failure`。
- `CpsOrderSyncPageServiceImpl`: page-level transaction boundary now deduplicates repeated `platformCode + platformOrderId` rows inside one upstream page, counts duplicates as skipped, and preserves cross-platform same-id orders.
- `CpsOrderSyncCheckpointDO` / `CpsOrderSyncFailureRecoveryService`: checkpoint records vendor, scene and query type; recovery lists, replays and marks sync failures, while `CpsOrderSyncFailureCompensationJob` handles retry. `CpsPlatformBillReconciliationService` imports platform bills and writes diff records.
- `CpsOrderServiceImpl.bindSpecialIdToMember(CpsOrderManualBindCommand)`: manual special_id binding records attribution audit logs, detects conflicts, and leaves pending compensation paths when settlement has already been affected.
- `CpsOrderServiceImpl`：负责新增/更新平台订单，并向 `cps_order_status_event` 追加不可变状态事件；事件包含来源、同步批次、原始状态摘要、映射状态、当前状态、事件时间、状态版本和拒绝降级原因。
- `CpsOrderServiceImpl`：好单库订单分别保留 `specialId`、`relationId` 与 `externalId/channel_code`；闪购订单只有在租户、供应商、平台、有效期都匹配且 SID 仅有一个有效转链候选时才自动归因，多候选、跨租户或过期记录不会绑定会员。
- `CpsOrderClaimServiceImpl`：支持会员按平台和订单号申领。订单未同步返回 `PENDING_SYNC`，仅凭订单号进入 `PENDING_REVIEW`，已归属他人返回不泄露身份的 `CONFLICT`，已有返利/冻结/实际返利活动返回 `ASSET_LOCKED`；审核批准使用行锁和 `member_id IS NULL` 条件更新，批准前不产生返利或资产变更。
- `CpsPlatformOrderStatusMapper`: centralizes Taobao/JD/PDD/Douyin/local-life raw status mapping, refund override handling and legal status migration checks before local order status changes.
- `CpsFundsTraceService` + `GET /admin-api/cps/order/funds-trace`: read-only admin trace endpoint that aggregates order events, rebate records, freeze records, debt rows, asset ledger entries and platform bill diffs with trace warnings.
- `CpsRebateSettleServiceImpl`：负责返利记录与账户余额更新。
- `AppCpsOrderController`：提供 `GET /app-api/cps/order/page`、`GET /app-api/cps/order/{id}`、`POST /app-api/cps/order/claim` 与 `GET /app-api/cps/order/claim/list`，只使用登录会员上下文查询和申领；非本人订单按不存在处理。
- `CpsOrderController`：提供 `GET /admin-api/cps/order/claim/page` 与 `POST /admin-api/cps/order/claim/review`，管理员审核必须填写核验说明，审核决定、操作员和时间追加到归因日志。

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

- `AppCpsRebateController`：用户端返利账户、预估、提交、查询兑换订单，均使用登录上下文 memberId；`pendingRebate` 来自登录会员的 pending/rebate 返利记录汇总。
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
- `cps_purchase_decision`
- `cps_promotion_strategy_advice`
- `cps_explain_rebate`
- `cps_list_selection_themes`
- `cps_recommend_from_selection_theme`
- `cps_get_rebate_balance`
- `cps_create_token_exchange`
- `cps_query_exchange_status`
- `cpx_list_tasks`
- `cpx_get_task_detail`
- `cpx_generate_tracking_link`
- `cpx_query_conversions`
- `cpx_recommend_tasks_by_scene`
- `cpx_search_articles`

注意：`CpsMcpToolConfiguration` 是 MCP 回调清单，`CpsMcpToolRiskRegistry` 是自测授权风险清单，`backend/sql/module/ai-all.sql` 与 `ai-update.sql` 是 AI 工具管理种子数据；新增工具需要三处同步。

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
| MCP 会员资产工具必须持续只信任可信上下文 | 高 | `CpsGenerateLinkToolFunction` 已将请求体 `memberId` 降为兼容字段并只使用 `ToolContext` 归因；新增转链、订单、返利、兑换类工具仍必须保持这个边界。 |
| 资金链路复杂且跨系统 | 高 | 返利兑换 Token 涉及本地订单、冻结/扣减/解冻、aitoken OpenAPI、幂等、异常 PROCESSING；任何状态机/补偿缺口都可能影响资产一致性。 |
| OpenAPI 签名缺少重放窗口/nonce 存储证据 | 高 | `CpsOpenApiSignatureService` 校验 HMAC、timestamp、nonce、signature，但当前扫描未看到 timestamp 窗口与 nonce 防重放落库/缓存。 |
| 本地配置含开发密钥/默认密码/第三方测试 secret | 高 | `application-local.yaml` 含 MySQL、Redis、RabbitMQ、Wx、OAuth 等本地/测试凭据；不得直接用于生产。 |
| 官方 vendor 多处可能未完全实现 | 中 | 既有技术债文档记录 official client 多处 `return null` / 待实现；当前后台供应商保存链路已拒绝启用只声明 `CONNECTION_TEST` 的 official skeleton，但真实平台迁移、凭证和沙箱验收仍未完成。 |
| 滴滴联盟凭证、日志与归因边界 | 高 | DUnion accessKey 和签名不得出现在日志；后台素材必须使用 `ops-UUID`，只有 App 登录上下文或 MCP 可信上下文可生成会员归因 `sourceId`。 |
| 滴滴联盟状态提前结算 | 高 | DUnion 处理中状态不能直接映射为已结算；只有 SDK 状态 7 可进入已结算，退款、风控、取消和失败必须阻断返利入账。 |
| 订单同步状态机和幂等边界 | 中高 | 订单同步按平台回写订单；既有技术债指出平台状态可能直接覆盖本地状态，需要防乱序/回退/重复入账。 |
| 统计 SQL 可能不走索引 | 中 | `CpsOrderMapper.xml` 对 `create_time` 使用 `DATE(create_time)`，大表统计可能导致索引失效。 |
| MCP 审计闭环仍需生产观测 | 中 | 存在 `CpsMcpAccessLogDO` / Mapper，P0 新增工具和核心工具已逐步接入审计；仍需通过真实 MCP Server 调用验证日志字段、脱敏和授权来源在生产环境一致。 |
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
- 商品主档 Controller：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goodsmaster/CpsGoodsMasterController.java`
- 商品主档服务：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/master/CpsGoodsMasterServiceImpl.java`
- 商品搜索/转链 Controller：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/app/goods/AppCpsGoodsController.java`
- 返利账户/兑换 Controller：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/app/rebate/AppCpsRebateController.java`
- Mall uniapp CPS goods search page: `frontend/mall-uniapp/pages/commission/goods.vue`
- Mall uniapp CPS goods API: `frontend/mall-uniapp/sheep/api/cps/goods.js`
- Mall uniapp CPS order page: `frontend/mall-uniapp/pages/commission/order.vue`
- Mall uniapp CPS order API: `frontend/mall-uniapp/sheep/api/cps/order.js`
- Mall uniapp CPS rebate wallet page: `frontend/mall-uniapp/pages/commission/wallet.vue`
- Mall uniapp CPS rebate API: `frontend/mall-uniapp/sheep/api/cps/rebate.js`
- Mall uniapp CPS withdraw page: `frontend/mall-uniapp/pages/commission/withdraw.vue`
- Mall uniapp CPS withdraw API: `frontend/mall-uniapp/sheep/api/cps/withdraw.js`
- 返利 OpenAPI Controller：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/openapi/rebate/OpenApiCpsRebateController.java`
- 平台策略接口：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/CpsPlatformClient.java`
- 供应商策略接口：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/CpsApiVendorClient.java`
- SDK 标准化描述符/能力/配置/治理：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/CpsVendorDescriptor.java`、`CpsVendorCapability.java`、`CpsVendorConfigSchema.java`、`CpsVendorGovernancePolicy.java`
- 平台/供应商工厂：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/CpsPlatformClientFactory.java`
- 滴滴联盟 SDK：`backend/qiji-module-cps/qiji-module-cps-sdk-dunion/`
- 滴滴联盟适配器：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/didi/`
- 商品服务：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsServiceImpl.java`
- 订单服务：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/order/CpsOrderServiceImpl.java`
- 订单申领服务：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/order/CpsOrderClaimServiceImpl.java`
- 会员活动转链 Controller：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/app/activity/AppCpsRebateActivityController.java`
- 好单库闪购适配器：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/haodanku/HdkElemeVendorClient.java`、`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/eleme/ElemePlatformClientAdapter.java`
- 返利结算：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateSettleServiceImpl.java`
- 返利兑换 Token：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/exchange/CpsRebateTokenExchangeServiceImpl.java`
- aitoken 客户端：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/exchange/CpsAitokenExchangeClient.java`
- OpenAPI 签名：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/exchange/CpsOpenApiSignatureService.java`
- 订单同步 Job：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/job/CpsOrderSyncJob.java`；失败恢复补偿 Job：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/job/CpsOrderSyncFailureCompensationJob.java`
- MCP Tools：`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/`

### 关键文档

- 生态 P0 闭环：`docs/agentic-ecosystem-p0-rebate-token-exchange.md`
- 大淘客高效转链与订单归因：`docs/dataoke-high-efficiency-link-attribution.md`
- 好单库活动转链与订单归因设计：`docs/superpowers/specs/2026-07-30-haodanku-activity-order-attribution-design.md`
- 大淘客搜索页面与商品广场：`docs/dataoke-search-page-implementation.md`
- 大淘客与好单库配置测试：`docs/大淘客与好单库配置及接口测试指南.md`
- 滴滴联盟 SDK 集成：`docs/didi-union-sdk-integration.md`
- 滴滴联盟实施计划：`docs/superpowers/plans/2026-07-10-didi-union-sdk-integration.md`
- P3 SDK 标准化计划：`docs/superpowers/plans/2026-07-14-stage-three-sdk-standardization.md`
- P4 增长分析计划：`docs/superpowers/plans/2026-07-15-stage-four-growth-analytics.md`
- CPS 技术债：`docs/cps-tech-debt-inventory.md`
- CPS 主 R 打样 SOP：`docs/cps-refactor-sop.md`
- CPS 订单同步打样报告：`docs/cps-order-sync-pilot-pre-pr.md`
- CPS Pre-PR 模板：`docs/cps-pre-pr-template.md`
- 测试规范：`agent_improvement/memory/testing-specification.md`
- 代码生成规则：`agent_improvement/memory/codegen-rules.md`
- CPS always 级 AI 规则：`agent_improvement/memory/cps-ai-coding-rules.md`
- CPS 需求/PRD：`docs/CPS系统需求文档.md`、`docs/CPS系统PRD文档.md`
- CPS 模块脚本：`backend/sql/module/cps-all-in-one.sql` 维护新库全量脚本，`backend/sql/module/cps-update.sql` 维护现有库增量更新；不要创建 `backend/sql/mysql/cps-all-in-one.sql`，`ruoyi-vue-pro.sql` 保持系统基础 SQL。

## 本次未做的事

- 当前刷新已覆盖好单库活动转链、SID 自动归因和订单申领的目标单元/数据库/控制器/前端契约测试；全量测试、类型检查、Playwright 和本地服务烟测结果以本次任务最终验收记录为准。
- 本地有效供应商配置已实测大淘客生成 `s.click.taobao.com` 官方链接、好单库闪购生成 `u.ele.me` 官方链接并写入会员 SID 归因记录；好单库淘宝会场仍需在供应商配置补齐已授权的 `tb_name`，会员自动归因还需渠道专属 PID、`relation_id` 和真实联盟测试订单。当前未执行真实下单及上游订单回传验收。
- 未验证 MCP Server 实际启动后的工具列表是否与配置声明完全一致。
- 滴滴联盟真实接口尚需使用有效 App-Key、accessKey、活动 ID 和推广位 ID，在专用测试租户完成冒烟验证；模拟订单回调不属于生产验收入口。
- 本次刷新仅更新文档说明，不改动业务代码。
