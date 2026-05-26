# Rebate Toolbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a unified CPS rebate toolbox that combines parsing, rebate estimation, batch transfer links, product square selection, and promotion copy editing.

**Architecture:** Keep existing CPS goods and square services as the source of truth. Add a thin backend toolbox layer for parse-only and batch operations, then add a Vue toolbox page that reuses existing API clients and interaction patterns.

**Tech Stack:** Spring Boot 3.5.9, Java 17/21, MyBatis Plus, Vue 3.5, Element Plus, TypeScript, pnpm, Maven, Mockito.

---

## File Structure

- Create: `frontend/admin-vue3/src/views/cps/toolbox/index.vue`  
  Owns the toolbox layout, grouped tool navigation, current tool state, shared member/platform/vendor/adzone state, and result sidebar.
- Create: `frontend/admin-vue3/src/views/cps/toolbox/components/UniversalTransferPanel.vue`  
  Owns single/batch transfer input, output-format options, batch result table, and copy actions.
- Create: `frontend/admin-vue3/src/views/cps/toolbox/components/ParsePanel.vue`  
  Owns parse-only workflow and “send to transfer” action.
- Create: `frontend/admin-vue3/src/views/cps/toolbox/components/GoodsSquarePanel.vue`  
  Wraps or extracts the current goods square workflow so selected goods can be sent to the transfer panel.
- Create: `frontend/admin-vue3/src/views/cps/toolbox/components/PromotionResultEditor.vue`  
  Owns editable promotion content and copy-all behavior.
- Create: `frontend/admin-vue3/src/api/cps/rebateToolbox.ts`  
  Adds parse-only and batch transfer API types.
- Modify: `frontend/admin-vue3/src/api/cps/goodsTool.ts`  
  Keep existing single query API; optionally export shared result types to avoid duplication.
- Modify: `frontend/admin-vue3/src/views/cps/goods/rebate-query/index.vue`  
  Either redirect users to the toolbox or keep as a compatibility page with a link to the toolbox.
- Modify: `frontend/admin-vue3/src/views/cps/goods/square/index.vue`  
  Extract reusable logic only if needed; otherwise keep old route and embed a dedicated toolbox wrapper.
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/CpsGoodsRebateQueryController.java`  
  Add parse-only and batch transfer endpoints, or delegate to a new toolbox controller if the file becomes too broad.
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/vo/CpsGoodsParseReqVO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/vo/CpsGoodsParseRespVO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/vo/CpsGoodsBatchTransferReqVO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/vo/CpsGoodsBatchTransferRespVO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsToolboxService.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsToolboxServiceImpl.java`
- Test: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/goods/CpsGoodsToolboxServiceImplTest.java`
- Modify: `backend/sql/mysql/cps-all-in-one.sql`  
  Add or adjust CPS menu records for “返利工具箱” and permissions. Keep CPS SQL out of `ruoyi-vue-pro.sql` and use UTF-8 safe editing.
- Optional docs: update `docs/project-map.md` only if routes or module ownership change materially.

## Task 1: Lock Current Behavior

**Files:**
- Test: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/goods/CpsGoodsRebateQueryServiceImplTest.java`
- Test: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/goods/CpsGoodsSquareServiceImplTest.java`

- [ ] **Step 1: Inspect existing tests**

Run: `rg -n "CpsGoodsRebateQuery|CpsGoodsSquare|queryRebate|generateLink" backend/qiji-module-cps/qiji-module-cps-biz/src/test`

- [ ] **Step 2: Add or confirm regression cases**

Cover these behaviors:

- Existing single rebate query returns `SUCCESS`, goods info, rebate info, links, and transfer record ID.
- Existing single rebate query returns `PARSE_FAILED` without inserting a transfer record.
- Existing goods square link inserts one transfer record and returns promotion content.

- [ ] **Step 3: Run targeted tests**

Run: `cd backend && mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsGoodsRebateQueryServiceImplTest,CpsGoodsSquareServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: PASS before new toolbox changes, or only fail for missing test fixtures that the next step explicitly fixes.

## Task 2: Add Backend Toolbox Service

**Files:**
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsToolboxService.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsToolboxServiceImpl.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/vo/CpsGoodsParseReqVO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/vo/CpsGoodsParseRespVO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/vo/CpsGoodsBatchTransferReqVO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/vo/CpsGoodsBatchTransferRespVO.java`
- Test: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/goods/CpsGoodsToolboxServiceImplTest.java`

- [ ] **Step 1: Write failing tests for batch transfer**

Test cases:

- Batch with two valid lines calls the single transfer flow twice and preserves input order.
- Batch with one valid and one invalid line returns one success and one failure.
- Batch with more than 20 lines fails validation.
- Blank lines are ignored or rejected consistently; choose one behavior and document it in the test.

