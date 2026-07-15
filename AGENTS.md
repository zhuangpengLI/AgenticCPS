# AGENTS.md

This file provides guidance to AI Agents (Qoder, Claude Code, etc.) when working with code in this repository.

## Project Overview

AgenticCPS is a **CPS (Cost Per Sale) Alliance Rebate System** built on ruoyi-vue-pro. It aggregates Taobao, JD.com, Pinduoduo, Douyin, and local-life style affiliate scenarios to provide activity-center operations, rebate toolbox operations, selection-theme operations, rebate search, price comparison, order tracking, and settlement services. The system features AI Agent integration via MCP (Model Context Protocol).

**Key differentiator**: This project uses Vibe Coding + AI autonomous programming — CPS module code is 100% AI-generated (20,000+ lines of code including business services, scheduled jobs, MCP interface layer, and unit tests).

## Codex Agentic Engineering Protocol

AgenticCPS is developed with Codex as an autonomous coding agent, deeply integrated with Superpowers, oh-my-codex (OMX), TDD, Playwright, and Midscene.js. Treat this as the default execution model for every non-trivial change.

### Startup Baseline

- Read the applicable `AGENTS.md`, `README.md`, and `docs/project-map.md` before modifying code or docs.
- Run `git status --short` before changes. Preserve existing user/agent edits and never remove untracked files unless explicitly requested.
- Prefer current filesystem/POM/package names over stale prose. Current backend modules use `qiji-*`, not older `yudao-*` names.
- For unfamiliar SDKs or testing tools, check official documentation or installed package types before coding.
- Keep changes small and reversible. Update docs whenever workflow, commands, entry points, or quality gates change.

### Superpowers Workflow

- Use `brainstorming` before creative or behavior-changing work unless the user has already provided an approved design or implementation plan.
- Use `writing-plans` for multi-step feature/refactor plans before implementation.
- Use TDD for feature and bugfix work: write the smallest failing test, prove it fails, implement the minimum change, prove it passes, then broaden verification.
- Use systematic debugging when behavior is unexpected, a test fails, or logs contradict the current hypothesis.
- Use verification-before-completion before claiming work is complete. Completion requires fresh command output or a clearly stated validation gap.

### oh-my-codex / Subagent Rules

- Default to solo execution for scoped work.
- Use Codex native subagents only for independent, bounded, verifiable subtasks that improve throughput without shared-file conflicts.
- In Codex App outside tmux, do not assume OMX `team`, `hud`, or `question` runtime surfaces are available. Use native structured input or direct execution instead.
- OMX plans and logs under `.omx/` are operational state. Do not hand-edit them unless recovering a broken workflow or explicitly asked.

### TDD Quality Gates

- CPS P0/P1 paths require tests before implementation: rebate freeze/deduct, exchange saga, order state transitions, tenant isolation, OpenAPI signatures, MCP member attribution, MCP audit logs, and platform adapter failures.
- Backend test choice:
  - Pure logic, adapters, and controller orchestration: `BaseMockitoUnitTest` or Mockito extension patterns already used in the CPS module.
  - Mapper, DB state, idempotency, or transaction-sensitive behavior: `BaseDbUnitTest` / H2-based module tests.
  - Redis-sensitive behavior: Redis test base from the framework.
- Frontend issue fixes start with Playwright reproduction in `frontend/admin-vue3/e2e`. Keep Playwright assertions deterministic.
- Midscene.js is an assisted visual/semantic E2E layer for admin-vue3 only. It may describe UI intent, but final acceptance must still include Playwright `expect` assertions or backend tests.
- Do not commit model credentials. Midscene reads only environment variables such as `MIDSCENE_MODEL_BASE_URL`, `MIDSCENE_MODEL_API_KEY`, `MIDSCENE_MODEL_NAME`, and `MIDSCENE_MODEL_FAMILY`.

### Current Project Map Notes (2026-05-24)

