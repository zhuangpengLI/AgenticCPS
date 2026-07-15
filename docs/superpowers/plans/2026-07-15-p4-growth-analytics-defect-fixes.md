# P4 Growth Analytics Defect Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the three confirmed P4 reconciliation and billing-boundary defects without changing the existing Admin API request or response shapes.

**Architecture:** Keep the pure domain rules in `CpsGrowthAnalyticsService`, add regression coverage at both the service boundary and a standalone MockMvc/Jackson HTTP boundary, and retain the current controller contract. Reconciliation remains read-only and groups events by tenant, business order number, and idempotency key; billing validation changes from deny-list behavior to an explicit allowlist.

**Tech Stack:** Java 17, Spring MVC Test/MockMvc, Jackson Java Time support, JUnit 5, Mockito, Maven, Python/pytest contract tests.

---

## File structure

- Modify `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/growth/CpsGrowthAnalyticsServiceTest.java`: service-level RED/GREEN tests for bidirectional reconciliation, timeout state semantics, and billing fail-closed behavior.
- Modify `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/growth/CpsGrowthAnalyticsService.java`: minimal domain-rule changes only.
- Modify `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/controller/admin/growth/CpsGrowthAnalyticsControllerTest.java`: real JSON-to-controller-to-service regression test using standalone MockMvc.
- Modify `script/test/test_stage_four_growth_analytics_contract.py`: static contract markers for the new difference and rejection codes.
- No database, SQL, frontend, or API VO changes.

### Task 1: Add service regression tests and prove RED

**Files:**
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/growth/CpsGrowthAnalyticsServiceTest.java:71-109`
- Test: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/growth/CpsGrowthAnalyticsServiceTest.java`

- [ ] **Step 1: Extend the reconciliation test with TokenHub-only success**

Add one TokenHub-only event to the existing input and assert the inverse missing-success code:

```java
event("TOKENHUB", "EX-6", "tenant-1", "idem-6", "CREDIT", "SUCCESS", now.minusMinutes(1))
```

```java
assertTrue(summary.diffCodesByOrderNo().get("EX-6").contains("CPS_MISSING_SUCCESS"));
```

- [ ] **Step 2: Add timeout state-transition edge tests**

```java
@Test
@DisplayName("reconcileTokenEvents treats the threshold as timed out and ignores resolved processing")
void reconcileTokenEvents_usesLatestCpsSubmitStateForTimeout() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 15, 12, 0);

    CpsGrowthAnalyticsService.TokenReconciliationSummary summary = service.reconcileTokenEvents(
            List.of(
                    event("CPS", "EX-TIMEOUT", "tenant-1", "idem-timeout", "SUBMIT", "PROCESSING",
                            now.minusMinutes(30)),
                    event("CPS", "EX-RESOLVED", "tenant-1", "idem-resolved", "SUBMIT", "PROCESSING",
                            now.minusHours(2)),
                    event("CPS", "EX-RESOLVED", "tenant-1", "idem-resolved", "SUBMIT", "SUCCESS",
                            now.minusMinutes(1)),
                    event("TOKENHUB", "EX-RESOLVED", "tenant-1", "idem-resolved", "CREDIT", "SUCCESS",
                            now)),
            now,
            Duration.ofMinutes(30));

    assertTrue(summary.diffCodesByOrderNo().get("EX-TIMEOUT").contains("PROCESSING_TIMEOUT"));
    assertFalse(summary.diffCodesByOrderNo().containsKey("EX-RESOLVED"));
}
```

- [ ] **Step 3: Add unknown billing-action rejection assertions**

Extend `validateBillingBoundary_rejectsCpsAssetWrites`:

```java
CpsGrowthAnalyticsService.BillingBoundaryDecision unknownDecision = service.validateBillingBoundary(
        new CpsGrowthAnalyticsService.BillingBoundaryCommand(
                "billing-service", "UNKNOWN_UNCONFIRMED_EVENT", false, false, false));

assertFalse(unknownDecision.allowed());
assertEquals("ONLY_CONFIRMED_ASSET_EVENT_CONSUMPTION_ALLOWED", unknownDecision.reasonCode());
```