- [ ] **Step 2: Run the new service test and confirm failure**

Run: `cd backend && mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsGoodsToolboxServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: FAIL because service and VO classes do not exist yet.

- [ ] **Step 3: Implement service interfaces and VOs**

Implementation guidance:

- Reuse `CpsGoodsRebateQueryService.queryRebate()` for batch transfer.
- Keep batch item response shape close to `CpsGoodsRebateQueryRespVO`.
- Include `inputIndex`, `originalContent`, `status`, and `message` on every item.
- Do not stop the batch after one item fails.

- [ ] **Step 4: Add parse-only implementation**

Implementation guidance:

- Reuse `CpsContentParser.parse()` first.
- Fall back to platform `parseContent()` when local parsing is unsupported.
- Do not generate links or insert transfer records in parse-only mode.

- [ ] **Step 5: Run targeted backend tests**

Run: `cd backend && mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsGoodsToolboxServiceImplTest,CpsGoodsRebateQueryServiceImplTest,CpsGoodsSquareServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: PASS.

## Task 3: Add Backend Controller Endpoints

**Files:**
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/CpsGoodsRebateQueryController.java`
- Test: add controller or service-level validation coverage under `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/controller/admin/goods/`

- [ ] **Step 1: Add endpoint tests or validation tests**

Cover:

- `POST /cps/goods/parse` requires original content.
- `POST /cps/goods/batch-transfer` requires member ID and 1-20 content lines.
- Permissions are documented as `cps:toolbox:query` and `cps:toolbox:link`.

- [ ] **Step 2: Implement endpoints**

Suggested endpoints:

- `POST /cps/goods/parse`
- `POST /cps/goods/batch-transfer`

Keep existing `POST /cps/goods/rebate-query` unchanged for compatibility.

- [ ] **Step 3: Run targeted backend tests**

Run: `cd backend && mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsGoodsToolboxServiceImplTest "*ControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: PASS. If no controller test harness exists, state the gap and rely on service validation plus later API smoke testing.

## Task 4: Add Frontend API Types

**Files:**
- Create: `frontend/admin-vue3/src/api/cps/rebateToolbox.ts`
- Modify: `frontend/admin-vue3/src/api/cps/goodsTool.ts`

- [ ] **Step 1: Define TypeScript interfaces**

Include:

- `CpsGoodsParseReqVO`
- `CpsGoodsParseRespVO`
- `CpsGoodsBatchTransferReqVO`
- `CpsGoodsBatchTransferRespVO`
- `CpsGoodsBatchTransferItemVO`

- [ ] **Step 2: Add API methods**

Add:

- `parseContent(data)`
- `batchTransfer(data)`

- [ ] **Step 3: Run TypeScript check**

Run: `cd frontend/admin-vue3 && pnpm ts:check`

Expected: PASS, or fail only on known pre-existing unrelated workspace issues that must be recorded.

## Task 5: Build Toolbox Shell

**Files:**
- Create: `frontend/admin-vue3/src/views/cps/toolbox/index.vue`
- Create: `frontend/admin-vue3/src/views/cps/toolbox/components/PromotionResultEditor.vue`

- [ ] **Step 1: Create static shell**

Build:

- Page title “返利工具箱”。
- Left grouped tool nav: 基础工具、选品工具、创作工具、玩法工具。
- Top alert/status band for authorization, default supplier, default adzone, and batch limit.
- Main panel placeholder.
- Right result editor placeholder.

- [ ] **Step 2: Wire active tool state**

Tools:

- `universal-transfer`
- `parse`
- `goods-square`
- `copy-editor`
- enabled P1/P2 tools: `ownership-check`, `coupon-query`, `cash-gift`

- [ ] **Step 3: Verify layout responsiveness**

Run the dev server and inspect desktop and mobile widths:

Run: `cd frontend/admin-vue3 && pnpm dev`

Use Browser or Playwright to check:

- 1440px desktop: left nav, main panel, result panel visible without overlap.
- 390px mobile: nav collapses or stacks; no text overlap.

## Task 6: Implement Universal Transfer Panel

**Files:**
- Create: `frontend/admin-vue3/src/views/cps/toolbox/components/UniversalTransferPanel.vue`
- Modify: `frontend/admin-vue3/src/views/cps/toolbox/index.vue`

- [ ] **Step 1: Add form fields**

Fields:

- Platform auto/manual select.
- Member remote select.
- API vendor select.
- Adzone select.
- Multi-line original content textarea.
- Output format checkboxes.

Reuse option loading patterns from `frontend/admin-vue3/src/views/cps/goods/rebate-query/index.vue`.

