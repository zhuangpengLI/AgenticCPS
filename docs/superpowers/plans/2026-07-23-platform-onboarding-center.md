# CPS Platform Onboarding Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the four separate CPS configuration menus with one platform onboarding center that supports first-time setup, safe draft-based reconfiguration, configuration CRUD, validation, connection testing, and atomic publish.

**Architecture:** Keep `cps_platform`, `cps_api_vendor`, `cps_adzone`, and `cps_rebate_config` as runtime truth. Store first-time and reconfiguration edits in an encrypted tenant-scoped draft, validate and test the exact draft fingerprint outside a database transaction, then publish the four configuration groups in one transaction and invalidate caches only after commit. The admin UI uses one dynamic menu route that switches between the platform list and a five-step full-page workspace.

**Tech Stack:** Java 17/21, Spring Boot 3.5.9, Spring Security, MyBatis Plus, MySQL/H2, Redis cache, Vue 3, TypeScript, Element Plus, Axios, Playwright, Maven, pnpm.

---

## Execution constraints

- The worktree is already dirty. Preserve every unrelated staged, unstaged, and untracked file.
- Before each commit, stage only files owned by that task and verify with `git diff --cached --name-only`.
- Use `apply_patch` for edits and verify every changed Chinese text file with UTF-8 decoding.
- Do not add dependencies. Reuse `EncryptTypeHandler`, `CpsPlatformClientFactory`, the four existing CPS configuration services, and current admin form patterns.
- Follow TDD: add the smallest failing test, run it and read the failure, implement the minimum behavior, then broaden verification.
- Do not remove the four legacy Controllers or API modules in this delivery. Remove only their menu visibility after the unified entry is proven.
- Commit messages must follow the repository Lore protocol.

## File structure

### Backend files to create

```text
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/
├── controller/admin/onboarding/
│   ├── CpsPlatformOnboardingController.java
│   └── vo/
│       ├── CpsPlatformOnboardingPageReqVO.java
│       ├── CpsPlatformOnboardingPageRespVO.java
│       ├── CpsPlatformOnboardingDraftSaveReqVO.java
│       ├── CpsPlatformOnboardingDetailRespVO.java
│       ├── CpsPlatformOnboardingCheckRespVO.java
│       ├── CpsPlatformOnboardingPublishReqVO.java
│       ├── CpsPlatformCapabilityRespVO.java
│       └── CpsVendorDescriptorRespVO.java
├── dal/dataobject/onboarding/CpsPlatformOnboardingDraftDO.java
├── dal/mysql/onboarding/CpsPlatformOnboardingDraftMapper.java
├── enums/onboarding/
│   ├── CpsPlatformOnboardingModeEnum.java
│   └── CpsPlatformOnboardingStatusEnum.java
└── service/onboarding/
    ├── CpsPlatformOnboardingService.java
    ├── CpsPlatformOnboardingServiceImpl.java
    ├── CpsPlatformOnboardingDraftService.java
    ├── CpsPlatformOnboardingDraftServiceImpl.java
    ├── CpsPlatformOnboardingValidator.java
    ├── CpsPlatformOnboardingConnectionTester.java
    ├── CpsPlatformOnboardingFingerprint.java
    ├── CpsPlatformOnboardingCacheInvalidator.java
    └── model/
        ├── CpsPlatformOnboardingPayload.java
        ├── CpsOnboardingVendor.java
        ├── CpsOnboardingAdzone.java
        └── CpsOnboardingRebateRule.java
```

### Backend files to modify

```text
backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsErrorCodeConstants.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/platform/CpsPlatformMapper.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/vendor/CpsApiVendorMapper.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/adzone/CpsAdzoneMapper.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/rebate/CpsRebateConfigMapper.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/platform/CpsPlatformService.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/platform/CpsPlatformServiceImpl.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/vendor/CpsApiVendorService.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/vendor/CpsApiVendorServiceImpl.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/adzone/CpsAdzoneService.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/adzone/CpsAdzoneServiceImpl.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateConfigService.java
backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateConfigServiceImpl.java
backend/qiji-module-cps/qiji-module-cps-biz/src/test/resources/sql/create_tables.sql
backend/qiji-module-cps/qiji-module-cps-biz/src/test/resources/sql/clean.sql
backend/qiji-module-cps/qiji-module-cps-biz/src/test/resources/application-unit-test.yaml
backend/sql/module/cps-all-in-one.sql
backend/sql/module/cps-update.sql
```

### Frontend files to create

```text
frontend/admin-vue3/src/api/cps/platformOnboarding.ts
frontend/admin-vue3/src/views/cps/platformOnboarding/
├── index.vue
├── workspace.vue
├── model.ts
└── components/
    ├── PlatformStep.vue
    ├── VendorStep.vue
    ├── AdzoneStep.vue
    ├── RebateStep.vue
    ├── ReviewStep.vue
    ├── CompletionBadge.vue
    ├── CheckResultPanel.vue
    ├── VendorEditorDialog.vue
    ├── AdzoneEditorDialog.vue
    ├── AdzoneBatchDialog.vue
    └── RebateRuleDialog.vue
frontend/admin-vue3/src/views/cps/components/adzoneRules.ts
frontend/admin-vue3/e2e/cps-platform-onboarding.spec.ts
script/test/test_admin_cps_platform_onboarding_ui_contract.py
```

### Documentation to modify

```text
README.md
docs/project-map.md
docs/superpowers/specs/2026-07-23-platform-onboarding-center-design.md
```

## Task 1: Add the encrypted onboarding draft schema

**Files:**

- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/onboarding/CpsPlatformOnboardingDraftDO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/onboarding/CpsPlatformOnboardingDraftMapper.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/enums/onboarding/CpsPlatformOnboardingModeEnum.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/enums/onboarding/CpsPlatformOnboardingStatusEnum.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/dal/mysql/onboarding/CpsPlatformOnboardingDraftMapperTest.java`
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/resources/sql/create_tables.sql`
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/resources/sql/clean.sql`
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/resources/application-unit-test.yaml`
- Modify: `backend/sql/module/cps-all-in-one.sql`
- Modify: `backend/sql/module/cps-update.sql`

- [ ] **Step 1: Write the failing tenant and optimistic-lock mapper test**

```java
class CpsPlatformOnboardingDraftMapperTest extends BaseDbUnitTest {

    @Resource
    private CpsPlatformOnboardingDraftMapper mapper;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void selectByPlatformCode_shouldIsolateTenant() {
        insertDraft(1L, "taobao", 1L);
        insertDraft(2L, "taobao", 1L);

        TenantContextHolder.setTenantId(1L);
        assertEquals(1L, mapper.selectByPlatformCode("taobao").getTenantId());

        TenantContextHolder.setTenantId(2L);
        assertEquals(2L, mapper.selectByPlatformCode("taobao").getTenantId());
    }

    @Test
    void updatePayload_whenVersionChanged_shouldRejectStaleWriter() {
        TenantContextHolder.setTenantId(1L);
        CpsPlatformOnboardingDraftDO draft = insertDraft(1L, "jd", 3L);

        int changed = mapper.updatePayload(
                draft.getId(), 2L, "ciphertext-v4", "fingerprint-v4", "DRAFT");

        assertEquals(0, changed);
    }

    @Test
    void payloadCiphertext_shouldEncryptAtRestAndRoundTrip() {
        TenantContextHolder.setTenantId(1L);
        CpsPlatformOnboardingDraftDO draft = insertDraftWithPayload(
                1L, "pdd", "{\"appSecret\":\"plain-secret\"}");

        String stored = jdbcTemplate.queryForObject(
                "SELECT payload_ciphertext FROM cps_platform_onboarding_draft WHERE id = ?",
                String.class, draft.getId());

        assertNotEquals("{\"appSecret\":\"plain-secret\"}", stored);
        assertEquals("{\"appSecret\":\"plain-secret\"}",
                mapper.selectById(draft.getId()).getPayloadCiphertext());
    }

    @Test
    void repeatedDeleteAndRecreate_shouldKeepHistoricalRowsWithoutUniqueConflict() {
        TenantContextHolder.setTenantId(1L);
        CpsPlatformOnboardingDraftDO first = insertDraft(1L, "douyin", 1L);
        mapper.deleteById(first.getId());

        assertDoesNotThrow(() -> insertDraft(1L, "douyin", 1L));
    }
}
```