- Current backend module names use the `qiji-*` prefix even where older docs still mention `yudao-*`: main app is `backend/qiji-server`, CPS module is `backend/qiji-module-cps`, framework is `backend/qiji-framework`.
- Backend entry point is `backend/qiji-server/src/main/java/com/qiji/cps/server/QijiServerApplication.java`; it scans `${qiji.info.base-package}.server` and `${qiji.info.base-package}.module`, with `qiji.info.base-package: com.qiji.cps`.
- Runtime config is split between `backend/qiji-server/src/main/resources/application.yaml` and `application-local.yaml`; local backend port is `48080`.
- API prefixes are framework-driven: `controller.admin` maps to `/admin-api`, `controller.app` maps to `/app-api`; CPS `controller.openapi` declares its own `/openapi/...` routes.
- CPS critical flow map: App/MCP -> CPS controller/tool -> `CpsGoodsService` / exchange services -> `CpsPlatformClientFactory` or aitoken OpenAPI -> external platform / aitoken -> CPS DB records.
- Activity center flow: Admin activity center -> `CpsRebateActivityController` -> `CpsRebateActivityService` -> `cps_rebate_activity`; card `search` jumps to `frontend/admin-vue3/src/views/cps/goods/square/index.vue` with platform/keyword/tag query params.
- Rebate toolbox flow: Admin toolbox -> `frontend/admin-vue3/src/views/cps/toolbox/index.vue` -> `CpsGoodsRebateQueryController` toolbox endpoints -> `CpsGoodsToolboxService` -> existing rebate query, goods square, transfer record, and platform parsing services.
- Selection ranking flow: Admin selection theme -> `frontend/admin-vue3/src/views/cps/selection/theme/index.vue` -> `/admin-api/cps/selection-theme` -> `CpsSelectionThemeService` -> `cps_selection_theme` / `cps_selection_theme_item`; list-style features such as hot goods recommendations, anchor sales rankings, 9.9 free-shipping zones, and blogger-window hot picks are modeled as selection themes/templates/rules over item snapshots, not separate financial truth tables.
- Generated companion map: `docs/project-map.md` contains the latest read-only project map and should be refreshed when module ownership, entrypoints, commands, or risk areas change.

## Agentic Ecosystem Relationship

AgenticCPS is one service in a three-project Agentic ecosystem. Future features must preserve these boundaries unless the user explicitly asks for an architecture change.

Current ecosystem capability baseline:

- `AgenticTokenHub` already has a multi-model gateway, Token billing, membership plans, credit/points transfer, and payment capabilities. It is the ecosystem's AI capability and Token settlement foundation.
- `AgenticCPS` already plans and implements activity-center operations, rebate toolbox operations, selection-theme libraries, ranking shelves, product search, price comparison, promotion link generation, order tracking, rebate summary, and MCP tools. It is the ecosystem's CPS rebate asset and product recommendation service.
- `AgenticAIoT` is positioned as a device access, data flow, rule engine, AI operations, and multi-protocol IoT platform. It is the ecosystem's enterprise device data and AI operations scenario entry.

The missing ecosystem integration is **account interoperability, asset conversion, scenario linkage, and Agent-callable interfaces**. Do not merge the three systems into a monolith. For every new feature, first decide which project owns the responsibility, then connect systems through OpenAPI, MCP tools, or event ledgers.

| Project | Path | Role | Owns | Must Not Own |
|---------|------|------|------|--------------|
| AgenticCPS | `F:\ai\AgenticCPS` | CPS rebate and product recommendation service | Activity-center operations, rebate toolbox operations, selection-theme libraries and item snapshots, ranking shelves such as hot goods, anchor sales, 9.9 free-shipping, and blogger-window picks, CPS platform adapters, goods search, price comparison, promotion links, batch transfer, content parsing, order tracking, rebate settlement, rebate freeze/deduct, CPS MCP tools, AIoT scene-based product recommendation | Model gateway, Token master ledger, IoT device ingestion, IoT rule engine |
| AgenticTokenHub | `F:\ai\AgenticTokenHub` | AI Token and model billing foundation | Multi-model gateway, Token wallet/quota, membership plans, external rebate-to-Token exchange, API Key quota, AI usage cost accounting, Token MCP tools | CPS orders, CPS rebate settlement, product recommendation, IoT devices |
| AgenticAIoT | `F:\ai\AgenticAIoT` | Enterprise AIoT data and operations scenario service | Device access, metrics, alerts, rules, AI analysis tasks, purchase-need generation, CPS recommendation trigger, AIoT MCP tools | Token wallet master ledger, CPS rebate accounting, ecommerce platform adapters |

The ecosystem direction is:

```text
Unified user, unified account, unified entitlements, unified OpenAPI auth,
unified MCP tools, unified event ledger.
```

### Current P0 Closed Loop

The implemented P0 business loop is:

```text
AgenticCPS AVAILABLE rebate
        -> freeze / idempotency / reconciliation
        -> AgenticTokenHub Token exchange submit
        -> new-api user.quota credit
        -> CPS confirm deduct or unfreeze on failure
```

Relevant local documentation:

- `docs/agentic-ecosystem-p0-rebate-token-exchange.md`
- `docs/cps-tech-debt-inventory.md`

### Development Boundary Rules