- [ ] **Step 4: Run the service test and verify the current implementation fails**

Run from `backend`:

```powershell
& 'C:\Users\zhuangpengli\.codex\tools\apache-maven-3.9.9\bin\mvn.cmd' test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=CpsGrowthAnalyticsServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: test failures show missing `CPS_MISSING_SUCCESS`, threshold timeout missing or resolved processing falsely reported, and the unknown billing action still allowed.

- [ ] **Step 5: Commit the RED regression tests**

```powershell
git add -- backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/growth/CpsGrowthAnalyticsServiceTest.java
git commit -m "Lock the confirmed P4 reconciliation and billing defects"
```

### Task 2: Implement the minimal domain-rule fixes

**Files:**
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/growth/CpsGrowthAnalyticsService.java:69-120`
- Test: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/growth/CpsGrowthAnalyticsServiceTest.java`

- [ ] **Step 1: Add inverse one-sided success detection**

Immediately after `TOKENHUB_MISSING_SUCCESS`, add:

```java
if (tokenCreditSuccessCount > 0 && cpsSuccessCount == 0) {
    diffCodes.add("CPS_MISSING_SUCCESS");
}
```

- [ ] **Step 2: Replace timeout `anyMatch` with latest CPS submit state**

Replace `hasTimedOutProcessing` with:

```java
private boolean hasTimedOutProcessing(List<TokenEvent> events, LocalDateTime now, Duration timeout) {
    return events.stream()
            .filter(event -> "CPS".equals(event.side()))
            .filter(event -> "SUBMIT".equals(event.eventType()))
            .max(Comparator.comparing(TokenEvent::eventTime))
            .filter(event -> "PROCESSING".equals(event.status()))
            .map(event -> !event.eventTime().plus(timeout).isAfter(now))
            .orElse(false);
}
```

- [ ] **Step 3: Convert billing validation to an allowlist**

Keep the existing forbidden-write detection and use the following decision order:

```java
if (forbiddenWrite) {
    return new BillingBoundaryDecision(false, "BILLING_MUST_NOT_WRITE_CPS_REBATE_ASSET");
}
boolean confirmedAssetConsumption = billingService
        && "CONSUME_CONFIRMED_ASSET_EVENT".equals(command.action());
if (!confirmedAssetConsumption) {
    return new BillingBoundaryDecision(false, "ONLY_CONFIRMED_ASSET_EVENT_CONSUMPTION_ALLOWED");
}
return new BillingBoundaryDecision(true, "ALLOWED_CONFIRMED_ASSET_EVENT_CONSUMPTION");
```

- [ ] **Step 4: Run service tests and verify GREEN**

Run the Task 1 Maven command again.

Expected: `CpsGrowthAnalyticsServiceTest` passes with zero failures and errors.

- [ ] **Step 5: Commit the service implementation**

```powershell
git add -- backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/growth/CpsGrowthAnalyticsService.java
git commit -m "Fail closed on P4 reconciliation and billing boundaries"
```

### Task 3: Add the real JSON/HTTP regression and static contract

**Files:**
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/controller/admin/growth/CpsGrowthAnalyticsControllerTest.java:1-115`
- Modify: `script/test/test_stage_four_growth_analytics_contract.py:16-32`

- [ ] **Step 1: Add standalone MockMvc/Jackson imports**

```java
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
```

- [ ] **Step 2: Add a JSON request regression test using the real service**

