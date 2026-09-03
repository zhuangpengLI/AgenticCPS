# AGENTS.md

Compact operating guidance for agents in `F:\ai\AgenticCPS`.

This file is intentionally short to reduce fixed prompt/cache tokens. Do not paste or preload long project documents unless the current task needs them. Prefer targeted `rg` lookup and read only the relevant sections of referenced files.

## Startup

- Run `git status --short` before edits. Preserve existing user/agent changes and never remove untracked files unless explicitly requested.
- Read `README.md` and `docs/project-map.md` only when the task touches project entry points, commands, module ownership, workflow docs, or unclear architecture. Use targeted section reads instead of full-file loading.
- Prefer current filesystem/POM/package names over stale prose. Backend modules currently use `qiji-*`, not older `yudao-*` names.
- For unfamiliar SDKs, frameworks, APIs, or test tools, check official docs or installed package types before coding.
- Keep diffs small, reversible, and scoped. Update docs when commands, entry points, quality gates, or workflow behavior change.

## Project Map

AgenticCPS is the CPS rebate and product recommendation service in the Agentic ecosystem.

- Backend: Spring Boot CPS service under `backend/`.
- Main app: `backend/qiji-server`, local port `48080`.
- CPS module: `backend/qiji-module-cps`.
- Framework: `backend/qiji-framework`.
- Admin frontend: `frontend/admin-vue3`.
- Mobile/admin UniApp: `frontend/admin-uniapp`.
- Mall UniApp: `frontend/mall-uniapp`.
- Current detailed read-only map: `docs/project-map.md`.

API prefixes:

- Admin controllers: `/admin-api`.
- App controllers: `/app-api`.
- CPS OpenAPI controllers declare their own `/openapi/...` routes.
- MCP endpoint: `/mcp/cps`.

## Ecosystem Boundaries

Do not merge the three Agentic systems into a monolith.

| Project | Owns | Must not own |
| --- | --- | --- |
| `AgenticCPS` (`F:\ai\AgenticCPS`) | CPS activity operations, rebate toolbox, selection themes/items, platform adapters, goods search, price comparison, promotion links, order tracking, rebate settlement, rebate freeze/deduct, CPS MCP tools, AIoT scene product recommendations | Model gateway, Token master ledger, IoT device ingestion, IoT rule engine |
| `AgenticTokenHub` (`F:\ai\AgenticTokenHub`) | Multi-model gateway, Token wallet/quota, membership plans, rebate-to-Token exchange intake, API key quota, AI usage accounting, Token MCP tools | CPS orders, CPS rebate settlement, product recommendation, IoT devices |
| `AgenticAIoT` (`F:\ai\AgenticAIoT`) | Device access, metrics, alerts, rules, AI analysis tasks, purchase-need generation, CPS recommendation trigger, AIoT MCP tools | Token wallet master ledger, CPS rebate accounting, ecommerce adapters |

Service-to-service calls use shared OpenAPI headers:

`X-App-Id`, `X-Tenant-Id`, `X-Timestamp`, `X-Nonce`, `X-Signature`, `X-Idempotency-Key`.

## Money And Attribution Rules

- Any money or Token mutation must be idempotent and auditable.
- Evidence fields: source system, source order id, tenant id, user/member id, idempotency key, status, failure reason, timestamps.
- Never trust request-body `memberId` or `userId` for user-facing member assets. Use login context or verified service signatures.
- Only `AVAILABLE` CPS rebate can be exchanged to Token.
- Exchange saga: create local exchange order -> freeze rebate -> call aitoken submit -> confirm deduct on success -> unfreeze on failure -> keep `PROCESSING` on timeout for compensation.
- Selection item snapshots, activity cards, and search results are for operations/recommendation only. They must not drive rebate settlement, order attribution, freeze/deduct, withdrawal, or Token exchange.
- Orders without `specialId`, `relationId`, or verified external mapping may be stored for reconciliation, but must not mutate member rebate assets automatically.

## Development Workflow

- Default to solo execution for scoped work.
- Use native subagents only for independent, bounded, verifiable subtasks that improve throughput without shared-file conflicts.
- Use TDD for feature/bugfix work when behavior changes: write the smallest failing test, prove it fails, implement, prove it passes, then broaden verification.
- CPS P0/P1 paths require tests before implementation: rebate freeze/deduct, exchange saga, order transitions, tenant isolation, OpenAPI signatures, MCP member attribution, MCP audit logs, and platform adapter failures.
- Frontend issue fixes should start with deterministic Playwright reproduction in `frontend/admin-vue3/e2e` when practical.
- Midscene.js may assist visual/semantic E2E for admin-vue3, but final acceptance still needs Playwright `expect` assertions or backend tests.
- Do not commit model credentials. Midscene uses env vars such as `MIDSCENE_MODEL_BASE_URL`, `MIDSCENE_MODEL_API_KEY`, `MIDSCENE_MODEL_NAME`, and `MIDSCENE_MODEL_FAMILY`.