- When adding CPS-facing features, keep CPS money movement inside AgenticCPS: rebate balance, freeze, unfreeze, deduct, refund/debt, and exchange order state.
- When adding AI Token features, keep Token balance and model usage accounting inside AgenticTokenHub. AgenticCPS may call aitoken OpenAPI but must not create a parallel Token ledger.
- When adding AIoT features, AgenticAIoT should generate structured analysis and purchase needs, then call AgenticCPS for product recommendation and AgenticTokenHub for model invocation/Token billing.
- Service-to-service calls must use the shared OpenAPI headers: `X-App-Id`, `X-Tenant-Id`, `X-Timestamp`, `X-Nonce`, `X-Signature`, `X-Idempotency-Key`.
- Any money or Token mutation must be idempotent and auditable. Required evidence fields are source system, source order id, tenant id, user/member id, idempotency key, status, failure reason, and timestamps.
- Never trust request-body `memberId` or `userId` for member assets in user-facing APIs. Use login context or a verified service signature.
- Only `AVAILABLE` CPS rebate can be exchanged to Token. Pending, refunded, invalid, frozen, withdrawn, or debt-related amounts are not exchangeable.
- The exchange saga must follow: create local exchange order -> freeze rebate -> call aitoken submit -> confirm deduct on success -> unfreeze on failure -> keep PROCESSING on timeout for compensation.
- Keep P0/P1/P2/P3/P4 scope boundaries clear. Do not mix AIoT device features into CPS or Token wallet features into CPS just because a workflow crosses projects.

### Roadmap Constraints

| Phase | Goal | Implementation Guidance |
|-------|------|-------------------------|
| P0 | CPS rebate -> aitoken Token | Maintain the existing exchange saga and HMAC OpenAPI contract. |
| P1 | CPS as AI-callable shopping tools | Improve `cps_search_goods`, `cps_compare_prices`, `cps_generate_link`, selection-theme recommendation, and scene recommendation without changing Token ownership. |
| P2 | AIoT consumes aitoken Token | AgenticAIoT should call aitoken `/v1/chat/completions` with metadata and usage accounting. |
| P3 | AIoT drives CPS recommendation | AIoT creates purchase needs; CPS maps them to categories/products/promotion links. |
| P4 | Ecosystem hub | Only introduce a separate hub when user/account/auth/event responsibilities outgrow repo-local contracts. |

## Architecture

```
AgenticCPS/
├── backend/                    # Spring Boot 3.5.9 Java backend
│   ├── qiji-server/           # Main application entry point (port 48080)
│   ├── qiji-module-cps/       # CPS rebate core module (primary focus)
│   │   ├── qiji-module-cps-api/     # API definitions (enums, remote interfaces)
│   │   └── qiji-module-cps-biz/     # Business implementation
│   │       ├── controller/admin/    # Admin REST APIs, including activity center, rebate toolbox, selection library, and goods square
│   │       ├── controller/app/      # Member-facing REST APIs
│   │       ├── service/             # Business services (goods, toolbox, order, rebate, activity, selection, exchange, etc.)
│   │       ├── client/              # CPS platform adapters (Strategy pattern)
│   │       │   ├── taobao/          # Taobao affiliate adapter
│   │       │   ├── jingdong/        # JD.com affiliate adapter
│   │       │   ├── pinduoduo/       # Pinduoduo affiliate adapter
│   │       │   └── douyin/          # Douyin affiliate adapter
│   │       ├── dal/                  # Data access layer (MyBatis Plus, CPS core tables)
│   │       ├── job/                  # Scheduled jobs (Quartz - order sync, status update)
│   │       └── mcp/                  # MCP AI interface layer
│   │           └── tool/             # MCP tool functions for goods, rebate, exchange, scenes, and selection libraries
│   ├── qiji-module-ai/       # AI module (Spring AI 1.1.2 + MCP support)
│   ├── qiji-module-member/   # Member management
│   ├── qiji-module-pay/      # Payment/wallet system
│   ├── qiji-module-mall/     # E-commerce module
│   ├── qiji-module-system/   # System management (auth, perms, menus)
│   ├── qiji-module-infra/    # Infrastructure (Redis, file storage, MQ)
│   ├── qiji-module-report/   # Report & dashboard module
│   ├── qiji-module-mp/       # WeChat Official Account module
│   ├── qiji-framework/       # Framework extensions (Web, Security, MyBatis, Redis, Job, Tenant, Data Permission, MQ, Monitor, Excel)
│   ├── qiji-dependencies/    # Centralized dependency version management
│   └── sql/                   # Database schema scripts for each module
│
├── frontend/
│   ├── admin-vue3/           # Vue3 admin panel (Element Plus + TypeScript)
│   ├── admin-uniapp/         # uni-app mobile admin (Node.js >= 20, pnpm >= 9)
│   └── mall-uniapp/          # E-commerce mobile app (UniApp multi-platform)
│
├── script/                    # Build and deployment scripts
│   └── docker/               # Docker Compose for one-click deployment
│
├── references/                # Agent reference documents, literature & standards
│
├── releases/                  # Version release packages
│
├── agent_improvement/         # AI agent improvement & rules
│   └── memory/               # Code generation rules and AI memory
│       ├── MEMORY.md         # Memory index
│       └── codegen-rules.md  # Code generation rules (Velocity templates)
│
└── docs/                      # PRD and requirements documents
```

