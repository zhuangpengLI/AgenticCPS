# AI Chat MCP Member Context Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use `executing-plans` to implement this plan task-by-task. For every behavior change, follow `test-driven-development`: write the smallest failing test, run it and confirm the expected failure, implement the minimum production change, then rerun the focused test.

**Goal:** Add member-owned AI chat APIs and a secure administrator MCP test mode that can bind a current-tenant member, exercise the real self MCP Client -> Server path, default to read-only tools, and audit the effective member and actor.

**Architecture:** Keep standard member conversations on the existing mixed tool path: local CPS callbacks execute directly and configured external MCP clients remain role-driven. Add an immutable conversation identity model. A lazy self-test MCP client signs a short-lived identity envelope into MCP `_meta`; the CPS MCP server verifies HMAC, TTL, audience and one-time nonce before rebuilding trusted tool context and applying a server-side tool-risk policy. Generic signed-identity DTOs live in `qiji-common` so AI and CPS do not create a circular dependency.

**Tech Stack:** Java 17, Spring Boot 3.5.9, Spring AI 1.1.2 MCP, MyBatis Plus, Redis, JUnit 5/Mockito/H2, Vue 3 + Element Plus + TypeScript, Playwright, MySQL migration SQL.

**Design reference:** `docs/superpowers/specs/2026-07-10-ai-chat-mcp-member-context-design.md`

## Delivery rules

- Preserve unrelated dirty worktree changes and stage only files belonging to the current task.
- Do not trust request-body `memberId` for member-facing APIs.
- Keep `chat_mode` on conversations and `invocation_source` on individual tool/audit records.
- Never log the signed envelope, API Key, raw nonce, or full sensitive tool arguments.
- A self-MCP failure must fail closed; do not fall back to local callbacks.
- Do not add a member UI in `mall-uniapp`.
- Run focused tests after every task and the consolidated verification suite at the end.

---

### Task 1: Add persistent identity and role exposure fields

**Files:**

- Create: `backend/sql/module/ai-2026-07-10.sql`
- Modify: `backend/sql/module/cps-all-in-one.sql`
- Modify: `backend/sql/module/cps-update.sql`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/dal/dataobject/chat/AiChatConversationDO.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/dal/dataobject/model/AiChatRoleDO.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/dal/mysql/chat/AiChatConversationMapper.java`
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/mcp/CpsMcpAccessLogDO.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/dal/mysql/chat/AiChatConversationMapperTest.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/dal/mysql/mcp/CpsMcpAccessLogMapperTest.java`

**Step 1: Write failing mapper tests**

Add an H2 mapper test that inserts and queries a conversation by tenant, `ownerUserType`, `userId` and `chatMode`. Add a CPS mapper test that persists `memberId`, actor fields, conversation, client, invocation source and trace ID.

**Step 2: Run tests and confirm RED**

```bash
cd backend
mvn test -pl qiji-module-ai -Dtest=AiChatConversationMapperTest
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsMcpAccessLogMapperTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: compilation or mapping failures because the new fields/query do not exist.

**Step 3: Add schema and DO fields**

Add conversation fields `owner_user_type`, `member_id`, `chat_mode`, `mcp_client_name`, `allow_mutation`, and `identity_bound_time`; add `member_enabled` to `ai_chat_role`. Backfill historical conversations to `ADMIN` / `STANDARD`. Add a tenant-owner lookup index.

Extend `cps_mcp_access_log` with `member_id`, `actor_user_id`, `actor_user_type`, `conversation_id`, `mcp_client_name`, `invocation_source`, and `trace_id`. Add the same columns to the full CPS baseline and a dated 2026-07-10 incremental block.

**Step 4: Implement mapper query and rerun GREEN**

Add a mapper method that includes owner type and owner ID instead of querying `user_id` alone. Rerun both tests and `git diff --check`.

**Step 5: Commit**

Use a Lore-format commit with intent “make conversation ownership and MCP audit identity explicit”.

---

### Task 2: Add enums, error contracts, role member exposure, and model tool capability

**Files:**

- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/enums/chat/AiChatOwnerTypeEnum.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/enums/chat/AiChatModeEnum.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/enums/chat/AiToolInvocationSourceEnum.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/enums/ErrorCodeConstants.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/dal/dataobject/model/AiChatRoleDO.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/admin/model/vo/chatRole/AiChatRoleSaveReqVO.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/admin/model/vo/chatRole/AiChatRoleRespVO.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/model/AiChatRoleService.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/model/AiChatRoleServiceImpl.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/util/AiUtils.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/service/model/AiChatRoleServiceImplTest.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/util/AiUtilsToolCapabilityTest.java`