- [ ] **Step 2: Run the test and verify the missing mapper/schema failure**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsPlatformOnboardingDraftMapperTest "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: compilation fails because `CpsPlatformOnboardingDraftDO` and `CpsPlatformOnboardingDraftMapper` do not exist.

- [ ] **Step 3: Add the draft DO with encrypted payload mapping**

```java
@TableName(value = "cps_platform_onboarding_draft", autoResultMap = true)
@KeySequence("cps_platform_onboarding_draft_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CpsPlatformOnboardingDraftDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String platformCode;
    private String mode;

    @TableField(typeHandler = EncryptTypeHandler.class)
    private String payloadCiphertext;

    private Long draftVersion;
    private String configFingerprint;
    private String validatedFingerprint;
    private String status;
    private String checkSummary;
    private LocalDateTime validatedAt;
    private LocalDateTime publishedAt;
}
```

Use explicit enums instead of scattered strings:

```java
public enum CpsPlatformOnboardingModeEnum {
    CREATE, RECONFIGURE
}

public enum CpsPlatformOnboardingStatusEnum {
    DRAFT, VALIDATING, READY, FAILED, PUBLISHED
}
```

The mapper must provide:

```java
default CpsPlatformOnboardingDraftDO selectByPlatformCode(String platformCode);

default int updatePayload(
        Long id,
        Long expectedVersion,
        String payload,
        String fingerprint,
        String status) {
    return update(new LambdaUpdateWrapper<CpsPlatformOnboardingDraftDO>()
            .eq(CpsPlatformOnboardingDraftDO::getId, id)
            .eq(CpsPlatformOnboardingDraftDO::getDraftVersion, expectedVersion)
            .set(CpsPlatformOnboardingDraftDO::getPayloadCiphertext, payload)
            .set(CpsPlatformOnboardingDraftDO::getConfigFingerprint, fingerprint)
            .set(CpsPlatformOnboardingDraftDO::getValidatedFingerprint, null)
            .set(CpsPlatformOnboardingDraftDO::getValidatedAt, null)
            .set(CpsPlatformOnboardingDraftDO::getCheckSummary, null)
            .set(CpsPlatformOnboardingDraftDO::getStatus, status)
            .set(CpsPlatformOnboardingDraftDO::getDraftVersion, expectedVersion + 1));
}
```

- [ ] **Step 4: Add the production and H2 schema**

Add the canonical MySQL table to both module SQL scripts:

