# AGENTS.md

This file provides guidance to AI Agents (Qoder, Claude Code, etc.) when working with code in this repository.

## Project Overview

AgenticCPS is a **CPS (Cost Per Sale) Alliance Rebate System** built on ruoyi-vue-pro. It aggregates Taobao, JD.com, Pinduoduo, and Douyin affiliate platforms to provide rebate search, price comparison, order tracking, and settlement services. The system features AI Agent integration via MCP (Model Context Protocol).

**Key differentiator**: This project uses Vibe Coding + AI autonomous programming — CPS module code is 100% AI-generated (20,000+ lines of code including business services, scheduled jobs, MCP interface layer, and unit tests).

## Architecture

```
AgenticCPS/
├── backend/                    # Spring Boot 3.5.9 Java backend
│   ├── yudao-server/          # Main application entry point (port 48080)
│   ├── yudao-module-cps/       # CPS rebate core module (primary focus)
│   │   ├── yudao-module-cps-api/    # API definitions (enums, remote interfaces)
│   │   └── yudao-module-cps-biz/    # Business implementation
│   │       ├── controller/admin/    # Admin REST APIs (15 controllers)
│   │       ├── controller/app/      # Member-facing REST APIs (13 controllers)
│   │       ├── service/             # Business services (7 service modules)
│   │       ├── client/              # CPS platform adapters (Strategy pattern)
│   │       │   ├── taobao/          # Taobao affiliate adapter
│   │       │   ├── jingdong/        # JD.com affiliate adapter
│   │       │   ├── pinduoduo/       # Pinduoduo affiliate adapter
│   │       │   └── douyin/          # Douyin affiliate adapter
│   │       ├── dal/                  # Data access layer (MyBatis Plus, 9 core tables)
│   │       ├── job/                  # Scheduled jobs (Quartz - order sync, status update)
│   │       └── mcp/                  # MCP AI interface layer
│   │           └── tool/             # 5 MCP tool functions
│   ├── yudao-module-ai/      # AI module (Spring AI 1.1.2 + MCP support)
│   ├── yudao-module-member/  # Member management
│   ├── yudao-module-pay/     # Payment/wallet system
│   ├── yudao-module-mall/    # E-commerce module
│   ├── yudao-module-system/  # System management (auth, perms, menus)
│   ├── yudao-module-infra/   # Infrastructure (Redis, file storage, MQ)
│   ├── yudao-module-report/  # Report & dashboard module
│   ├── yudao-module-mp/      # WeChat Official Account module
│   ├── yudao-framework/       # Framework extensions (Web, Security, MyBatis, Redis, Job, Tenant, Data Permission, MQ, Monitor, Excel)
│   ├── yudao-dependencies/   # Centralized dependency version management
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
mvn spring-boot:run -pl yudao-server -Dspring-boot.run.profiles=local

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

### MCP AI Interface Layer

Located in `yudao-module-cps-biz/mcp/tool/`, 5 tool functions registered via Spring AI:

| Tool Class | Tool Name | Description |
|-----------|-----------|-------------|
| `CpsSearchGoodsToolFunction` | `cps_search_goods` | Search goods across platforms with keyword, platform filter, price range, pagination |
| `CpsComparePricesToolFunction` | `cps_compare_prices` | Cross-platform price comparison, returns cheapest/highest-rebate/best-overall |
| `CpsGenerateLinkToolFunction` | `cps_generate_link` | Generate promotion links with rebate tracking (short/long/token/mobile) |
| `CpsQueryOrdersToolFunction` | `cps_query_orders` | Query member orders and rebate status |
| `CpsGetRebateSummaryToolFunction` | `cps_get_rebate_summary` | Query rebate account: balance, pending, total, recent records |

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
- CPS module tables: `cps_*` prefix

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