**Step 1: Write failing tests**

Cover these rules:

- `getMemberEnabledRoleList()` returns only enabled, public, `memberEnabled=true` roles.
- a member cannot create a conversation with a private/admin-only role.
- `AiUtils.supportsToolCalling(platform)` is explicit and the YiYan/QianFan branch does not silently omit configured tools.

**Step 2: Confirm RED**

```bash
cd backend
mvn test -pl qiji-module-ai -Dtest=AiChatRoleServiceImplTest,AiUtilsToolCapabilityTest
```

**Step 3: Implement minimum contracts**

Add enum values and error codes for member-role denial, immutable identity, unsupported tool model, self-MCP unavailable, invalid MCP identity and mutation denial. Add `memberEnabled` to admin role save/response objects and role filtering service. Centralize model tool capability in `AiUtils` or a small `AiModelToolCapabilityService`; never infer support from whether callbacks happened to be attached.

**Step 4: Rerun GREEN and commit**

Run the focused tests, then commit only this task.

---

### Task 3: Introduce immutable conversation identity and ownership checks

**Files:**

- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/chat/AiChatConversationService.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/chat/AiChatConversationServiceImpl.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/dal/mysql/chat/AiChatConversationMapper.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/admin/chat/vo/conversation/AiChatConversationRespVO.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/service/chat/AiChatConversationServiceImplTest.java`

**Step 1: Write failing service tests**

Test:

- standard admin conversation writes `ADMIN`, owner admin ID, `STANDARD`.
- member conversation writes `MEMBER`, owner/member current login ID, `STANDARD`.
- owner lookup requires both owner type and owner ID.
- update cannot change member, owner type, mode, client or mutation policy.
- historical admin ownership remains readable.

**Step 2: Confirm RED**

```bash
cd backend
mvn test -pl qiji-module-ai -Dtest=AiChatConversationServiceImplTest
```

**Step 3: Implement identity-aware service methods**

Keep the existing admin methods for compatibility but route them through explicit owner type. Add `createMemberConversation`, `getOwnedConversation`, and identity-safe list/update/delete methods. Copy only mutable fields from update DTOs instead of using unrestricted bean copying.

**Step 4: Rerun GREEN and commit**

---

### Task 4: Add member-facing AI conversation and message APIs

**Files:**

- Modify: `backend/qiji-module-ai/pom.xml`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/app/chat/AppAiChatConversationController.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/app/chat/AppAiChatMessageController.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/app/chat/vo/conversation/AppAiChatConversationCreateReqVO.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/app/chat/vo/conversation/AppAiChatConversationUpdateReqVO.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/app/chat/vo/conversation/AppAiChatConversationRespVO.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/app/chat/vo/message/AppAiChatMessageSendReqVO.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/app/chat/vo/message/AppAiChatMessageRespVO.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/controller/app/chat/AppAiChatConversationControllerTest.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/controller/app/chat/AppAiChatMessageControllerTest.java`

**Step 1: Add failing controller tests**

Verify exact `/app-api/ai/chat/...` routes, authenticated member ID propagation, role filtering, stream/non-stream send, and cross-owner denial. Assert that member request DTOs have no `memberId`, `userId`, `chatMode`, `mcpClientName`, or `allowMutation` properties.

**Step 2: Confirm RED**

```bash
cd backend
mvn test -pl qiji-module-ai -Dtest=AppAiChatConversationControllerTest,AppAiChatMessageControllerTest
```

**Step 3: Implement App APIs**

Add the approved create/list/get/update/delete, send/send-stream/message-list and role/simple-list endpoints. Use `getLoginUserId()` as the only effective member source. Add the member module dependency only for its API contract if required; do not call member implementation classes.

**Step 4: Rerun GREEN and commit**

---

### Task 5: Build trusted local ToolContext and refactor tool callback assembly

**Files:**

- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/chat/AiChatIdentityContextService.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/chat/AiChatToolCallbackService.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/chat/AiChatMessageServiceImpl.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/util/AiUtils.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/tool/function/UserProfileQueryToolFunction.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/service/chat/AiChatIdentityContextServiceTest.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/service/chat/AiChatToolCallbackServiceTest.java`

**Step 1: Write failing tests**

Assert exact context keys: `LOGIN_USER_ID`, `TENANT_ID`, `ACTOR_USER_ID`, `ACTOR_USER_TYPE`, `CONVERSATION_ID`, `CHAT_MODE`, and `TRACE_ID`. Assert standard conversations can mix local direct and external MCP callbacks, while external MCP receives no internal identity metadata by default.

**Step 2: Confirm RED**

```bash
cd backend
mvn test -pl qiji-module-ai -Dtest=AiChatIdentityContextServiceTest,AiChatToolCallbackServiceTest
```

**Step 3: Implement and refactor**

Move `getToolCallbackListByRoleId` out of `AiChatMessageServiceImpl`. Build context from the persisted conversation identity, not directly from arbitrary request fields. Keep the legacy full `LOGIN_USER` object only where the existing profile tool requires it, and add the canonical scalar `LOGIN_USER_ID` for CPS tools.

**Step 4: Rerun GREEN and commit**

---

### Task 6: Add signed MCP identity envelope and configuration

**Files:**

- Create: `backend/qiji-framework/qiji-common/src/main/java/com/qiji/cps/framework/common/mcp/McpIdentityClaims.java`
- Create: `backend/qiji-framework/qiji-common/src/main/java/com/qiji/cps/framework/common/mcp/McpIdentityEnvelope.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/framework/ai/config/QijiAiProperties.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/mcp/AiMcpIdentitySigner.java`
- Modify: `backend/qiji-server/src/main/resources/application.yaml`
- Modify: `backend/qiji-server/src/main/resources/application-local.yaml`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/service/mcp/AiMcpIdentitySignerTest.java`

**Step 1: Write failing signer tests**

Use a fixed clock and nonce supplier. Assert deterministic HMAC-SHA256 output, 60-second expiry, required claims, audience/client binding, and no signing when the secret is blank.

**Step 2: Confirm RED**

```bash
cd backend
mvn test -pl qiji-module-ai -Dtest=AiMcpIdentitySignerTest
```

**Step 3: Implement minimum signer and properties**

Add `spring.ai.mcp.self-test` properties for enablement, client name, base URL, endpoint, timeouts, TTL and environment-only secret. Serialize a stable canonical payload before signing. Never add a development default secret.

**Step 4: Rerun GREEN and commit**

---

### Task 7: Verify MCP identity, prevent replay, and enforce server-side tool risk

**Files:**

- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/security/CpsMcpIdentityVerifier.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/security/CpsMcpNonceStore.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/security/CpsMcpToolRisk.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/security/CpsMcpToolRiskRegistry.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/security/CpsMcpAuthorizationService.java`
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsMcpToolConfiguration.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/mcp/security/CpsMcpIdentityVerifierTest.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/mcp/security/CpsMcpAuthorizationServiceTest.java`
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/mcp/tool/CpsMcpToolConfigurationTest.java`

**Step 1: Write failing security tests**

Cover valid envelope, tampered payload/signature, expired/not-yet-valid claims, wrong tenant/client, missing claim, blank secret and replayed nonce. Cover risk mapping for every registered CPS/CPX tool and deny mutation unless `allowMutation=true`.

**Step 2: Confirm RED**

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsMcpIdentityVerifierTest,CpsMcpAuthorizationServiceTest,CpsMcpToolConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false
```

**Step 3: Implement fail-closed verifier and policy**

Use constant-time signature comparison. Consume nonce with Redis `SETNX` and expiry equal to the remaining claim TTL. Rebuild trusted tool context only after validation. Register each tool as `READ_ONLY`, `ATTRIBUTION_WRITE`, or `ASSET_WRITE`; unknown tools default to deny in self-test mode.

**Step 4: Rerun GREEN and commit**

---

### Task 8: Enrich and redact MCP access audit

**Files:**

- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsMcpToolAuditSupport.java`
- Modify: all audited tool functions under `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/mcp/tool/CpsMcpToolAuditSupportTest.java`

**Step 1: Write failing audit tests**

Assert audit rows include member, actor, actor type, conversation, client, invocation source and trace ID. Pass a request containing signature/API-key/nonce-like fields and assert they are absent or masked.

**Step 2: Confirm RED**

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsMcpToolAuditSupportTest -Dsurefire.failIfNoSpecifiedTests=false
```