## Reference Projects

When discussing requirements, you can refer to these projects for business logic:

| Project | Path | Description |
|---------|------|-------------|
| sfb | `F:\ai\cps` | CPS reference project |
| xc-union | `F:\ai\cps2` | CPS reference project |

## Tech Stack

| Layer | Technology |
|-------|-------------|
| Backend Framework | Spring Boot 3.5.9, Spring Security 6.5.2 |
| Language | Java 17/21 |
| ORM | MyBatis Plus 3.5.15 |
| Cache | Redis 7.0, Redisson 3.35.0 |
| Database | MySQL 5.7/8.0+ (also supports Oracle, PostgreSQL, SQLServer, DM, KingBase, GaussDB, openGauss) |
| Frontend | Vue 3.5.12, Element Plus 2.11.1, TypeScript 5.3.3 |
| Build Tool | Maven 3.8+, pnpm 8.6+ (admin-uniapp requires pnpm >= 9) |
| Mobile | UniApp (admin-uniapp: Node.js >= 20; mall-uniapp: multi-platform) |
| AI | Spring AI 1.1.2 + MCP Protocol (Streamable HTTP, JSON-RPC 2.0) |
| Workflow | Flowable 7.2.0 |
| Job Scheduler | Quartz 2.5.0 |
| APM | SkyWalking 9.5.0 |
| MapStruct | 1.6.3 |

## Common Commands

### Backend

```bash
cd backend

# Full compile
mvn clean compile

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run with specific profile
mvn spring-boot:run -pl qiji-server -Dspring-boot.run.profiles=local

# Build JAR
mvn clean package -DskipTests
```

### Frontend

```bash
cd frontend/admin-vue3

# Install dependencies
pnpm install

# Development server
pnpm dev

# Build for production
pnpm build:prod

# Type check
pnpm ts:check

# Lint
pnpm lint:eslint
```

### Docker

```bash
cd backend/script/docker

# Start all services (MySQL, Redis, backend, frontend)
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f server
```

### Additional Verified Commands (2026-05-18)

```bash
cd backend

# Run a CPS biz module test class
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsRebateTokenExchangeServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"

# Start backend with current module name
mvn spring-boot:run -pl qiji-server -Dspring-boot.run.profiles=local
```

```bash
cd frontend/admin-vue3

# E2E support added in this repo state
pnpm e2e:install
pnpm dev:e2e
pnpm e2e
```

```bash
cd frontend/admin-uniapp

# UniApp admin checks/builds
pnpm type-check
pnpm lint
pnpm build:prod
```

## Key Patterns

### CPS Platform Adapter (Strategy Pattern)

Each CPS platform implements `CpsPlatformClient` interface:

```java
public interface CpsPlatformClient {
    String getPlatformCode();
    CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request);
    CpsGoodsDetail getGoodsDetail(CpsGoodsDetailRequest request);
    CpsParsedContent parseContent(String content);
    CpsPromotionLink generatePromotionLink(CpsPromotionLinkRequest request);
    List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request);
    boolean testConnection();
}
```

To add a new platform (e.g., Weibo), implement this interface and register as a Spring Bean. No core logic changes required. Platform registration is managed by `CpsPlatformClientFactory`.

### Haodanku OpenAPI Docs

开发好单库相关功能时，先读取 `haodanku-openapi-docs/AI使用说明.md` 和 `haodanku-openapi-docs/接口目录.md`，按意图路由到对应分类文档再编码。

### Dataoke High-Efficiency Link Attribution

开发大淘客淘宝高效转链、订单同步、推广位管理或 MCP/App 转链归因时，先读取 `docs/dataoke-high-efficiency-link-attribution.md`。

