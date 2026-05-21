# CPS AI Coding Rules

> Always-level rules for `backend/qiji-module-cps`. These rules are written for AI agents before they modify CPS code, tests, SQL, docs, or frontend pages that touch CPS behavior.

## Scope

- Current CPS module path: `backend/qiji-module-cps`.
- Current CPS Java package: `com.qiji.cps.module.cps`.
- Current backend app module: `backend/qiji-server`.
- Treat old names such as `yudao-module-cps`, `yudao-server`, or `cn.iocoder.yudao.module.cps` as historical unless verified against the filesystem.
- Before changing CPS code, read `docs/project-map.md`, `docs/cps-tech-debt-inventory.md`, and this file.

## Encoding And Path Rules

- Read and write Chinese text files with explicit UTF-8.
- After editing rules/docs, verify UTF-8 decoding for every changed Markdown file.
- Do not rely on PowerShell `Get-Content` / `Set-Content` for Chinese files.
- If a document appears garbled, stop using that rendering as source of truth and re-read the bytes as UTF-8 before making decisions.

## CPS Ownership Boundaries

- AgenticCPS owns CPS platform adapters, goods search, price comparison, promotion links, order tracking, rebate settlement, rebate freeze/deduct, and CPS MCP tools.
- AgenticCPS must not create a parallel AI Token ledger or own model gateway billing.
- Cross-system money or Token mutations must be idempotent and auditable.
- Service-to-service calls must keep the shared OpenAPI headers: `X-App-Id`, `X-Tenant-Id`, `X-Timestamp`, `X-Nonce`, `X-Signature`, `X-Idempotency-Key`.

## Layer Rules

- Controller only validates request shape, permissions, login context, and calls Service.
- Service owns orchestration, idempotency, status transitions, transactions, and cross-module calls.
- Client owns external platform protocol, signing, request execution, response parsing, platform field mapping, and vendor error mapping.
- Mapper owns persistence and query shape only.
- MCP Tool owns AI-facing schema, trusted member context, parameter normalization, audit logging, and stable error response.
- Platform-specific differences must stay under `client/*`; they must not leak into Controller, MCP Tool, or generic Service code.

## Money Rules

- New CPS money fields must use `Integer` cents.
- Do not add new `Double` money fields.
- Do not add new business-model `BigDecimal` money fields unless preserving an existing schema/API boundary and documenting the unit.
- Third-party API adapters may parse decimals at the boundary, but Service/DO/VO changes must state whether the value is cents, yuan, basis points, or percent.
- Any migration from existing `BigDecimal` yuan fields must be planned with compatibility tests; do not do opportunistic schema-wide money rewrites.

## Query Rules

- CPS queries must preserve tenant isolation, soft delete, and user/member ownership.
- New custom SQL must explicitly include `tenant_id` and `deleted` unless an existing framework interceptor is proven to apply.
- Large-table queries must avoid wrapping indexed time columns with functions such as `DATE(create_time)`.
- Pagination queries must have deterministic order and bounded page size.
- App/MCP member-facing queries must derive member identity from login context or trusted ToolContext, not request-body `memberId`.
- Admin queries that accept `memberId` must remain admin-only and permission-protected.

## Order Rules

- Order identity must include platform order id, platform code, and tenant. Do not assume `platform_order_id` alone is globally unique.
- Order status changes must go through a unified Service/state-machine method.
- Do not scatter direct `setStatus` / `setOrderStatus` mutations across jobs, controllers, or tools.
- Status transitions must guard terminal states and out-of-order platform updates.
- Refund/invalid transitions after rebate credit must trigger a verified rebate reversal path or explicitly record a manual-processing state.
- Order sync must treat platform/API failure differently from an empty order result.
- Batch sync must record enough evidence to diagnose platform, time window, counts, skipped rows, and failure reason.

## Rebate And Freeze Rules

- Rebate priority must be consistent across every entry point:
  member personal platform -> member personal all-platform -> level platform -> level all-platform -> platform default -> global default.
- Only `AVAILABLE` rebate may be exchanged to Token.
- Freeze, unfreeze, deduct, refund, and reversal must update both the account ledger and audit record in the same transactional boundary.
- Freeze records cannot say `UNFREEZED` while account `frozen_balance` still contains the same amount.
- Rebate settlement must be idempotent with a database-level uniqueness or compare-and-set guard, not only a pre-insert query.
- Account balance updates must be atomic and tested under duplicate submission or concurrent settlement.

## Platform Client Rules

- New platforms and vendors must implement `CpsPlatformClient` or `CpsApiVendorClient` consistently and register as Spring beans.
- Unsupported vendor capabilities must fail explicitly or be blocked by capability checks; they must not silently return empty search/order results or null links.
- Active vendor lookup must respect vendor status and platform status.
- Vendor-specific credential meanings must be documented or expressed through validated `extraConfig`, not hidden overloads of generic fields.
- Every new client needs at least one test stub for connection, search, link generation, and order query behavior.

## MCP Tool Rules

- Every MCP Tool must validate required parameters, page size, platform allowlist, and value ranges.
- Every MCP Tool must record tool name, parameter summary, member context when available, duration, status, and failure reason.
- MCP Tool responses must have a stable error shape. Do not leak raw exception details that expose secrets or internal stack details.
- MCP Tools that touch member assets must use trusted ToolContext/login context. Request `memberId` may be used only as an admin/service-signed input with explicit verification.
- MCP Tool implementations should call Service methods, not Mapper or platform Client directly.

## Frontend Rules

- CPS CRUD pages should follow the existing Vue3/Element Plus codegen structure.
- Do not create a one-off frontend style for CPS admin pages.
- Any page that changes money, order status, freeze, rebate, platform config, or MCP API key behavior must show clear state, error, and confirmation flows.

## Required Pre-Change Checklist

1. Confirm actual path uses `qiji-*`, not stale `yudao-*`.
2. Identify whether the change touches money, order, rebate, freeze, platform adapter, Mapper, MCP, or frontend CRUD.
3. Check `docs/cps-tech-debt-inventory.md` for matching debt IDs.
4. Decide which debt can be absorbed by the current business change and which must be explicitly deferred.
5. Choose tests before editing: unit, DB, Redis, Maven module test, Playwright, or documentation UTF-8/path validation.

## Required Pre-PR Checklist

1. Change goal and affected CPS entry points are listed.
2. Tenant, permission, soft delete, and member identity boundaries are checked.
3. Money units are checked.
4. Idempotency, concurrency, retry, and refund/invalid paths are checked.
5. Platform adapter failures and unsupported capabilities are checked.
6. MCP audit logging and stable error shape are checked if MCP changed.
7. Tests and command output are recorded.
8. Remaining business risks and manual review points are listed.