**Step 3: Implement centralized audit context**

Change `record` to obtain identity/audit values from trusted tool context and apply one redaction function. Update tool callers mechanically; do not duplicate redaction rules per tool.

**Step 4: Rerun GREEN and commit**

---

### Task 9: Add lazy self-MCP client and signed `_meta` conversion

**Files:**

- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/mcp/AiSelfMcpClientManager.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/mcp/AiMcpToolContextMetaConverter.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/admin/chat/vo/mcp/AiMcpClientStatusRespVO.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/framework/ai/config/AiAutoConfiguration.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/service/mcp/AiSelfMcpClientManagerTest.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/service/mcp/AiMcpToolContextMetaConverterTest.java`

**Step 1: Confirm installed Spring AI API before coding**

Inspect the Spring AI 1.1.2 class signatures for `ToolContextToMcpMetaConverter`, MCP transport builder, initialization and close lifecycle. Do not code against a newer documentation-only signature.

**Step 2: Write failing tests**

Assert no client is created at application startup, first status/refresh creates it, discovery is cached, refresh replaces/cleans the old client, failure is reported without local fallback, and `_meta` contains only the signed envelope plus non-sensitive tracing metadata.

**Step 3: Confirm RED**

```bash
cd backend
mvn test -pl qiji-module-ai -Dtest=AiSelfMcpClientManagerTest,AiMcpToolContextMetaConverterTest
```

**Step 4: Implement lazy lifecycle**

Create the client under synchronization or an atomic state holder, initialize only on demand, cache status/tool discovery, expose refresh, and close replaced clients. Configure only the dedicated self-test client with the signed metadata converter; leave external MCP clients identity-free.

**Step 5: Rerun GREEN and commit**

---

### Task 10: Add administrator MCP test conversation APIs

**Files:**

- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/admin/chat/AiChatConversationController.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/admin/chat/AiMcpTestController.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/controller/admin/chat/vo/conversation/AiChatConversationCreateMcpTestReqVO.java`
- Create: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/chat/AiMcpTestConversationService.java`
- Modify: `backend/qiji-module-ai/src/main/java/com/qiji/cps/module/ai/service/chat/AiChatToolCallbackService.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/service/chat/AiMcpTestConversationServiceTest.java`
- Create: `backend/qiji-module-ai/src/test/java/com/qiji/cps/module/ai/controller/admin/chat/AiMcpTestControllerTest.java`

**Step 1: Write failing tests**

Test `ai:chat:mcp-test`, `member:user:query`, and mutation permission behavior. Mock `MemberUserApi.validateUser`, tenant context, model capability and self-client status. Assert selected member, client and `allowMutation` are persisted once and cannot change. Assert read-only discovery excludes write tools and self-MCP failure does not expose local equivalents.

**Step 2: Confirm RED**

```bash
cd backend
mvn test -pl qiji-module-ai -Dtest=AiMcpTestConversationServiceTest,AiMcpTestControllerTest
```

**Step 3: Implement APIs and orchestration**

Implement:

```text
POST /admin-api/ai/chat/conversation/create-mcp-test
GET  /admin-api/ai/chat/mcp-test/client-list
POST /admin-api/ai/chat/mcp-test/client-refresh
```

Before persisting, validate permission, tenant-scoped member, role/model capability, trusted self client, tool discovery, and mutation permission. Build signed claims per tool call from persisted conversation identity.

**Step 4: Rerun GREEN and commit**

---

### Task 11: Add admin role toggle and MCP test dialog/status UI

**Files:**

- Modify: `frontend/admin-vue3/src/api/ai/model/chatRole/index.ts`
- Modify: `frontend/admin-vue3/src/views/ai/model/chatRole/ChatRoleForm.vue`
- Modify: `frontend/admin-vue3/src/api/ai/chat/conversation/index.ts`
- Create: `frontend/admin-vue3/src/api/ai/chat/mcpTest/index.ts`
- Create: `frontend/admin-vue3/src/views/ai/chat/index/components/mcp/McpTestConversationDialog.vue`
- Create: `frontend/admin-vue3/src/views/ai/chat/index/components/mcp/McpTestIdentityBadge.vue`
- Modify: `frontend/admin-vue3/src/views/ai/chat/index/index.vue`
- Modify: `frontend/admin-vue3/src/views/ai/chat/index/components/conversation/ConversationList.vue`
- Create: `frontend/admin-vue3/e2e/ai-mcp-test.spec.ts`
- Create: `script/test/test_ai_mcp_test_ui_contract.py`

**Step 1: Write failing UI contract and Playwright tests**

Assert:

- role form sends `memberEnabled`.
- test dialog is permission-gated.
- member selector, role selector and client selector are required.
- mutation defaults false and requires an explicit confirmation dialog.
- created conversation displays fixed member, client, connection and read/write badges.
- identity fields cannot be edited after creation.
- failure state offers refresh.

**Step 2: Confirm RED**

```bash
python -m pytest script/test/test_ai_mcp_test_ui_contract.py -q
cd frontend/admin-vue3
pnpm e2e -- ai-mcp-test.spec.ts
```

**Step 3: Implement UI**

Reuse the existing member page API and role APIs. Keep all security decisions server-side; UI permission checks only control affordances. On successful creation, refresh the conversation list and select the new immutable test conversation.

**Step 4: Run frontend checks**

```bash
cd frontend/admin-vue3
pnpm ts:check
pnpm lint:eslint
pnpm e2e -- ai-mcp-test.spec.ts
```

**Step 5: Commit**

---

### Task 12: Add permissions, SQL consistency checks, and consolidated verification

**Files:**

- Modify: `backend/sql/module/ai-2026-07-10.sql`
- Modify: `backend/sql/module/cps-update.sql`
- Modify: `backend/sql/module/cps-all-in-one.sql`
- Modify: `backend/qiji-server/src/main/resources/application.yaml`
- Create: `script/test/test_ai_mcp_member_sql_contract.py`
- Modify if entrypoints changed: `docs/project-map.md`

**Step 1: Write failing SQL/config contract test**

Assert both permissions exist, AI migration contains every new conversation/role column and history backfill, both CPS scripts contain identical audit columns, no real secret is present, and no CPS SQL is added to `backend/sql/mysql/ruoyi-vue-pro.sql`.

**Step 2: Confirm RED then complete SQL/menu/config**

```bash
python -m pytest script/test/test_ai_mcp_member_sql_contract.py -q
```

Add `ai:chat:mcp-test` and `ai:chat:mcp-test-mutate` permission/menu records using the repo's existing AI menu IDs and idempotent migration pattern. Rerun GREEN.

**Step 3: Run focused backend suite**

```bash
cd backend
mvn test -pl qiji-module-ai
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am \
  -Dtest=CpsMcpIdentityVerifierTest,CpsMcpAuthorizationServiceTest,CpsMcpToolAuditSupportTest,CpsMcpToolConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