- `pid` is the commission adzone. Channel/member attribution requires a matching dedicated PID, not a normal PID.
- `channelId` / `relationId` identifies channel orders. It must be passed with a channel-dedicated PID, and order sync must query channel orders with `orderScene=2`.
- `specialId` identifies member-operation orders. It must use a member-dedicated PID, and order sync must query member orders with `orderScene=3`.
- `externalId` is only a developer-defined binding marker used to obtain/map `specialId`; do not treat it as a standalone order-attribution guarantee.
- App and MCP link generation must prefer trusted login context or ToolContext over request-body `memberId`; never let user-supplied member IDs decide rebate ownership.
- Orders without `specialId`, `relationId`, or a verified external mapping can be stored for reconciliation, but must not automatically mutate member rebate assets.

### Dataoke Search Page / Goods Square

开发大淘客搜索页、商品广场、热搜记录、搜索联想词、搜索召回或搜索结果转链前，先读取 `docs/dataoke-search-page-implementation.md`。

- 大淘客官方搜索页由热搜记录、搜索联想词、超级搜索、大淘客搜索、联盟搜索组合实现；默认优先使用大淘客搜索，超级搜索/联盟搜索只作为扩展召回或补量策略。
- 当前管理后台商品广场入口是 `frontend/admin-vue3/src/views/cps/goods/square/index.vue`，后端入口是 `CpsGoodsSquareController` 和 `CpsGoodsSquareServiceImpl`。
- 淘宝 dataoke 搜索默认走 `DtkTaobaoVendorClient` 的 `/goods/get-dtk-search-goods`；热搜走 `/category/get-top100`；联想词走 `/goods/search-suggestion`。
- 搜索结果只用于导购展示、运营选品和转链前置展示，不得直接驱动订单归因、返利入账、冻结/扣减或 Token 兑换。
- 前端搜索页必须覆盖首屏热搜、输入联想、提交搜索、空状态、加载中、接口失败、平台切换和转链成功/失败状态。

### Activity Center / Rebate Activity Cards

The admin activity center is backed by `cps_rebate_activity` and `CpsRebateActivityService`. It is an operations-configured source of truth with optional vendor metadata enhancement only; do not add external SDK dependencies just to populate activity cards.

- Admin API: `GET /admin-api/cps/rebate-activity/center` with `platformCode`, `billingType`, `keyword`, `sortMode`, `pageNo`, and `pageSize`.
- Frontend page: `frontend/admin-vue3/src/views/cps/activity/square/index.vue`; card `search` jumps to `frontend/admin-vue3/src/views/cps/goods/square/index.vue`.
- Card fields include activity name, platform code/name/logo, main image, `CPS` / `CPA` / `CPS+CPA`, promotion count, reward text, activity window, jump type/params, source type, external activity id, and tag text.
- Required permissions: `cps:rebate-activity:query/create/update/delete`.
- Fallback tabs must include hot, Meituan, Eleme, Douyin, local life, Fliggy, Pinduoduo, Taobao, and JD even when no platform adapter exists yet.

### Rebate Toolbox

The admin rebate toolbox is the unified operations workbench for parsing, batch transfer, ownership checking, coupon querying, goods-square selection, cash-gift planning, and promotion copy editing. It is inspired by Dataoke-style tool workspaces but must reuse existing CPS services instead of creating a second transfer pipeline.

- Frontend page: `frontend/admin-vue3/src/views/cps/toolbox/index.vue`.
- Admin APIs: `POST /admin-api/cps/goods/parse`, `POST /admin-api/cps/goods/batch-transfer`, `POST /admin-api/cps/goods/ownership-check`, `POST /admin-api/cps/goods/coupon-query`, and `POST /admin-api/cps/goods/cash-gift/plan`.
- Backend service: `CpsGoodsToolboxService`; batch transfer delegates to `CpsGoodsRebateQueryService.queryRebate()` per item.
- Batch transfer accepts at most 20 nonblank inputs, preserves input index, and does not stop the whole batch when one item fails.
- Permissions: `cps:toolbox:query` for parsing/viewing/ownership/coupon checks and `cps:toolbox:link` for transfer or cash-gift planning actions.
- Cash-gift planning is plan-only until a real Taobao official or vendor cash-gift creation API is integrated; do not mutate balances or create real subsidies from the toolbox.

### Selection Library / Theme Goods Shelf

The admin selection library is backed by `cps_selection_theme` and `cps_selection_theme_item`. It is an operations-owned thematic goods shelf: themes store reusable rules, while items store third-party goods snapshots captured at import/refresh time.