- [ ] **Step 2: Implement single and batch submit**

Behavior:

- One nonblank line can call existing single query or batch endpoint.
- Multiple nonblank lines call batch endpoint.
- Trim to max 20 lines and show validation error if exceeded.

- [ ] **Step 3: Render result table**

Columns:

- Index, status, platform, goods ID, title, commission, rebate, adzone, vendor, record ID, actions.

Actions:

- Copy short link.
- Copy淘口令。
- Copy promotion content.
- Send to editor.

- [ ] **Step 4: Run frontend typecheck**

Run: `cd frontend/admin-vue3 && pnpm ts:check`

Expected: PASS.

## Task 7: Implement Parse Panel

**Files:**
- Create: `frontend/admin-vue3/src/views/cps/toolbox/components/ParsePanel.vue`
- Modify: `frontend/admin-vue3/src/views/cps/toolbox/index.vue`

- [ ] **Step 1: Add parse form**

Fields:

- Platform auto/manual select.
- Original content textarea.

- [ ] **Step 2: Render parse result**

Show:

- Platform, goods ID, goodsSign, title, item link, parse source, failure reason.

- [ ] **Step 3: Add “带入转链”**

Emit parsed content to `UniversalTransferPanel` with the original content and inferred platform.

- [ ] **Step 4: Run frontend typecheck**

Run: `cd frontend/admin-vue3 && pnpm ts:check`

Expected: PASS.

## Task 8: Integrate Goods Square

**Files:**
- Create: `frontend/admin-vue3/src/views/cps/toolbox/components/GoodsSquarePanel.vue`
- Modify: `frontend/admin-vue3/src/views/cps/goods/square/index.vue` if extraction is needed.
- Modify: `frontend/admin-vue3/src/views/cps/toolbox/index.vue`

- [ ] **Step 1: Choose reuse strategy**

Prefer a thin wrapper around current goods square logic. Extract only when direct reuse would duplicate too much code.

- [ ] **Step 2: Add “带入转链” event**

Selected goods should populate transfer input with title, goods ID, goodsSign, platform, and vendor.

- [ ] **Step 3: Preserve old route**

Keep `/cps/goods/square` working. Add a link from old page to the toolbox if useful.

- [ ] **Step 4: Run frontend typecheck**

Run: `cd frontend/admin-vue3 && pnpm ts:check`

Expected: PASS.

## Task 9: Menu and Permissions

**Files:**
- Modify: `backend/sql/mysql/cps-all-in-one.sql`
- Optional modify: generated or seed SQL for other databases only if this repo currently keeps CPS menu rows there.

- [ ] **Step 1: Find existing CPS menu rows**

Run: `rg -n "商品返利查询|返利商品广场|cps:goods-rebate-query|cps:goods-square|返利工具" backend/sql`

- [ ] **Step 2: Add toolbox menu**

Add or update:

- Menu name: `返利工具箱`
- Component path: `cps/toolbox/index`
- Permission: `cps:toolbox:query`
- Child/button permissions: `cps:toolbox:link`

Use UTF-8 safe editing and verify the SQL still decodes as UTF-8.
Do not add CPS table, seed, menu, or permission rows to `backend/sql/mysql/ruoyi-vue-pro.sql`.

- [ ] **Step 3: Keep old permissions compatible**

Do not remove existing `cps:goods-rebate-query:query` or `cps:goods-square:*` permissions in P0 unless a migration explicitly handles existing roles.

## Task 10: End-to-End Verification

**Files:**
- Optional create: `frontend/admin-vue3/e2e/cps/rebate-toolbox.spec.ts`

- [ ] **Step 1: Backend targeted tests**

Run: `cd backend && mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsGoodsToolboxServiceImplTest,CpsGoodsRebateQueryServiceImplTest,CpsGoodsSquareServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: PASS.

- [ ] **Step 2: Frontend typecheck**

Run: `cd frontend/admin-vue3 && pnpm ts:check`

Expected: PASS.

- [ ] **Step 3: Frontend lint**

Run: `cd frontend/admin-vue3 && pnpm lint:eslint`

Expected: PASS or documented pre-existing lint issues.

- [ ] **Step 4: Browser smoke test**

Start frontend:

Run: `cd frontend/admin-vue3 && pnpm dev`

Smoke test:

- Open toolbox route.
- Switch between tools.
- Enter one sample content and validate form behavior.
- Enter more than 20 lines and confirm validation.
- Check desktop and mobile screenshots for overlap.

- [ ] **Step 5: Final regression check**

Confirm:

- Existing商品返利查询 still works.
- Existing返利商品广场 still works.
- Existing转链记录 table still receives records from successful transfer operations.