**Step 4: Run frontend and static verification**

```bash
cd frontend/admin-vue3
pnpm ts:check
pnpm lint:eslint
pnpm e2e -- ai-mcp-test.spec.ts
cd ../..
python -m pytest script/test/test_ai_mcp_test_ui_contract.py script/test/test_ai_mcp_member_sql_contract.py -q
git diff --check
```

**Step 5: Optional live smoke test when MySQL, Redis and backend credentials are available**

Start the backend with `local` profile and verify:

1. member App conversation can call a read-only CPS tool with its own `LOGIN_USER_ID`;
2. admin read-only test calls `/mcp/cps` and returns the chosen member's data;
3. write tool is absent in read-only mode;
4. tampered/replayed identity is rejected;
5. access log links member, admin, conversation, self client and trace ID.

If the environment cannot run this smoke test, record the exact missing dependency/credential as `Not-tested`; do not imply live MCP verification passed.

**Step 6: Review and final commit**

Run `requesting-code-review`, fix any P0/P1 findings, rerun affected tests, then use `verification-before-completion`. Commit remaining integration changes with a Lore-format message whose `Tested` trailer lists the exact successful commands.

---

## Expected commit sequence

1. Explicit conversation and audit identity schema.
2. Member-visible role and model capability contracts.
3. Immutable conversation ownership.
4. Member App AI chat APIs.
5. Trusted local tool context and callback assembly.
6. Signed MCP identity envelope.
7. MCP verification, replay defense and tool-risk authorization.
8. Enriched redacted MCP audit.
9. Lazy self-MCP client and metadata conversion.
10. Admin MCP test APIs.
11. Admin MCP test UI.
12. Permissions, migrations and final verification.

Each commit must stage only its task files and follow the repository Lore Commit Protocol.