- Admin API root: `/admin-api/cps/selection-theme`; permissions use `cps:selection-theme:*`.
- Frontend page: `frontend/admin-vue3/src/views/cps/selection/theme/index.vue`.
- Data boundary: selection item prices, coupons, commissions, sales, and shop/category fields are snapshots for operations and AI recommendation only; they must not drive rebate settlement, order attribution, freeze/deduct, withdrawal, or Token exchange.
- Ranking/shelf features such as hot goods recommendations, anchor sales rankings, 9.9 free-shipping zones, and blogger-window hot picks should be represented as selection themes, promotion templates, or vendor-pulled theme snapshots. They reuse theme rules and item snapshots rather than introducing separate settlement or order-attribution models.
- Import sources are fixed as `MANUAL`, `AI_RECOMMEND`, `VENDOR_PULL`, and `PROMOTION_TEMPLATE`; item status is `ENABLED` / `DISABLED`; theme status is `DRAFT` / `PUBLISHED` / `OFFLINE`.
- Third-party pull must reuse `CpsGoodsSquareService` / `CpsGoodsService` / `CpsPlatformClientFactory`; do not add a platform adapter just for selection-library ingestion.
- AI recommendation must remain explainable: deterministic rule scoring decides ranking, while LLM output may only enrich theme copy and item recommendation reasons. Never let LLM output overwrite third-party facts such as `goodsId`, price, coupon, commission, or sales.
- Ranking logic should stay explainable: deterministic scoring may combine commission amount/rate, coupon price, monthly sales, activity tags, platform/vendor weights, source rank tags, and manual top/sort fields.
- Built-in promotion templates create draft themes only. Operations must explicitly publish after reviewing rules and imported items.

### MCP AI Interface Layer

Located in `qiji-module-cps-biz/mcp/tool/`, MCP tool functions registered via Spring AI include:

| Tool Class | Tool Name | Description |
|-----------|-----------|-------------|
| `CpsSearchGoodsToolFunction` | `cps_search_goods` | Search goods across platforms with keyword, platform filter, price range, pagination |
| `CpsComparePricesToolFunction` | `cps_compare_prices` | Cross-platform price comparison, returns cheapest/highest-rebate/best-overall |
| `CpsGenerateLinkToolFunction` | `cps_generate_link` | Generate promotion links with rebate tracking (short/long/token/mobile) |
| `CpsQueryOrdersToolFunction` | `cps_query_orders` | Query member orders and rebate status |
| `CpsGetRebateSummaryToolFunction` | `cps_get_rebate_summary` | Query rebate account: balance, pending, total, recent records |
| `CpsRecommendBySceneToolFunction` | `cps_recommend_by_scene` | Recommend CPS goods for AIoT or scenario-based purchase needs |
| `CpsListSelectionThemesToolFunction` | `cps_list_selection_themes` | List published selection-library themes |
| `CpsRecommendFromSelectionThemeToolFunction` | `cps_recommend_from_selection_theme` | Return goods from a published selection theme; only generate promotion links when explicitly requested with trusted member context |
| `CpsGetRebateBalanceToolFunction` | `cps_get_rebate_balance` | Query exchangeable rebate balance |
| `CpsCreateTokenExchangeToolFunction` | `cps_create_token_exchange` | Create a rebate-to-Token exchange order |
| `CpsQueryExchangeStatusToolFunction` | `cps_query_exchange_status` | Query rebate-to-Token exchange status |

**MCP Protocol Details:**
- Transport: Streamable HTTP (JSON-RPC 2.0)
- Endpoint: `/mcp/cps`
- Authentication: API Key (managed via `cps_mcp_api_key` table)
- Access logging: `cps_mcp_access_log` table (tool name, params, duration, client IP)
- Security: Spring Security permits MCP SSE and HTTP endpoints for AI Agent access
- Context: `ToolContext` passes current logged-in member ID for order attribution

### Rebate Calculation Priority

System resolves rebate rate in this order:
1. Member personal config (exact platform) → 2. Member personal config (all platforms) → 3. Level + Platform → 4. Level (all platforms) → 5. Platform default → 6. Global default

### Order Status Flow

```
已下单 → 已付款 → 已收货 → 已结算 → 已到账
                  ↓
              已退款 / 已失效
```

Order sync is handled by Quartz scheduled jobs with incremental sync every 5 minutes.

## Code Generation Rules

Business code generation follows rules defined in `agent_improvement/memory/codegen-rules.md`.

### Supported Templates

| Template | Frontend Framework | Notes |
|----------|-------------------|-------|
| vue3 | Vue3 + Element Plus | Standard CRUD pages |
| vue3_vben | Vue3 + Vben Admin | Modal-based forms |
| vue3_vben5_antd | Vue3 + Vben5 + Antd | VxeTable + Ant Design |
| vue3_admin_uniapp | UniApp (Mobile) | Wot design components |

