# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AgenticCPS is a **CPS (Cost Per Sale) Alliance Rebate System** built on ruoyi-vue-pro. It aggregates Taobao, JD.com, Pinduoduo, and Douyin affiliate platforms to provide rebate search, price comparison, order tracking, and settlement services. The system features AI Agent integration via MCP (Model Context Protocol).

**Key differentiator**: This project uses Vibe Coding + AI autonomous programming — CPS module code is 100% AI-generated.

## Architecture

```
AgenticCPS/
├── backend/                    # Spring Boot 3.x Java backend
│   ├── yudao-server/          # Main application entry point
│   ├── yudao-module-cps/       # CPS rebate core module (primary focus)
│   │   ├── yudao-module-cps-api/    # API definitions (enums, remote interfaces)
│   │   └── yudao-module-cps-biz/    # Business implementation
│   │       ├── controller/admin/    # Admin REST APIs
│   │       ├── controller/app/      # Member-facing REST APIs
│   │       ├── service/             # Business services
│   │       ├── client/              # CPS platform adapters (Strategy pattern)
│   │       │   ├── taobao/          # Taobao affiliate adapter
│   │       │   ├── jingdong/        # JD.com affiliate adapter
│   │       │   ├── pinduoduo/       # Pinduoduo affiliate adapter
│   │       │   └── douyin/          # Douyin affiliate adapter
│   │       ├── dal/                  # Data access layer (MyBatis Plus)
│   │       ├── job/                  # Scheduled jobs (Quartz)
│   │       └── mcp/                  # MCP AI interface layer
│   ├── yudao-module-ai/      # AI module (Spring AI + MCP support)
│   ├── yudao-module-member/  # Member management
│   ├── yudao-module-pay/     # Payment/wallet system
│   ├── yudao-module-mall/    # E-commerce module
│   ├── yudao-module-system/  # System management (auth, perms, menus)
│   ├── yudao-module-infra/   # Infrastructure (Redis, file storage, MQ)
│   ├── yudao-framework/       # Framework extensions
│   └── sql/                   # Database schema scripts for each module
│
├── frontend/
│   ├── admin-vue3/           # Vue3 admin panel (Element Plus + TypeScript)
│   ├── admin-uniapp/         # uni-app mobile admin
│   └── mall-uniapp/          # E-commerce mobile app
│
├── script/                    # Build and deployment scripts
│
├── references/                # Agent reference documents, literature & standards
│
├── releases/                  # Version release packages
│
├── agent_improvement/         # AI agent improvement & rules
│   └── memory/               # Code generation rules and Claude memory
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
| ORM | MyBatis Plus 3.5.12 |
| Cache | Redis 7.0, Redisson 3.35.0 |
| Database | MySQL 5.7/8.0+ |
| Frontend | Vue 3.5.12, Element Plus 2.11.1, TypeScript 5.3.3 |
| Build Tool | Maven 3.8+, pnpm 8.6+ |
| Mobile | UniApp |
| AI | Spring AI + MCP Protocol |

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

To add a new platform (e.g., Weibo), implement this interface and register as a Spring Bean. No core logic changes required.

### MCP AI Interface Layer

Located in `yudao-module-cps-biz/mcp/`:
- **Tools**: AI-callable functions (search, compare, generate link, query orders)
- **Resources**: Read-only data sources (platform configs, rebate rules, statistics)
- **Prompts**: Pre-defined interaction templates

MCP uses JSON-RPC 2.0 over Streamable HTTP at endpoint `/mcp/cps`.

### Rebate Calculation Priority

System resolves rebate rate in this order:
1. Member personal config (exact platform) → 2. Member personal config (all platforms) → 3. Level + Platform → 4. Level (all platforms) → 5. Platform default → 6. Global default

### Order Status Flow

```
已下单 → 已付款 → 已收货 → 已结算 → 已到账
                  ↓
              已退款 / 已失效
```

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
| Docker | environment variables | 48080 |

Key configs in `application-local.yaml`:
- `spring.datasource.dynamic` — MySQL connection
- `spring.data.redis` — Redis connection
- `yudao.cps.mcp.*` — MCP server settings
- CPS platform API keys (淘宝/京东/拼多多/抖音)

## Important Notes

- **Integer for money**: Never use Double/BigDecimal for monetary amounts — always use Integer (cents) to avoid floating-point errors
- **Timezone**: System configured for Asia/Shanghai; ensure database and JVM match
- **Multi-tenancy**: All CPS queries must include tenant isolation
- **Soft delete**: Use MyBatis Plus `deleted` column, never hard delete CPS data
- **Password**: Default admin password is `admin` (change in production)