Verification before completion:

- Small changes: targeted tests/checks.
- Standard changes: targeted tests plus lint/typecheck/build where relevant.
- Large, financial, security, or architectural changes: thorough module tests and risk-focused regression checks.
- If validation cannot run, report the reason and the next-best evidence.

## Common Commands

Backend:

```bash
cd backend
mvn clean compile
mvn test
mvn test -Dtest=ClassName
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsRebateTokenExchangeServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"
mvn spring-boot:run -pl qiji-server -Dspring-boot.run.profiles=local
```

Frontend admin:

```bash
cd frontend/admin-vue3
pnpm install
pnpm dev
pnpm build:prod
pnpm ts:check
pnpm lint:eslint
pnpm e2e
```

UniApp admin:

```bash
cd frontend/admin-uniapp
pnpm type-check
pnpm lint
pnpm build:prod
```

Docker:

```bash
cd backend/script/docker
docker-compose up -d
docker-compose logs -f server
docker-compose down
```

## CPS Implementation Anchors

- Platform adapter interface: `CpsPlatformClient`.
- Platform registration: `CpsPlatformClientFactory`.
- Rebate rate priority: member exact platform -> member all platforms -> level platform -> level all platforms -> platform default -> global default.
- Order status flow: placed -> paid -> received -> settled -> credited; refund/invalid branches after received.

MCP tool layer lives in `qiji-module-cps-biz/mcp/tool/` and exposes:

- `cps_search_goods`
- `cps_compare_prices`
- `cps_generate_link`
- `cps_query_orders`
- `cps_get_rebate_summary`
- `cps_recommend_by_scene`
- `cps_list_selection_themes`
- `cps_recommend_from_selection_theme`
- `cps_get_rebate_balance`
- `cps_create_token_exchange`
- `cps_query_exchange_status`

## Feature-Specific Docs

Read these only for matching tasks:

- Haodanku work: `haodanku-openapi-docs/AI使用说明.md`, `haodanku-openapi-docs/接口目录.md`, then the relevant categorized doc.
- Dataoke high-efficiency links, order sync, adzone management, or MCP/App attribution: `docs/dataoke-high-efficiency-link-attribution.md`.
- Dataoke search page, goods square, hot searches, suggestions, recall, or search-to-link flows: `docs/dataoke-search-page-implementation.md`.
- P0 rebate-to-Token exchange: `docs/agentic-ecosystem-p0-rebate-token-exchange.md`.
- CPS technical debt: `docs/cps-tech-debt-inventory.md`.

Current feature anchors:

- Activity center frontend: `frontend/admin-vue3/src/views/cps/activity/square/index.vue`.
- Activity center backend: `CpsRebateActivityController`, `CpsRebateActivityService`, table `cps_rebate_activity`.
- Rebate toolbox frontend: `frontend/admin-vue3/src/views/cps/toolbox/index.vue`.
- Rebate toolbox backend: `CpsGoodsToolboxService`; batch transfer delegates to `CpsGoodsRebateQueryService.queryRebate()`.
- Selection theme frontend: `frontend/admin-vue3/src/views/cps/selection/theme/index.vue`.
- Selection theme backend: `/admin-api/cps/selection-theme`, `CpsSelectionThemeService`, tables `cps_selection_theme` and `cps_selection_theme_item`.

## Database Rules

- Monetary amounts are stored in cents as integer values.
- Timestamps use Shanghai timezone.
- Soft delete uses `deleted`.
- Multi-tenancy uses `tenant_id`.
- CPS table names use `cps_*`.
- CPS SQL lives in `backend/sql/module/cps-all-in-one.sql` and `backend/sql/module/cps-update.sql`.
- Every update block in `cps-update.sql` must include modification date/time.
- New incremental SQL update records must be appended to the end of the corresponding `*-update.sql` file; do not insert them in the middle. Use the actual write date and time in the modification comment.
- Keep full baseline and incremental scripts synchronized.
- Do not add CPS SQL to `backend/sql/mysql/ruoyi-vue-pro.sql`.
- Do not create one-off/date-named CPS SQL scripts under `backend/sql/mysql`.
- Local development test records belong in `backend/sql/module/test_data.sql`; keep it local-only and excluded from Git.

## File Encoding Rules

This repo contains Chinese UTF-8 text. PowerShell text file IO has previously corrupted UTF-8 content.

- Do not use PowerShell `Get-Content` / `Set-Content` for files that may contain Chinese text.
- Prefer UTF-8 aware tooling. For Python read/write, use `encoding='utf-8'` and `newline=''`.
- After writing files that may contain Chinese text, verify UTF-8 decoding.
- Avoid broad, automated text rewrites unless the target paths and encodings are verified.

## Token Budget Rules

- Keep prompts and final reports concise.
- Do not paste full files, long logs, README, or project-map sections unless required.
- Use targeted search (`rg`) and narrow file reads.
- Load feature-specific docs only when the task matches that feature.
- Summarize large findings instead of carrying raw output forward.