### Backend Template Types

| templateType | Type | Description |
|-------------|------|-------------|
| 1 | Common | Standard CRUD + pagination |
| 2 | Tree | Tree structure with parent-child relationship |
| 11 | ERP Master | Master-detail with independent sub-table operations |

See `agent_improvement/memory/codegen-rules.md` for full details.

## Database Conventions

- All monetary amounts stored in **cents** (Integer, not BigDecimal)
- All timestamps in **Shanghai timezone**
- Soft delete via `deleted` bit field
- Multi-tenancy via `tenant_id` column
- Database script convention for every module: maintain exactly `backend/sql/module/<module>-all.sql` for new-database initialization and `backend/sql/module/<module>-update.sql` for existing-database upgrades. Do not create date-named module SQL scripts. Every update block in `<module>-update.sql` must include its modification date/time, and every schema or seed-data change must be synchronized into the matching `<module>-all.sql`.
- CPS module tables: `cps_*` prefix
- CPS database scripts live under `backend/sql/module/`: `cps-all-in-one.sql` is the full baseline script for new databases, and `cps-update.sql` is the incremental upgrade script for existing databases. Every update block in `cps-update.sql` must include its modification date/time record.
- Do not create or update `backend/sql/mysql/cps-all-in-one.sql` or one-off CPS SQL scripts under `backend/sql/mysql`; keep CPS tables, seed data, menus, permissions, and incremental updates in the module scripts to prevent drift. Do not add CPS tables, seed data, menus, or permissions to `backend/sql/mysql/ruoyi-vue-pro.sql`.

## Configuration

| Environment | File | Port |
|-------------|------|------|
| Local Dev | `application-local.yaml` | 48080 |
| Docker | environment variables (`docker.env`) | 48080 |

Key configs in `application-local.yaml`:
- `spring.datasource.dynamic` — MySQL connection (Druid connection pool)
- `spring.data.redis` — Redis connection
- `yudao.cps.mcp.*` — MCP server settings (SSE/HTTP endpoint config)
- CPS platform API keys (淘宝/京东/拼多多/抖音)

### Docker Deployment

```bash
cd backend/script/docker

# Start all services (MySQL 8, Redis 6, backend, frontend)
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f server
```

Docker port mappings: backend 48080 → 48080, MySQL 3306 → 3306, Redis 6379 → 6379, frontend 8080 → 80.

## File Operation Rules (CRITICAL - Enforced)

> **This rule has been violated 4 times, causing irreversible UTF-8 corruption in Chinese text files.**

### NEVER use PowerShell to read/write files containing Chinese characters

PowerShell `Get-Content` / `Set-Content` defaults to system encoding (ANSI/GBK on Windows), which **corrupts UTF-8 multi-byte sequences** for Chinese characters. This applies to ALL project files: `.sql`, `.java`, `.ts`, `.yaml`, `.json`, `.xml`, etc.

### ALWAYS use Python for file operations

```python
# Read
with open(file_path, encoding='utf-8') as f:
    content = f.read()

# Modify
content = content.replace('old_value', 'new_value')

# Write back (no BOM, preserve line endings)
with open(file_path, 'w', encoding='utf-8', newline='') as f:
    f.write(content)

# Always verify after writing
with open(file_path, 'rb') as f:
    f.read().decode('utf-8')  # raises if encoding is broken
print('OK: ' + file_path)
```

### Safe template for batch replacement across multiple files

```python
import subprocess

replacements = [
    ('SECRET_KEY_OLD', 'PLACEHOLDER_NEW'),
    # add more pairs...
]
files = [
    'backend/sql/module/cps-all-in-one.sql',  # CPS full baseline SQL
    'backend/sql/module/cps-update.sql',  # CPS existing-database update SQL
    'backend/sql/mysql/ruoyi-vue-pro.sql',  # non-CPS base SQL only
    'backend/sql/oracle/ruoyi-vue-pro.sql',
    # add more files...
]

for path in files:
    with open(path, encoding='utf-8') as f:
        text = f.read()
    for old, new in replacements:
        text = text.replace(old, new)
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(text)
    # Verify UTF-8 integrity
    with open(path, 'rb') as f:
        f.read().decode('utf-8')
    print('OK: ' + path)
```

### When recovering from git history

If files are already corrupted, recover from git and re-apply changes via Python:

```python
import subprocess

# Replace CLEAN_COMMIT with the last known-good commit hash
result = subprocess.run(
    ['git', 'show', 'CLEAN_COMMIT:path/to/file.sql'],
    capture_output=True
)
text = result.stdout.decode('utf-8')  # always decode as utf-8
for old, new in replacements:
    text = text.replace(old, new)
with open('local/path/to/file.sql', 'w', encoding='utf-8', newline='') as f:
    f.write(text)
```