```java
@Test
void reconcileTokenEvents_deserializesIsoDurationAndReturnsTimeout() throws Exception {
    CpsGrowthAnalyticsController httpController = new CpsGrowthAnalyticsController();
    ReflectionTestUtils.setField(httpController, "growthAnalyticsService", new CpsGrowthAnalyticsService());
    MockMvc mockMvc = standaloneSetup(httpController)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(
                    JsonMapper.builder().findAndAddModules().build()))
            .build();

    mockMvc.perform(post("/cps/growth-analytics/token-reconciliation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "events": [{
                                "side": "CPS",
                                "businessOrderNo": "EX-HTTP-TIMEOUT",
                                "tenantId": "tenant-1",
                                "idempotencyKey": "idem-http-timeout",
                                "eventType": "SUBMIT",
                                "status": "PROCESSING",
                                "eventTime": "2026-07-15T11:30:00"
                              }],
                              "now": "2026-07-15T12:00:00",
                              "processingTimeout": "PT30M"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$['data']['diffCodesByOrderNo']['EX-HTTP-TIMEOUT'][0]")
                    .value("PROCESSING_TIMEOUT"));
}
```

- [ ] **Step 3: Lock the new codes in the Python contract test**

Add to `test_growth_analytics_service_exposes_stage_four_capabilities`:

```python
    for token in [
        "CPS_MISSING_SUCCESS",
        "PROCESSING_TIMEOUT",
        "ONLY_CONFIRMED_ASSET_EVENT_CONSUMPTION_ALLOWED",
        "CONSUME_CONFIRMED_ASSET_EVENT",
    ]:
        assert token in text
```

- [ ] **Step 4: Run Java and Python regression tests**

Run from `backend`:

```powershell
& 'C:\Users\zhuangpengli\.codex\tools\apache-maven-3.9.9\bin\mvn.cmd' test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=CpsGrowthAnalyticsServiceTest,CpsGrowthAnalyticsControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Run from repository root:

```powershell
python -m pytest script/test/test_stage_four_growth_analytics_contract.py -q
```

Expected: all targeted Java tests and all P4 Python contract tests pass.

- [ ] **Step 5: Commit the HTTP and static contract tests**

```powershell
git add -- backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/controller/admin/growth/CpsGrowthAnalyticsControllerTest.java script/test/test_stage_four_growth_analytics_contract.py
git commit -m "Cover the P4 JSON timeout and safety contracts"
```

### Task 4: Broaden verification and retest the running API

**Files:**
- Verify only; no planned source changes.

- [ ] **Step 1: Run the CPS biz module test suite**

Run from `backend`:

```powershell
& 'C:\Users\zhuangpengli\.codex\tools\apache-maven-3.9.9\bin\mvn.cmd' test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: Reactor `BUILD SUCCESS`; no new failures or errors.

- [ ] **Step 2: Check formatting and UTF-8 integrity for changed files**

```powershell
git diff --check HEAD~3..HEAD
python script/check_utf8_integrity.py
```

Expected: no diff errors and UTF-8 check succeeds.

- [ ] **Step 3: Rebuild and restart the local backend**

Build the current `qiji-server` and restart the local development process using the repository's existing local profile. Do not write the administrator Token to disk or process arguments that will be persisted.

Expected: `http://localhost:48080` responds and the new process loads the rebuilt CPS biz classes.

- [ ] **Step 4: Retest all three defects through the Admin API**

Send authenticated POST requests to:

```text
/admin-api/cps/growth-analytics/token-reconciliation
/admin-api/cps/growth-analytics/billing-boundary/validate
```

Acceptance evidence:

- A CPS `SUBMIT/PROCESSING` event exactly 30 minutes old with `PT30M` returns `PROCESSING_TIMEOUT`.
- A TokenHub-only `CREDIT/SUCCESS` event returns `CPS_MISSING_SUCCESS`.
- `UNKNOWN_UNCONFIRMED_EVENT` returns `allowed=false` with `ONLY_CONFIRMED_ASSET_EVENT_CONSUMPTION_ALLOWED`.
- `CONSUME_CONFIRMED_ASSET_EVENT` with all mutation flags false remains allowed.

- [ ] **Step 5: Record final verification evidence**

Report targeted test counts, module-suite result, runtime HTTP outcomes, and any validation gaps. Do not expose the administrator Token.