```sql
CREATE TABLE `cps_platform_onboarding_draft` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `mode` varchar(16) NOT NULL COMMENT 'CREATE/RECONFIGURE',
  `payload_ciphertext` longtext NOT NULL COMMENT '加密后的配置草稿',
  `draft_version` bigint NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  `config_fingerprint` varchar(64) NOT NULL COMMENT '当前配置指纹',
  `validated_fingerprint` varchar(64) DEFAULT NULL COMMENT '最近检测通过的配置指纹',
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/VALIDATING/READY/FAILED/PUBLISHED',
  `check_summary` text DEFAULT NULL COMMENT '脱敏检测摘要 JSON',
  `validated_at` datetime DEFAULT NULL COMMENT '检测通过时间',
  `published_at` datetime DEFAULT NULL COMMENT '最近发布时间',
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `active_unique_key` varchar(128) GENERATED ALWAYS AS
    (IF(`deleted` = b'0', CONCAT(`tenant_id`, ':', `platform_code`), NULL)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_tenant_platform` (`active_unique_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS平台接入草稿';
```

In the same migration:

```sql
ALTER TABLE `cps_platform`
  DROP INDEX `uk_platform_code`,
  ADD COLUMN `active_unique_key` varchar(128) GENERATED ALWAYS AS
    (IF(`deleted` = b'0', CONCAT(`tenant_id`, ':', `platform_code`), NULL)) STORED,
  ADD UNIQUE KEY `uk_active_tenant_platform` (`active_unique_key`);

ALTER TABLE `cps_adzone`
  ADD COLUMN `active_unique_key` varchar(256) GENERATED ALWAYS AS
    (IF(`deleted` = b'0',
        CONCAT(`tenant_id`, ':', `platform_code`, ':', `adzone_id`), NULL)) STORED,
  ADD UNIQUE KEY `uk_active_tenant_platform_adzone` (`active_unique_key`);
```

Do not use a unique key on `(tenant_id, business_key, deleted)`: after the first soft delete it blocks a second delete/recreate cycle. The nullable generated key enforces uniqueness only for active rows and preserves all deleted history.

Add H2-compatible versions of `cps_platform`, `cps_api_vendor`, `cps_adzone`, `cps_rebate_config`, and `cps_platform_onboarding_draft` to `create_tables.sql`; add matching cleanup statements in child-to-parent order to `clean.sql`. Add a deterministic test-only `mybatis-plus.encryptor.password` value to `application-unit-test.yaml`; it must contain no production credential.

- [ ] **Step 5: Run the mapper test and SQL consistency checks**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsPlatformOnboardingDraftMapperTest "-Dsurefire.failIfNoSpecifiedTests=false"
cd ..
python script/check_utf8_integrity.py backend/sql/module/cps-all-in-one.sql backend/sql/module/cps-update.sql
git diff --check -- backend/sql/module/cps-all-in-one.sql backend/sql/module/cps-update.sql
```

Expected: mapper tests pass; both SQL files decode as UTF-8; no whitespace errors.

- [ ] **Step 6: Commit only the schema slice**

```text
Create tenant-safe onboarding drafts before touching runtime configuration

Constraint: Reconfiguration must remain isolated from live CPS routing until publish
Rejected: Plain JSON draft storage | exposes supplier credentials
Confidence: high
Scope-risk: moderate
Directive: Keep cps-all-in-one.sql and cps-update.sql synchronized
Tested: CpsPlatformOnboardingDraftMapperTest and UTF-8 SQL validation
```

## Task 2: Define the typed payload, fingerprint, and secret-merge contract

**Files:**

- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/model/CpsPlatformOnboardingPayload.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/model/CpsOnboardingVendor.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/model/CpsOnboardingAdzone.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/model/CpsOnboardingRebateRule.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingFingerprint.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingTestFixtures.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingFingerprintTest.java`
- Modify: `backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsErrorCodeConstants.java`

- [ ] **Step 1: Write failing canonical fingerprint tests**

```java
class CpsPlatformOnboardingFingerprintTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private final CpsPlatformOnboardingFingerprint fingerprint =
            new CpsPlatformOnboardingFingerprint(objectMapper);

    @Test
    void calculate_shouldIgnoreVendorAndRuleInputOrder() {
        CpsPlatformOnboardingPayload left = payload(
                List.of(vendor("dataoke"), vendor("haodanku")),
                List.of(rule(null, 10), rule(2L, 20)));
        CpsPlatformOnboardingPayload right = payload(
                List.of(vendor("haodanku"), vendor("dataoke")),
                List.of(rule(2L, 20), rule(null, 10)));

        assertEquals(fingerprint.calculate(left), fingerprint.calculate(right));
    }

    @Test
    void mergeSecrets_shouldKeepStoredSecretWhenRequestIsBlank() {
        CpsOnboardingVendor incoming = vendor("dataoke");
        incoming.setAppSecret("");
        CpsOnboardingVendor stored = vendor("dataoke");
        stored.setAppSecret("stored-secret");

        CpsOnboardingVendor merged = fingerprint.mergeSecrets(incoming, stored);

        assertEquals("stored-secret", merged.getAppSecret());
    }
}
```

Use static imports from `CpsPlatformOnboardingTestFixtures`. That fixture class is the only shared test builder and must provide these concrete methods used by Tasks 2–6:

```java
static CpsPlatformOnboardingPayload payload(
        List<CpsOnboardingVendor> vendors,
        List<CpsOnboardingRebateRule> rules);
static CpsOnboardingVendor vendor(String vendorCode);
static CpsOnboardingRebateRule rule(Long memberLevelId, Integer priority);
static CpsPlatformOnboardingDraftDO readyDraft(
        Long id, Long version, String validatedFingerprint);
static CpsPlatformOnboardingDraftDO draft(Long id, Long version);
static CpsPlatformOnboardingDraftSaveReqVO saveRequest(
        String platformCode, Long draftVersion);
static CpsPlatformDO platform(String platformCode);
static CpsPlatformOnboardingPayload withUnregisteredPlatform();
static CpsPlatformOnboardingPayload withMissingPrimaryVendor();
static CpsPlatformOnboardingPayload withDuplicateVendors();
static CpsPlatformOnboardingPayload withForeignDefaultAdzone();
static CpsPlatformOnboardingPayload withNegativeRebateAmount();
static CpsPlatformOnboardingPayload withDuplicateRebateScope();
static DraftSnapshot draftWithSecret(String secret);
static CpsPlatformOnboardingPublishReqVO publish(
        String platformCode, Long version, String fingerprint, boolean enable);
static CpsPlatformOnboardingPublishReqVO readyCreateRequest(String platformCode);
static CpsPlatformDO conflictingPlatform(String platformCode);
static CpsPlatformDO enabledPlatform(String platformCode);
```

Implement each method with explicit valid defaults: one registered platform, one enabled primary vendor, one enabled general adzone selected as runtime default, and one platform-default rebate rule. Invalid builders change only the field named by the method, keeping each validation test single-cause.

- [ ] **Step 2: Run the test and verify missing domain types**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsPlatformOnboardingFingerprintTest "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: compilation fails because the onboarding model and fingerprint service do not exist.

- [ ] **Step 3: Implement stable payload keys and canonicalization**

The payload root must have exactly these sections:

```java
@Data
public class CpsPlatformOnboardingPayload {
    private CpsPlatformSaveReqVO platform;
    private String primaryVendorCode;
    private String runtimeDefaultAdzoneId;
    private List<CpsOnboardingVendor> vendors = new ArrayList<>();
    private List<CpsOnboardingAdzone> adzones = new ArrayList<>();
    private List<CpsOnboardingRebateRule> rebateRules = new ArrayList<>();
}
```

Stable business keys:

```java
String vendorKey(CpsOnboardingVendor vendor) {
    return vendor.getVendorCode();
}

String adzoneKey(CpsOnboardingAdzone adzone) {
    return adzone.getAdzoneId();
}

String rebateKey(CpsOnboardingRebateRule rule) {
    return (rule.getMemberLevelId() == null ? "DEFAULT" : rule.getMemberLevelId())
            + ":" + rule.getPriority();
}
```

`calculate()` must deep-copy, sort each collection by its stable key, serialize only business fields, and return lowercase SHA-256 hex. It must exclude IDs, timestamps, `draftVersion`, validation results, and ciphertext.

- [ ] **Step 4: Add onboarding error codes**

Use the next CPS range:

```java
// ========== 平台接入中心 1-100-017-000 ==========
ErrorCode ONBOARDING_DRAFT_NOT_EXISTS =
        new ErrorCode(1_100_017_000, "平台[{}]接入草稿不存在");
ErrorCode ONBOARDING_DRAFT_VERSION_CONFLICT =
        new ErrorCode(1_100_017_001, "平台接入草稿已被其他操作更新，请刷新后重试");
ErrorCode ONBOARDING_CONFIG_INVALID =
        new ErrorCode(1_100_017_002, "平台接入配置不合法：{}");
ErrorCode ONBOARDING_TEST_REQUIRED =
        new ErrorCode(1_100_017_003, "当前草稿版本尚未通过连接检测");
ErrorCode ONBOARDING_PLATFORM_ENABLED =
        new ErrorCode(1_100_017_004, "平台已启用，请先停用后再删除");
ErrorCode ONBOARDING_PUBLISH_CONFLICT =
        new ErrorCode(1_100_017_005, "草稿配置与已检测配置不一致，请重新检测");
ErrorCode ADZONE_RELATION_REQUIRED =
        new ErrorCode(1_100_017_006, "渠道或会员推广位缺少必需的归因关系");
ErrorCode REBATE_CONFIG_AMOUNT_RANGE_INVALID =
        new ErrorCode(1_100_017_007, "返利金额下限不能大于上限");
```

- [ ] **Step 5: Run the focused tests**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsPlatformOnboardingFingerprintTest "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: all fingerprint and secret-merge tests pass.

- [ ] **Step 6: Commit**

```text
Make draft identity stable across harmless UI ordering changes

Constraint: Publish may accept only the exact configuration that passed testing
Rejected: Hashing raw request JSON | array order and metadata create false conflicts
Confidence: high
Scope-risk: narrow
Directive: Never include ciphertext randomness in config fingerprints
Tested: CpsPlatformOnboardingFingerprintTest
```

## Task 3: Implement draft save, resume, and optimistic concurrency

**Files:**

- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingDraftService.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingDraftServiceImpl.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/onboarding/vo/CpsPlatformOnboardingDraftSaveReqVO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/onboarding/vo/CpsPlatformOnboardingDetailRespVO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingDraftServiceImplTest.java`

- [ ] **Step 1: Write failing save/resume tests**

```java
@ExtendWith(MockitoExtension.class)
class CpsPlatformOnboardingDraftServiceImplTest {

    @Mock private CpsPlatformOnboardingDraftMapper draftMapper;
    @Mock private CpsPlatformMapper platformMapper;
    @Mock private CpsApiVendorMapper vendorMapper;
    @Mock private CpsAdzoneMapper adzoneMapper;
    @Mock private CpsRebateConfigMapper rebateMapper;

    @Test
    void saveDraft_shouldResetValidationAndIncrementVersion() {
        when(draftMapper.selectByPlatformCode("taobao"))
                .thenReturn(readyDraft(8L, 4L, "old-fingerprint"));

        CpsPlatformOnboardingDetailRespVO saved =
                service.saveDraft(saveRequest("taobao", 4L));

        verify(draftMapper).updatePayload(
                eq(8L), eq(4L), anyString(), anyString(), eq("DRAFT"));
        assertEquals(5L, saved.getDraftVersion());
        assertNull(saved.getValidatedFingerprint());
    }

    @Test
    void saveDraft_whenVersionIsStale_shouldFailWithoutOverwrite() {
        when(draftMapper.selectByPlatformCode("taobao"))
                .thenReturn(draft(8L, 6L));
        when(draftMapper.updatePayload(anyLong(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(0);

        assertServiceException(
                () -> service.saveDraft(saveRequest("taobao", 5L)),
                ONBOARDING_DRAFT_VERSION_CONFLICT);
    }

    @Test
    void getDetail_withoutDraft_shouldBuildReconfigurePayloadFromRuntimeTables() {
        when(platformMapper.selectByPlatformCode("jd")).thenReturn(platform("jd"));

        CpsPlatformOnboardingDetailRespVO result = service.getDetail("jd");

        assertEquals("RECONFIGURE", result.getMode());
        assertEquals("jd", result.getPayload().getPlatform().getPlatformCode());
        assertTrue(result.getPayload().getVendors().stream()
                .allMatch(vendor -> vendor.getAppSecret() == null));
    }
}
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsPlatformOnboardingDraftServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: compilation failure for missing draft service and VOs.

- [ ] **Step 3: Implement draft create/update/resume**

Service contract:

```java
public interface CpsPlatformOnboardingDraftService {
    CpsPlatformOnboardingDetailRespVO getDetail(String platformCode);
    CpsPlatformOnboardingDetailRespVO saveDraft(
            @Valid CpsPlatformOnboardingDraftSaveReqVO request);
    void deleteDraft(String platformCode);
    CpsPlatformOnboardingPayload getRequiredPayload(String platformCode);
    void markValidating(Long draftId, Long expectedVersion);
    void markChecked(Long draftId, Long expectedVersion,
                     String status, String validatedFingerprint,
                     String checkSummary, LocalDateTime validatedAt);
}
```

Behavior:

- New platform creates `mode=CREATE`, `draftVersion=1`, `status=DRAFT`.
- Existing platform without a draft creates a response-only `RECONFIGURE` payload from runtime rows.
- First save persists that generated payload as a draft.
- Runtime AppSecret/AuthToken values are merged server-side when incoming sensitive fields are blank.
- Responses expose `appSecretConfigured` and `authTokenConfigured` booleans, never secret text.
- A changed payload clears `validatedFingerprint`, `validatedAt`, and old check results.
- A stale `draftVersion` throws `ONBOARDING_DRAFT_VERSION_CONFLICT`.

- [ ] **Step 4: Run tests and verify sensitive response fields**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsPlatformOnboardingDraftServiceImplTest,CpsPlatformOnboardingFingerprintTest "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: tests pass and assertions prove responses contain configured flags but no secret values.

- [ ] **Step 5: Commit**

```text
Let operators resume configuration without mutating live platforms

Constraint: Existing credentials must survive blank edit fields without being disclosed
Rejected: Browser-held drafts | cannot support safe resume or multi-user version checks
Confidence: high
Scope-risk: moderate
Directive: Treat every payload change as invalidating previous test evidence
Tested: CpsPlatformOnboardingDraftServiceImplTest
```

## Task 4: Add cross-section validation and draft connection testing

**Files:**

- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingValidator.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingConnectionTester.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/onboarding/vo/CpsPlatformOnboardingCheckRespVO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingValidatorTest.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingConnectionTesterTest.java`
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/adzone/CpsAdzoneServiceImpl.java`
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateConfigServiceImpl.java`
- Modify: corresponding existing service tests.

- [ ] **Step 1: Write failing validation matrix tests**

```java
@ParameterizedTest
@MethodSource("invalidPayloads")
void validate_shouldReturnFieldSpecificErrors(
        CpsPlatformOnboardingPayload payload,
        String expectedCode,
        String expectedPath) {
    CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);
    assertFalse(result.isSuccess());
    assertTrue(result.getItems().stream().anyMatch(item ->
            expectedCode.equals(item.getCode())
                    && expectedPath.equals(item.getFieldPath())));
}

static Stream<Arguments> invalidPayloads() {
    return Stream.of(
            arguments(withUnregisteredPlatform(), "PLATFORM_NOT_REGISTERED", "platform.platformCode"),
            arguments(withMissingPrimaryVendor(), "PRIMARY_VENDOR_REQUIRED", "primaryVendorCode"),
            arguments(withDuplicateVendors(), "VENDOR_DUPLICATE", "vendors"),
            arguments(withForeignDefaultAdzone(), "DEFAULT_ADZONE_INVALID", "runtimeDefaultAdzoneId"),
            arguments(withNegativeRebateAmount(), "REBATE_AMOUNT_INVALID", "rebateRules[0]"),
            arguments(withDuplicateRebateScope(), "REBATE_SCOPE_DUPLICATE", "rebateRules"));
}
```

Add focused service regression tests:

```java
@Test
void createAdzone_whenTaobaoChannelHasNoExternalRelationId_shouldReject() {
    CpsAdzoneSaveReqVO request = new CpsAdzoneSaveReqVO()
            .setPlatformCode("taobao")
            .setAdzoneId("mm_1_2_3")
            .setAdzoneType("channel")
            .setRelationId(9001L)
            .setExternalRelationId(null)
            .setStatus(1);

    assertServiceException(
            () -> adzoneService.createAdzone(request),
            ADZONE_RELATION_REQUIRED);
}

@Test
void createRebateConfig_whenMinGreaterThanMax_shouldReject() {
    CpsRebateConfigSaveReqVO request = new CpsRebateConfigSaveReqVO()
            .setPlatformCode("taobao")
            .setRebateRate(new BigDecimal("20"))
            .setMinRebateAmount(new BigDecimal("100"))
            .setMaxRebateAmount(new BigDecimal("50"))
            .setStatus(1);

    assertServiceException(
            () -> rebateConfigService.createRebateConfig(request),
            REBATE_CONFIG_AMOUNT_RANGE_INVALID);
}
```

- [ ] **Step 2: Run the tests and verify failures**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=CpsPlatformOnboardingValidatorTest,CpsAdzoneServiceImplTest,CpsRebateConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: new validation tests fail; existing tests continue to compile.

- [ ] **Step 3: Implement deterministic validation**

`CpsPlatformOnboardingValidator.validate(payload)` must:

- Require `platform.platformCode` in the union of `CpsPlatformClientFactory.getRegisteredPlatformCodes()` and platform codes exposed by registered vendor descriptors.
- Require exactly one primary vendor and no duplicate `vendorCode`.
- Require descriptors for every enabled vendor.
- Run `CpsVendorConfigSchema.validate()` for each enabled vendor.
- Require a registered vendor client with real business capabilities, not only `CONNECTION_TEST`.
- Require one enabled general adzone and a valid runtime default.
- Synchronize the main vendor default adzone in the normalized result.
- Reject duplicate adzone IDs.
- Reject `platformCode=null` rebate rules.
- Reject member-personal rules in this workspace.
- Validate rate, min/max amount, status, priority, and normalized rebate scope uniqueness.

Return all deterministic validation failures in one response; do not stop on the first field.

- [ ] **Step 4: Write the failing connection-test state test**

```java
@Test
void testDraft_shouldTestExactFingerprintAndMaskFailureDetails() {
    when(draftService.getRequiredDraft("taobao"))
            .thenReturn(draftWithSecret("raw-secret"));
    when(client.testConnection(any())).thenThrow(
            new IllegalStateException("token raw-secret rejected"));

    CpsPlatformOnboardingCheckRespVO result = tester.test("taobao", 5L);

    assertFalse(result.isSuccess());
    assertFalse(result.toString().contains("raw-secret"));
    verify(draftService).markChecked(
            anyLong(), eq(5L), eq("FAILED"), isNull(), anyString(), isNull());
}
```

- [ ] **Step 5: Implement connection testing outside transactions**

```java
public CpsPlatformOnboardingCheckRespVO test(String platformCode, Long draftVersion) {
    DraftSnapshot snapshot = draftService.getRequiredSnapshot(platformCode, draftVersion);
    CpsPlatformOnboardingCheckRespVO structural = validator.validate(snapshot.payload());
    if (!structural.isSuccess()) {
        draftService.markChecked(snapshot.id(), draftVersion, "FAILED",
                null, sanitize(structural), null);
        return structural;
    }
    // Test primary and every enabled backup independently.
    // Store READY only when all enabled vendors pass.
    // A failing optional vendor may be disabled and retested.
}
```

Do not annotate connection testing with `@Transactional`. Build `CpsVendorConfig` from draft values and call the registered `CpsApiVendorClient.testConnection()` directly.

- [ ] **Step 6: Run focused tests**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=CpsPlatformOnboardingValidatorTest,CpsPlatformOnboardingConnectionTesterTest,CpsAdzoneServiceImplTest,CpsRebateConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: all validation and connection-test cases pass; no credential appears in assertion output.

- [ ] **Step 7: Commit**

```text
Reject unsafe platform bundles before they reach runtime tables

Constraint: Connection calls must not hold database transactions
Rejected: First-error validation | forces repeated submit-and-fix cycles
Confidence: high
Scope-risk: moderate
Directive: Enabled backup vendors must pass or be disabled before publish
Tested: Onboarding validator, connection tester, adzone, and rebate service tests
```

## Task 5: Implement atomic and idempotent publish

**Files:**

- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingService.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingServiceImpl.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingCacheInvalidator.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/onboarding/vo/CpsPlatformOnboardingPublishReqVO.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingPublishDbTest.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingCacheInvalidatorTest.java`
- Modify: the four Mapper and Service pairs listed in the file map.

- [ ] **Step 1: Write failing DB tests for safe reconfiguration**

```java
class CpsPlatformOnboardingPublishDbTest extends BaseDbUnitTest {

    @Test
    void publishReconfigure_shouldKeepRuntimeRowsUntilPublish() {
        seedRuntimePlatform("taobao", "dataoke", "old-pid", new BigDecimal("20"));
        saveReadyDraft("taobao", "haodanku", "new-pid", new BigDecimal("35"));

        assertEquals("dataoke", platformMapper.selectByPlatformCode("taobao").getActiveVendorCode());
        assertEquals("old-pid", platformMapper.selectByPlatformCode("taobao").getDefaultAdzoneId());
    }

    @Test
    void publishReconfigure_shouldSwitchAllFourGroupsTogether() {
        CpsPlatformOnboardingPublishReqVO request =
                publish("taobao", 7L, "validated-fingerprint", true);

        service.publish(request);

        assertEquals("haodanku", platformMapper.selectByPlatformCode("taobao").getActiveVendorCode());
        assertEquals("new-pid", platformMapper.selectByPlatformCode("taobao").getDefaultAdzoneId());
        assertEquals("new-pid", vendorMapper
                .selectByVendorAndPlatform("haodanku", "taobao").getDefaultAdzoneId());
        assertEquals(0, rebateMapper.selectManagedRulesByPlatform("taobao").get(0)
                .getRebateRate().compareTo(new BigDecimal("35")));
    }

    @Test
    void publish_whenFinalPlatformWriteFails_shouldRollbackVendorAdzoneAndRebateWrites() {
        platformMapper.insert(conflictingPlatform("taobao"));

        assertThrows(Exception.class,
                () -> service.publish(readyCreateRequest("taobao")));

        assertTrue(vendorMapper.selectListByPlatformCodeAllStatuses("taobao").isEmpty());
        assertTrue(adzoneMapper.selectListByPlatformCode("taobao").isEmpty());
        assertTrue(rebateMapper.selectManagedRulesByPlatform("taobao").isEmpty());
    }

    @Test
    void publishSameVersionTwice_shouldBeIdempotent() {
        CpsPlatformOnboardingPublishReqVO request = readyCreateRequest("jd");
        service.publish(request);
        service.publish(request);
        assertEquals(1, platformMapper.selectCountByPlatform("jd"));
    }

    @Test
    void publish_shouldNotReadOrWriteAnotherTenantBundle() {
        seedReadyDraftForTenant(1L, "pdd");
        TenantContextHolder.setTenantId(2L);

        assertServiceException(
                () -> service.publish(readyCreateRequest("pdd")),
                ONBOARDING_DRAFT_NOT_EXISTS);
    }
}
```

- [ ] **Step 2: Run the DB test and verify failure**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsPlatformOnboardingPublishDbTest "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: compilation failure for the publish service and missing platform-scoped mapper methods.

- [ ] **Step 3: Add platform-scoped service primitives**

Add explicit methods used only by the orchestration service:

```java
List<CpsApiVendorDO> getVendorListByPlatform(String platformCode);
List<CpsAdzoneDO> getAdzoneListByPlatform(String platformCode);
List<CpsRebateConfigDO> getManagedRebateRulesByPlatform(String platformCode);
void deleteVendorsNotIn(String platformCode, Set<String> retainedVendorCodes);
void deleteAdzonesNotIn(String platformCode, Set<String> retainedAdzoneIds);
void deleteManagedRebateRulesNotIn(String platformCode, Set<String> retainedScopeKeys);
```

“Managed rebate rules” means `platformCode` matches and `memberId IS NULL`; individual member rules are never removed by onboarding publish.

Harden the reused service primitives before orchestration:

- Vendor reads include enabled and disabled rows so a disabled backup can be edited instead of duplicated.
- Blank `appSecret` or `authToken` on update preserves the encrypted stored value; explicit secret clearing is not supported by this workflow.
- Vendor `extraConfig` is parsed and validated by its registered descriptor. Validation errors and logs contain only field names/codes, never raw JSON or credentials.
- Adzone writes normalize `adzoneType`/`relationType`, enforce platform-plus-adzone uniqueness, require channel/member relation fields, and require Taobao channel `externalRelationId`.
- Rebate writes enforce rate `0–100`, nonnegative amount bounds, `min <= max`, and normalized scope uniqueness. Onboarding forbids global (`platformCode=null`) and personal-member (`memberId!=null`) rules.
- Platform code is immutable after creation. Enabling requires a publishable bundle, and deleting an enabled platform remains forbidden.

- [ ] **Step 4: Implement transactional publish**

```java
@Transactional(rollbackFor = Exception.class)
public CpsPlatformOnboardingDetailRespVO publish(
        CpsPlatformOnboardingPublishReqVO request) {
    DraftSnapshot draft = draftService.getRequiredSnapshot(
            request.getPlatformCode(), request.getDraftVersion());
    if (!Objects.equals(draft.fingerprint(), draft.validatedFingerprint())
            || !Objects.equals(draft.fingerprint(), request.getConfigFingerprint())) {
        throw exception(ONBOARDING_PUBLISH_CONFLICT);
    }

    CpsPlatformOnboardingPayload payload = validator.validateAndNormalize(draft.payload());
    upsertVendors(payload);
    upsertAdzones(payload);
    upsertManagedRebateRules(payload);
    upsertPlatformLast(payload, request.getEnableAfterPublish());
    removeMissingManagedRows(payload);
    draftService.markPublished(draft.id(), draft.version());
    cacheInvalidator.evictAfterCommit(payload.getPlatform().getPlatformCode());
    return detailAssembler.fromRuntime(payload.getPlatform().getPlatformCode());
}
```

`markPublished` sets `status=PUBLISHED` and `publishedAt`, while retaining the tested fingerprint as audit evidence. A second publish request with the same platform, draft version, and fingerprint returns the current runtime detail without writing again; a different version or fingerprint fails with `ONBOARDING_PUBLISH_CONFLICT`.

Upsert keys:

- Vendor: `vendorCode + platformCode`.
- Adzone: `adzoneId + platformCode`.
- Rebate rule: normalized `memberLevelId/default + priority + platformCode`.
- Platform: `platformCode`.

The platform write is last because its active vendor and default adzone validations depend on the other rows. Never call `batchCreateAdzones()` inside publish because it intentionally permits partial success.

- [ ] **Step 5: Invalidate caches only after commit**

```java
public void evictAfterCommit(String platformCode) {
    TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(CpsCacheConfig.CACHE_PLATFORM, platformCode);
                    evict(CpsCacheConfig.CACHE_API_VENDOR, platformCode);
                    clear(CpsCacheConfig.CACHE_REBATE_CONFIG);
                }
            });
}
```

Use the named `cpsCacheManager`. Add a test proving no cache operation occurs before commit and all three cache groups are evicted after commit.

`CpsPlatformOnboardingCacheInvalidatorTest` must cover these exact cases:

- `evictAfterCommit_insideTransaction_shouldNotEvictBeforeCommit`
- `evictAfterCommit_afterSuccessfulCommit_shouldEvictPlatformVendorAndRebateCaches`
- `evictAfterCommit_afterRollback_shouldNotEvict`

Route legacy platform/vendor/rebate mutations through the same invalidator or register equivalent `afterCommit` callbacks. Do not rely on `@CacheEvict` from a method invoked inside the outer publish transaction because it can evict before the transaction outcome is known.

- [ ] **Step 6: Run publish, existing service, and cache tests**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=CpsPlatformOnboardingPublishDbTest,CpsPlatformOnboardingCacheInvalidatorTest,CpsPlatformServiceImplTest,CpsApiVendorServiceImplTest,CpsAdzoneServiceImplTest,CpsRebateConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: atomic switch, rollback, idempotency, default-adzone synchronization, and regression tests all pass.

- [ ] **Step 7: Commit**

```text
Switch tested platform bundles as one runtime decision

Constraint: Search, transfer, order sync, and rebate settlement must never observe half-published configuration
Rejected: Frontend sequencing of four CRUD APIs | leaves partial runtime state
Confidence: high
Scope-risk: broad
Directive: Keep platform write last and cache invalidation after commit
Tested: Publish DB tests and four existing configuration service suites
```

## Task 6: Add aggregate reads, capabilities, lifecycle actions, and permissions

**Files:**

- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/onboarding/CpsPlatformOnboardingController.java`
- Create: remaining onboarding request/response VOs from the file map.
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/controller/admin/onboarding/CpsPlatformOnboardingControllerTest.java`
- Create: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/onboarding/CpsPlatformOnboardingLifecycleServiceTest.java`
- Modify: `backend/sql/module/cps-all-in-one.sql`
- Modify: `backend/sql/module/cps-update.sql`

- [ ] **Step 1: Write failing completion and lifecycle tests**

```java
@Test
void page_shouldReturnComputedMissingItems() {
    CpsPlatformOnboardingPageRespVO item = service.getPage(
            new CpsPlatformOnboardingPageReqVO().setPageNo(1).setPageSize(10))
            .getList().get(0);
    assertEquals(60, item.getCompletionPercent());
    assertEquals(List.of("DEFAULT_REBATE", "CONNECTION_TEST"),
            item.getMissingItems());
}

@Test
void deletePlatform_whenEnabled_shouldReject() {
    when(platformService.getPlatformByCode("taobao"))
            .thenReturn(enabledPlatform("taobao"));
    assertServiceException(
            () -> service.deletePlatformBundle("taobao"),
            ONBOARDING_PLATFORM_ENABLED);
}

@Test
void deletePlatformBundle_shouldSoftDeleteOnlyPlatformScopedManagedRows() {
    service.deletePlatformBundle("taobao");
    verify(rebateConfigService).deleteManagedRulesByPlatform("taobao");
}
```

The lifecycle DB fixture must seed one global rebate rule and one member-personal rule before deletion, then assert both rows remain active after the platform bundle is deleted.

- [ ] **Step 2: Implement aggregate list and detail reads**

The page response must contain:

```java
private String platformCode;
private String platformName;
private String primaryVendorCode;
private Integer backupVendorCount;
private String runtimeDefaultAdzoneId;
private BigDecimal defaultRebateRate;
private Integer completionPercent;
private List<String> missingItems;
private String connectionStatus;
private Integer runtimeStatus;
private String draftStatus;
private LocalDateTime updateTime;
```

Filter `ALL`, `INCOMPLETE`, `READY`, `ENABLED`, and `FAILED` on the backend. Compute completion from runtime rows plus current draft/test evidence; do not let the frontend duplicate the rules.

- [ ] **Step 3: Implement capability endpoints**

Map:

```java
platformClientFactory.getRegisteredPlatformCodes()
platformClientFactory.getRegisteredVendorDescriptors()
platformClientFactory.getVendorDescriptor(platformCode, vendorCode)
```

to safe admin responses. Build the platform capability list from the union of registered platform clients and vendor descriptors because a vendor-backed platform may exist without a native platform client. Expose descriptor fields, capabilities, governance policy, and config schema definitions, but never runtime credential values.

- [ ] **Step 4: Implement lifecycle methods**

- `enable`: require a published runtime configuration whose exact current fingerprint has valid test evidence.
- `disable`: set only the platform runtime status to disabled; keep configuration rows.
- `delete`: require disabled status, then transactionally soft-delete platform, platform vendors, platform adzones, managed platform rebate rules, and draft.
- Preserve global rebate rules, member-personal rules, orders, rebate records, settlements, and audit rows.

- [ ] **Step 5: Add controller routes and permission tests**

Controller contract:

```java
@RequestMapping("/cps/platform-onboarding")
class CpsPlatformOnboardingController {
    @GetMapping("/page")       // query
    @GetMapping("/get")        // query
    @PostMapping("/draft")     // create or update
    @DeleteMapping("/draft")   // delete
    @PostMapping("/validate")  // update
    @PostMapping("/test")      // test
    @PostMapping("/publish")   // publish
    @PutMapping("/enable")     // publish
    @PutMapping("/disable")    // update
    @DeleteMapping("/delete")  // delete
    @GetMapping("/platform-capabilities") // query
    @GetMapping("/vendor-descriptors")    // query
}
```

Use these exact permissions:

```text
cps:platform-onboarding:query
cps:platform-onboarding:create
cps:platform-onboarding:update
cps:platform-onboarding:delete
cps:platform-onboarding:test
cps:platform-onboarding:publish
```

Controller tests must assert every route has its intended `@PreAuthorize` expression and that response conversion does not expose AppSecret/AuthToken.

- [ ] **Step 6: Add menu and permission SQL**

Use a new stable menu ID range that does not collide with existing `6200–6296` rows. Add one visible “平台配置中心” menu under `6287` plus six action permissions. Change the four legacy menu rows to `visible=b'0'`; do not delete their permissions.

Apply identical menu meaning to `cps-all-in-one.sql` and a dated `2026-07-23` block in `cps-update.sql`.

- [ ] **Step 7: Run controller/lifecycle tests and SQL checks**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=CpsPlatformOnboardingControllerTest,CpsPlatformOnboardingLifecycleServiceTest,CpsPlatformOnboardingPublishDbTest" "-Dsurefire.failIfNoSpecifiedTests=false"
cd ..
python script/check_utf8_integrity.py backend/sql/module/cps-all-in-one.sql backend/sql/module/cps-update.sql
```

Expected: permissions, aggregate completion, lifecycle guards, tenant-scoped deletion, and SQL UTF-8 checks pass.

- [ ] **Step 8: Commit**

```text
Expose one governed platform configuration surface

Constraint: Unified actions must not bypass the four original configuration permission boundaries
Rejected: Reusing cps:platform:* alone | grants supplier and rebate mutation implicitly
Confidence: high
Scope-risk: moderate
Directive: Keep legacy endpoints hidden but available until migration rollback is no longer needed
Tested: Controller, lifecycle, publish, and SQL integrity tests
```

## Task 7: Add the typed frontend API and local form model

**Files:**

- Create: `frontend/admin-vue3/src/api/cps/platformOnboarding.ts`
- Create: `frontend/admin-vue3/src/views/cps/platformOnboarding/model.ts`
- Create: `script/test/test_admin_cps_platform_onboarding_ui_contract.py`

- [ ] **Step 1: Write the failing UI contract test**

```python
def test_platform_onboarding_api_contract_exists():
    source = read_utf8("frontend/admin-vue3/src/api/cps/platformOnboarding.ts")
    for route in [
        "/cps/platform-onboarding/page",
        "/cps/platform-onboarding/get",
        "/cps/platform-onboarding/draft",
        "/cps/platform-onboarding/validate",
        "/cps/platform-onboarding/test",
        "/cps/platform-onboarding/publish",
        "/cps/platform-onboarding/platform-capabilities",
        "/cps/platform-onboarding/vendor-descriptors",
    ]:
        assert route in source


def test_secret_fields_use_configured_flags():
    source = read_utf8("frontend/admin-vue3/src/api/cps/platformOnboarding.ts")
    assert "appSecretConfigured: boolean" in source
    assert "authTokenConfigured: boolean" in source
    assert "appSecret?: string" in source
```

- [ ] **Step 2: Run the test and verify missing files**

Run:

```bash
python -m pytest script/test/test_admin_cps_platform_onboarding_ui_contract.py -q
```

Expected: failure because the API and form model do not exist.

- [ ] **Step 3: Add complete TypeScript types**

Core draft type:

```ts
export interface PlatformOnboardingDraft {
  platformCode: string
  mode: 'CREATE' | 'RECONFIGURE'
  draftVersion?: number
  configFingerprint?: string
  validatedFingerprint?: string
  status: 'DRAFT' | 'VALIDATING' | 'READY' | 'FAILED' | 'PUBLISHED'
  platform: PlatformForm
  primaryVendorCode: string
  runtimeDefaultAdzoneId: string
  vendors: VendorForm[]
  adzones: AdzoneForm[]
  rebateRules: RebateRuleForm[]
  checkResult?: OnboardingCheckResult
}
```

API functions:

```ts
export const PlatformOnboardingApi = {
  getPage: (params: OnboardingPageReq) =>
    request.get<PageResult<OnboardingPageItem>>({ url: ROOT + '/page', params }),
  getDetail: (platformCode: string) =>
    request.get<PlatformOnboardingDraft>({ url: ROOT + '/get', params: { platformCode } }),
  saveDraft: (data: SaveDraftReq) =>
    request.post<PlatformOnboardingDraft>({ url: ROOT + '/draft', data }),
  deleteDraft: (platformCode: string) =>
    request.delete({ url: ROOT + '/draft', params: { platformCode } }),
  validate: (platformCode: string, draftVersion: number) =>
    request.post<OnboardingCheckResult>({ url: ROOT + '/validate', data: { platformCode, draftVersion } }),
  test: (platformCode: string, draftVersion: number) =>
    request.post<OnboardingCheckResult>({ url: ROOT + '/test', data: { platformCode, draftVersion } }),
  publish: (data: PublishReq) =>
    request.post<PlatformOnboardingDraft>({ url: ROOT + '/publish', data }),
  enable: (platformCode: string) =>
    request.put({ url: ROOT + '/enable', data: { platformCode } }),
  disable: (platformCode: string) =>
    request.put({ url: ROOT + '/disable', data: { platformCode } }),
  deleteBundle: (platformCode: string) =>
    request.delete({ url: ROOT + '/delete', params: { platformCode } }),
  getPlatformCapabilities: () =>
    request.get<PlatformCapability[]>({ url: ROOT + '/platform-capabilities' }),
  getVendorDescriptors: (platformCode: string) =>
    request.get<VendorDescriptor[]>({ url: ROOT + '/vendor-descriptors', params: { platformCode } })
}
```

- [ ] **Step 4: Add pure form helpers**

`model.ts` must export:

- `createEmptyDraft()`
- `normalizeDraftForSave()`
- `isDirty(original, current)`
- `completionLabel(item)`
- `stepForFieldPath(fieldPath)`
- `maskConfiguredSecrets(draft)`
- `amountCentToYuan(value)`
- `amountYuanToCent(value)`

No platform or vendor option arrays may be hard-coded here; options come from capability APIs. Keep all rebate amount conversion in `model.ts`: the API contract uses integer cents, while Element Plus inputs display yuan. Never scatter `* 100` or `/ 100` conversions across components.

- [ ] **Step 5: Run contract and TypeScript checks**

Run:

```bash
python -m pytest script/test/test_admin_cps_platform_onboarding_ui_contract.py -q
cd frontend/admin-vue3
pnpm ts:check
```

Expected: UI contract passes. `ts:check` passes, or any pre-existing unrelated failures are recorded separately with proof that the new files add no new errors.

- [ ] **Step 6: Commit**

```text
Give the unified workspace one typed backend contract

Constraint: Platform and vendor support must come from registered server capabilities
Rejected: Reusing four legacy API modules | preserves inconsistent hard-coded options
Confidence: high
Scope-risk: narrow
Directive: Never render stored credentials; use configured flags
Tested: UI contract test and TypeScript check
```

## Task 8: Build the platform configuration center list

**Files:**

- Create: `frontend/admin-vue3/src/views/cps/platformOnboarding/index.vue`
- Create: `frontend/admin-vue3/src/views/cps/platformOnboarding/components/CompletionBadge.vue`
- Modify: `script/test/test_admin_cps_platform_onboarding_ui_contract.py`

- [ ] **Step 1: Extend the failing contract test for list behavior**

```python
def test_platform_center_exposes_required_actions_and_permissions():
    source = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/index.vue")
    assert "接入新平台" in source
    assert "配置完整度" in source
    assert "连接状态" in source
    for permission in [
        "cps:platform-onboarding:create",
        "cps:platform-onboarding:update",
        "cps:platform-onboarding:test",
        "cps:platform-onboarding:publish",
        "cps:platform-onboarding:delete",
    ]:
        assert permission in source
```

- [ ] **Step 2: Run the contract test and verify failure**

Run:

```bash
python -m pytest script/test/test_admin_cps_platform_onboarding_ui_contract.py -q
```

Expected: failure because the list page and actions are missing.

- [ ] **Step 3: Implement list, filters, and actions**

The list page must:

- Query by keyword and status.
- Render platform, main vendor, backup count, default adzone, default rebate, completion, connection status, runtime status, and update time.
- Use `CompletionBadge.vue` to show percentage and missing items.
- Open create workspace with `router.replace({ query: { mode: 'create' } })`.
- Open edit workspace with `router.replace({ query: { mode: 'edit', platformCode } })`.
- Confirm disable, delete draft, and delete bundle separately.
- Refuse to show delete bundle as available while runtime status is enabled.
- Refresh only after successful actions.

Use:

```vue
<workspace
  v-if="route.query.mode"
  :platform-code="route.query.platformCode as string | undefined"
  :mode="route.query.mode as 'create' | 'edit'"
  @close="closeWorkspace"
  @published="handlePublished"
/>
<template v-else>
  <!-- filters and table -->
</template>
```

This keeps the backend-generated dynamic menu route stable and avoids a second hidden route.

- [ ] **Step 4: Run contract, typecheck, and lint for the new page**

Run:

```bash
python -m pytest script/test/test_admin_cps_platform_onboarding_ui_contract.py -q
cd frontend/admin-vue3
pnpm ts:check
pnpm lint:eslint
```

Expected: contract passes; no new TypeScript or ESLint errors.

- [ ] **Step 5: Commit**

```text
Make platform readiness visible from one operational list

Constraint: Completion rules remain server-owned
Rejected: Four independent menu lists | hides missing cross-configuration dependencies
Confidence: high
Scope-risk: moderate
Directive: Keep draft deletion distinct from runtime bundle deletion
Tested: UI contract, TypeScript, and ESLint checks
```

## Task 9: Build the five-step draft workspace

**Files:**

- Create: `frontend/admin-vue3/src/views/cps/platformOnboarding/workspace.vue`
- Create: all eleven components under `frontend/admin-vue3/src/views/cps/platformOnboarding/components/`
- Create: `frontend/admin-vue3/src/views/cps/components/adzoneRules.ts`
- Modify: `script/test/test_admin_cps_platform_onboarding_ui_contract.py`

- [ ] **Step 1: Add failing component contract tests**

```python
def test_workspace_has_five_steps_and_draft_publish_actions():
    source = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/workspace.vue")
    for title in ["平台信息", "API供应商", "推广位", "返利配置", "检测与启用"]:
        assert title in source
    for action in ["保存草稿", "连接检测", "发布但保持禁用", "发布并启用"]:
        assert action in source


def test_each_step_exposes_validate():
    for name in ["PlatformStep", "VendorStep", "AdzoneStep", "RebateStep"]:
        source = read_utf8(
            f"frontend/admin-vue3/src/views/cps/platformOnboarding/components/{name}.vue"
        )
        assert "defineExpose" in source
        assert "validate" in source
```

- [ ] **Step 2: Run the test and verify missing components**

Run:

```bash
python -m pytest script/test/test_admin_cps_platform_onboarding_ui_contract.py -q
```

Expected: failure because workspace components do not exist.

- [ ] **Step 3: Implement the workspace controller**

Required state:

```ts
const currentStep = ref(0)
const originalDraft = shallowRef<PlatformOnboardingDraft>()
const draft = ref<PlatformOnboardingDraft>(createEmptyDraft())
const saving = ref(false)
const testing = ref(false)
const publishing = ref(false)
const stepRefs = [platformRef, vendorRef, adzoneRef, rebateRef, reviewRef]
```

Required behavior:

- New mode validates prior steps before moving forward.
- Edit mode allows direct step navigation.
- `saveDraft()` sends the current `draftVersion`.
- Successful save replaces local version and clears stale check evidence.
- `runTest()` saves first when dirty, then tests that returned version.
- Publish sends `draftVersion` and `configFingerprint`.
- The publish buttons remain disabled unless `status === 'READY'` and both fingerprints match.
- `onBeforeRouteLeave` and close actions confirm unsaved changes.
- Validation errors use `stepForFieldPath()` to focus the relevant step.

- [ ] **Step 4: Implement platform and vendor steps**

`PlatformStep.vue`:

- Loads registered platform capabilities.
- Disables platform code changes in reconfigure mode.
- Shows a clear unsupported-adapter message.
- Contains name, logo, service rate, sort, and remark.

`VendorStep.vue`:

- Requires exactly one main vendor.
- Adds/removes optional backups.
- Renders credential fields from `VendorDescriptor.configSchema`.
- Uses configured flags for masked secrets.
- Allows enabled/disabled and priority changes.
- Provides a per-vendor test action whose response is displayed by `CheckResultPanel`.
- Uses `VendorEditorDialog.vue` for schema-driven add/edit so table rows never expose stored secrets.

- [ ] **Step 5: Implement adzone and rebate steps**

`AdzoneStep.vue`:

- Supports general, channel, and member rows.
- Requires one enabled general adzone as runtime default.
- Extracts type normalization, member/channel requirements, and batch-paste parsing into `frontend/admin-vue3/src/views/cps/components/adzoneRules.ts`, then reuses that module from both the new editor and the legacy page.
- Does not use the existing `AdzoneSelectDialog.vue` for draft creation because that dialog queries already-published rows.
- Uses `AdzoneEditorDialog.vue` for one row and `AdzoneBatchDialog.vue` for all-or-nothing local batch editing.
- Supports batch paste, but validates the entire draft locally; no legacy partial-success API is called.
- Clearly labels the one runtime default; do not expose three competing default concepts.

`RebateStep.vue`:

- Shows default platform rebate first.
- Hides member level, min/max, and priority under “高级设置”.
- Supports multiple level rules.
- Reuses `frontend/admin-vue3/src/views/member/level/components/MemberLevelSelect.vue` inside `RebateRuleDialog.vue`.
- Displays the rule matching explanation.
- Rejects global and personal-member scopes.

- [ ] **Step 6: Implement review and result components**

`ReviewStep.vue` renders normalized summaries and missing items. `CheckResultPanel.vue` renders:

```text
供应商 / 检测能力 / 成功或失败 / 耗时 / 脱敏原因 / 建议
```

It must never render request payload JSON or credential fields.

- [ ] **Step 7: Run contract, typecheck, and lint**

Run:

```bash
python -m pytest script/test/test_admin_cps_platform_onboarding_ui_contract.py -q
cd frontend/admin-vue3
pnpm ts:check
pnpm lint:eslint
```

Expected: all new component contracts pass and no new frontend static errors appear.

- [ ] **Step 8: Commit**

```text
Guide first-time setup while keeping expert reconfiguration fast

Constraint: Failed tests must remain saveable without affecting runtime configuration
Rejected: Wizard-only editing | makes single-field maintenance unnecessarily slow
Confidence: high
Scope-risk: broad
Directive: Save before test and publish only the matching validated fingerprint
Tested: UI contracts, TypeScript, and ESLint checks
```

## Task 10: Add deterministic Playwright coverage and menu migration checks

**Files:**

- Create: `frontend/admin-vue3/e2e/cps-platform-onboarding.spec.ts`
- Modify: `backend/sql/module/cps-all-in-one.sql`
- Modify: `backend/sql/module/cps-update.sql`
- Modify: `script/test/test_admin_cps_platform_onboarding_ui_contract.py`

- [ ] **Step 1: Add a failing menu SQL contract**

```python
def test_unified_menu_replaces_four_visible_entries():
    all_sql = read_utf8("backend/sql/module/cps-all-in-one.sql")
    update_sql = read_utf8("backend/sql/module/cps-update.sql")
    assert "平台配置中心" in all_sql
    assert "cps/platformOnboarding/index" in all_sql
    assert "cps:platform-onboarding:publish" in all_sql
    assert "2026-07-23" in update_sql
```

- [ ] **Step 2: Add Playwright scenarios with deterministic API fixtures**

Mock the admin bootstrap and onboarding endpoints with `page.route()`. `mockAdminBootstrapAndMenu(page)` must install a test token and intercept the login/user permission response plus the dynamic menu response containing `cps/platformOnboarding/index`; this keeps the test independent of a seeded admin database while still proving the real permission/menu path. Then cover:

```ts
test('saves a failed first-time setup as an incomplete draft', async ({ page }) => {
  await mockAdminBootstrapAndMenu(page)
  await mockOnboardingApi(page, { testStatus: 'FAILED' })
  await openPlatformCenter(page)
  await page.getByRole('button', { name: '接入新平台' }).click()
  await fillMinimumDraft(page)
  await page.getByRole('button', { name: '连接检测' }).click()
  await expect(page.getByText('连接检测失败')).toBeVisible()
  await page.getByRole('button', { name: '保存草稿' }).click()
  await expect(page.getByText('草稿已保存')).toBeVisible()
  await expect(page.getByRole('button', { name: '发布并启用' })).toBeDisabled()
})

test('keeps runtime summary until a reconfiguration draft is published', async ({ page }) => {
  await mockAdminBootstrapAndMenu(page)
  await mockOnboardingApi(page, { runtimeVendor: 'dataoke', draftVendor: 'haodanku' })
  await openReconfigureWorkspace(page, 'taobao')
  await expect(page.getByText('当前运行：大淘客')).toBeVisible()
  await expect(page.getByText('草稿配置：好单库')).toBeVisible()
  await runSuccessfulTest(page)
  await page.getByRole('button', { name: '发布并启用' }).click()
  await expect(page.getByText('当前运行：好单库')).toBeVisible()
})
```

Also cover:

- Resume draft.
- Add/remove backup vendor.
- Change runtime default adzone.
- Advanced rebate validation.
- Stale draft version conflict.
- Disable before delete.
- Permission-hidden actions.

Use Playwright `expect` for every acceptance result. Midscene is not required.

- [ ] **Step 3: Run Playwright and UI contracts**

Run:

```bash
python -m pytest script/test/test_admin_cps_platform_onboarding_ui_contract.py -q
cd frontend/admin-vue3
pnpm exec playwright test e2e/cps-platform-onboarding.spec.ts
```

Expected: deterministic mocked flows pass without requiring third-party CPS credentials.

- [ ] **Step 4: Run SQL UTF-8 and diff checks**

Run:

```bash
cd ../../
python script/check_utf8_integrity.py backend/sql/module/cps-all-in-one.sql backend/sql/module/cps-update.sql
git diff --check -- backend/sql/module/cps-all-in-one.sql backend/sql/module/cps-update.sql
```

- [ ] **Step 5: Commit**

```text
Make the unified configuration path the visible admin default

Constraint: Legacy APIs remain available for rollback while their menus are hidden
Rejected: Deleting legacy pages immediately | increases migration and rollback risk
Confidence: high
Scope-risk: moderate
Directive: Keep mocked Playwright outcomes deterministic and credential-free
Tested: Playwright onboarding flows, UI contract, and SQL integrity checks
```

## Task 11: Update project documentation and run the full verification gate

**Files:**

- Modify: `README.md`
- Modify: `docs/project-map.md`
- Modify: `docs/superpowers/specs/2026-07-23-platform-onboarding-center-design.md`

- [ ] **Step 1: Update current project guidance**

Document:

- New menu path `/cps-config/platform-onboarding`.
- New backend root `/admin-api/cps/platform-onboarding`.
- Draft/test/publish lifecycle.
- Runtime truth remains the four existing tables.
- Draft table and sensitive-field encryption.
- Legacy page menus are hidden but endpoints remain.
- New target test commands.

Do not describe future automatic vendor failover as implemented.

- [ ] **Step 2: Run the complete backend target suite**

Run:

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=CpsPlatformOnboardingDraftMapperTest,CpsPlatformOnboardingFingerprintTest,CpsPlatformOnboardingDraftServiceImplTest,CpsPlatformOnboardingValidatorTest,CpsPlatformOnboardingConnectionTesterTest,CpsPlatformOnboardingPublishDbTest,CpsPlatformOnboardingCacheInvalidatorTest,CpsPlatformOnboardingLifecycleServiceTest,CpsPlatformOnboardingControllerTest,CpsPlatformServiceImplTest,CpsApiVendorServiceImplTest,CpsAdzoneServiceImplTest,CpsRebateConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: all listed tests pass.

- [ ] **Step 3: Run frontend verification**

Run:

```bash
cd frontend/admin-vue3
pnpm ts:check
pnpm lint:eslint
pnpm exec playwright test e2e/cps-platform-onboarding.spec.ts
pnpm build:prod
```

Expected: checks and production build pass. If unrelated repository failures remain, capture exact command output and prove the onboarding files introduce no additional failures.

- [ ] **Step 4: Run repository integrity checks**

Run:

```bash
cd ../..
python -m pytest script/test/test_admin_cps_platform_onboarding_ui_contract.py -q
python script/check_utf8_integrity.py README.md docs/project-map.md backend/sql/module/cps-all-in-one.sql backend/sql/module/cps-update.sql
git diff --check
git status --short
```

Expected:

- UI contract passes.
- Changed Chinese files decode as UTF-8.
- `git diff --check` has no errors.
- `git status --short` contains only intended task changes plus preserved pre-existing user changes.

- [ ] **Step 5: Review against the design**

Verify each success criterion from `docs/superpowers/specs/2026-07-23-platform-onboarding-center-design.md`:

- One visible entry replaces four menus.
- First-time and reconfiguration workflows both work.
- Failed tests save disabled drafts.
- Runtime configuration remains unchanged until publish.
- Exact tested fingerprint is required for publish.
- Main vendor, runtime default adzone, and platform default rebate stay consistent.
- Configuration list and CRUD actions remain available.
- Tenant isolation, permissions, idempotency, rollback, cache invalidation, and credential masking have test evidence.

- [ ] **Step 6: Commit documentation and final verification evidence**

```text
Document the platform onboarding center as the supported configuration path

Constraint: Operators and future agents need one current workflow and one rollback boundary
Rejected: Leaving four-page instructions in project maps | recreates the original usability problem
Confidence: high
Scope-risk: narrow
Directive: Do not claim automatic vendor failover until runtime routing implements it
Tested: Backend target suite, frontend checks, Playwright, build, UTF-8, and diff integrity
Not-tested: Record any credential-gated real supplier smoke tests here
```

## Final handoff checklist

- [ ] Every task commit contains only task-owned files.
- [ ] No user changes were overwritten or newly staged.
- [ ] SQL baseline and incremental scripts are synchronized.
- [ ] The test database schema includes all five configuration tables.
- [ ] The final response lists changed files, verification evidence, and any real-credential smoke-test gap.
- [ ] If real supplier credentials are available, run a dedicated test-tenant smoke test after all local gates; never place credentials in commands, logs, docs, or commits.