### Also avoid: `git filter-repo --replace-text` on UTF-8 files

`git filter-repo --replace-text` operates at the **byte level** and can corrupt UTF-8 multi-byte sequences when replacement strings overlap byte boundaries. Use Python-based file replacement + clean commit instead.

---

## Risk Areas

- **Uncommitted workspace state**: this repository may contain existing user/agent edits. Always check `git status --short` before changing files and avoid overwriting unrelated work.
- **Naming drift**: older documentation and generated snippets may still reference `yudao-*`, while current modules are `qiji-*`. Prefer actual filesystem/POM names over stale prose.
- **CPS money movement**: rebate balance, freeze, unfreeze, deduct, exchange order, refund/debt, and reconciliation are high-risk. Keep operations idempotent and auditable.
- **Cross-system Token exchange**: `CpsRebateTokenExchangeServiceImpl`, `CpsAitokenExchangeClient`, and OpenAPI signature code form the P0 saga. Preserve create-order -> freeze -> aitoken submit -> confirm deduct/unfreeze -> PROCESSING-on-timeout semantics.
- **OpenAPI signature and replay protection**: current HMAC verification depends on `X-App-Id`, `X-Tenant-Id`, `X-Timestamp`, `X-Nonce`, `X-Signature`, and `X-Idempotency-Key`. Any change must consider timestamp windows, nonce replay, tenant isolation, and body canonicalization.
- **Member identity trust boundary**: never trust request-body `memberId` / `userId` for user-facing asset operations. Use login context or verified service signatures. MCP link generation is especially sensitive because attribution affects rebates.
- **Dataoke attribution fields**: `externalId` alone does not prove who placed an order. For Taobao Dataoke links, preserve and validate dedicated PID + `relationId` (`orderScene=2`) or dedicated PID + `specialId` (`orderScene=3`) before member rebate mutation.
- **Platform adapter completeness**: official vendor clients may be partial; switching `active_vendor_code` can turn unimplemented adapters into silent empty results or null link failures.
- **Activity center data boundary**: operation-configured cards are the stable source; vendor/source metadata is optional enhancement. Keep `search/url/none` jump semantics safe, and do not make card availability depend on external platform calls.
- **Selection library snapshot boundary**: theme items and ranking shelves are marketing/operations snapshots, not financial truth. Refresh/import must preserve tenant isolation, item dedupe, source/status fields, and explainable ranking fields; AI copy must not mutate platform facts.
- **Order sync and settlement state**: platform order sync can receive duplicates or out-of-order state changes. Guard against status rollback, duplicate rebate records, and repeated account mutation.
- **MCP auditability**: MCP access log tables exist, but tool-level logging may be incomplete. Changes to MCP tools should preserve tool name, parameter summary, member context, status, duration, and error reason.
- **Statistics performance**: SQL that wraps indexed time columns, such as `DATE(create_time)`, can degrade on large tables. Prefer range predicates when touching dashboard/statistics queries.
- **Money units drift**: project rules prefer integer cents, but existing CPS monetary fields include `BigDecimal`. Do not add new mixed-unit fields without documenting units and compatibility.
- **Encoding safety**: do not use PowerShell `Get-Content` / `Set-Content` or byte-level replacement on Chinese files. Use Python UTF-8 read/write and verify decoding after writes.

## Important Notes

- **Integer for money**: Never use Double/BigDecimal for monetary amounts — always use Integer (cents) to avoid floating-point errors
- **Timezone**: System configured for Asia/Shanghai; ensure database and JVM match
- **Multi-tenancy**: All CPS queries must include tenant isolation
- **Soft delete**: Use MyBatis Plus `deleted` column, never hard delete CPS data
- **Password**: Default admin password is `admin` (change in production)
- **Database support**: Primary MySQL, also supports Oracle, PostgreSQL, SQLServer, 达梦, 人大金仓, GaussDB, openGauss
- **pnpm version**: admin-uniapp requires pnpm >= 9, Node.js >= 20; admin-vue3 requires pnpm >= 8.6, Node.js >= 16

## Performance Benchmarks

| Metric | Target |
|--------|--------|
| Single platform search | < 2s (P99) |
| Multi-platform price comparison | < 5s (P99) |
| Promotion link generation | < 1s |
| Order sync delay | < 30 minutes |
| Rebate credit | Within 24h after platform settlement |
| MCP Tool call (search) | < 3s |
| MCP Tool call (query) | < 1s |
