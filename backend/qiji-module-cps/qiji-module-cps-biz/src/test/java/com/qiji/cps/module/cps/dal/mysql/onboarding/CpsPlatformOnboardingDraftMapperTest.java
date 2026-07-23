package com.qiji.cps.module.cps.dal.mysql.onboarding;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.qiji.cps.framework.mybatis.core.util.MyBatisUtils;
import com.qiji.cps.framework.tenant.config.TenantProperties;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.qiji.cps.framework.test.core.ut.BaseDbUnitTest;
import com.qiji.cps.module.cps.dal.dataobject.onboarding.CpsPlatformOnboardingDraftDO;
import com.qiji.cps.module.cps.enums.onboarding.CpsPlatformOnboardingModeEnum;
import com.qiji.cps.module.cps.enums.onboarding.CpsPlatformOnboardingStatusEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(CpsPlatformOnboardingDraftMapperTest.TenantTestConfiguration.class)
class CpsPlatformOnboardingDraftMapperTest extends BaseDbUnitTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantTestConfiguration {

        @Bean
        TenantLineInnerInterceptor tenantLineInnerInterceptor(MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(
                    new TenantDatabaseInterceptor(new TenantProperties()));
            MyBatisUtils.addInterceptor(interceptor, tenantInterceptor, 0);
            return tenantInterceptor;
        }

    }

    @Resource
    private CpsPlatformOnboardingDraftMapper draftMapper;
    @Resource
    private DataSource dataSource;
    @Resource
    private TenantLineInnerInterceptor tenantLineInnerInterceptor;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void selectByPlatformCode_shouldIsolateTenant() {
        CpsPlatformOnboardingDraftDO tenantOneDraft = newDraft("taobao", "{\"appKey\":\"tenant-one\"}");
        draftMapper.insert(tenantOneDraft);

        TenantContextHolder.setTenantId(2L);
        CpsPlatformOnboardingDraftDO tenantTwoDraft = newDraft("taobao", "{\"appKey\":\"tenant-two\"}");
        draftMapper.insert(tenantTwoDraft);

        TenantContextHolder.setTenantId(1L);
        assertEquals(tenantOneDraft.getId(), draftMapper.selectByPlatformCode("taobao").getId());

        TenantContextHolder.setTenantId(2L);
        assertEquals(tenantTwoDraft.getId(), draftMapper.selectByPlatformCode("taobao").getId());
    }

    @Test
    void updatePayload_whenVersionChanged_shouldRejectStaleWriter() {
        CpsPlatformOnboardingDraftDO draft = newDraft("taobao", "{\"appKey\":\"before\"}");
        draftMapper.insert(draft);

        int updated = draftMapper.updatePayload(
                draft.getId(),
                99,
                "{\"appKey\":\"after\"}",
                "new-fingerprint",
                CpsPlatformOnboardingStatusEnum.DRAFT.getCode());

        assertEquals(0, updated);
        CpsPlatformOnboardingDraftDO unchanged = draftMapper.selectById(draft.getId());
        assertEquals(1, unchanged.getDraftVersion());
        assertEquals("{\"appKey\":\"before\"}", unchanged.getPayloadCiphertext());
        assertEquals("fingerprint", unchanged.getConfigFingerprint());
    }

    @Test
    void updatePayload_shouldEncryptAndResetPriorValidationState() {
        LocalDateTime previousValidationTime = LocalDateTime.of(2026, 7, 22, 10, 0);
        CpsPlatformOnboardingDraftDO draft = newDraft("taobao", "{\"appSecret\":\"before\"}");
        draft.setValidatedFingerprint("validated-fingerprint");
        draft.setValidatedAt(previousValidationTime);
        draft.setCheckSummary("validation passed");
        draft.setPublishedAt(previousValidationTime.plusHours(1));
        draft.setStatus(CpsPlatformOnboardingStatusEnum.FAILED.getCode());
        draftMapper.insert(draft);

        String updatedPayload = "{\"appSecret\":\"after-secret\"}";
        int updated = draftMapper.updatePayload(
                draft.getId(),
                1,
                updatedPayload,
                "updated-fingerprint",
                CpsPlatformOnboardingStatusEnum.DRAFT.getCode());

        assertEquals(1, updated);
        CpsPlatformOnboardingDraftDO reloaded = draftMapper.selectById(draft.getId());
        assertEquals(2, reloaded.getDraftVersion());
        assertEquals(updatedPayload, reloaded.getPayloadCiphertext());
        assertEquals("updated-fingerprint", reloaded.getConfigFingerprint());
        assertEquals(CpsPlatformOnboardingStatusEnum.DRAFT.getCode(), reloaded.getStatus());
        assertNull(reloaded.getValidatedFingerprint());
        assertNull(reloaded.getValidatedAt());
        assertNull(reloaded.getCheckSummary());
        assertNull(reloaded.getPublishedAt());

        String storedValue = queryString(
                "SELECT payload_ciphertext FROM cps_platform_onboarding_draft WHERE id = ?",
                draft.getId());
        assertNotEquals(updatedPayload, storedValue);
        assertFalse(storedValue.contains("after-secret"));
    }

    @Test
    void updatePayload_whenTenantDiffers_shouldRejectCrossTenantWriter() {
        CpsPlatformOnboardingDraftDO tenantOneDraft = newDraft("taobao", "{\"appKey\":\"tenant-one\"}");
        draftMapper.insert(tenantOneDraft);

        TenantContextHolder.setTenantId(2L);
        int updated = draftMapper.updatePayload(
                tenantOneDraft.getId(),
                1,
                "{\"appKey\":\"tenant-two\"}",
                "tenant-two-fingerprint",
                CpsPlatformOnboardingStatusEnum.DRAFT.getCode());

        assertEquals(0, updated);
        TenantContextHolder.setTenantId(1L);
        CpsPlatformOnboardingDraftDO unchanged = draftMapper.selectById(tenantOneDraft.getId());
        assertEquals(1, unchanged.getDraftVersion());
        assertEquals("{\"appKey\":\"tenant-one\"}", unchanged.getPayloadCiphertext());
        assertEquals("fingerprint", unchanged.getConfigFingerprint());
    }

    @Test
    void payloadCiphertext_shouldEncryptAtRestAndRoundTrip() {
        String plaintext = "{\"appSecret\":\"secret-value\"}";
        CpsPlatformOnboardingDraftDO draft = newDraft("taobao", plaintext);
        draftMapper.insert(draft);

        String storedValue = queryString(
                "SELECT payload_ciphertext FROM cps_platform_onboarding_draft WHERE id = ?",
                draft.getId());

        assertNotNull(storedValue);
        assertNotEquals(plaintext, storedValue);
        assertFalse(storedValue.contains("secret-value"));
        assertEquals(plaintext, draftMapper.selectById(draft.getId()).getPayloadCiphertext());
    }

    @Test
    void repeatedDeleteAndRecreate_shouldKeepHistoricalRowsWithoutUniqueConflict() {
        CpsPlatformOnboardingDraftDO first = newDraft("taobao", "{\"attempt\":1}");
        draftMapper.insert(first);
        assertEquals(1, draftMapper.deleteById(first.getId()));

        CpsPlatformOnboardingDraftDO second = newDraft("taobao", "{\"attempt\":2}");
        draftMapper.insert(second);
        assertEquals(1, draftMapper.deleteById(second.getId()));

        CpsPlatformOnboardingDraftDO active = newDraft("taobao", "{\"attempt\":3}");
        draftMapper.insert(active);

        int historicalCount = queryInt(
                "SELECT COUNT(*) FROM cps_platform_onboarding_draft "
                        + "WHERE tenant_id = 1 AND platform_code = 'taobao'");
        int activeCount = queryInt(
                "SELECT COUNT(*) FROM cps_platform_onboarding_draft "
                        + "WHERE tenant_id = 1 AND platform_code = 'taobao' AND deleted = FALSE");

        assertEquals(3, historicalCount);
        assertEquals(1, activeCount);
        assertEquals(active.getId(), draftMapper.selectByPlatformCode("taobao").getId());
    }

    private CpsPlatformOnboardingDraftDO newDraft(String platformCode, String payload) {
        CpsPlatformOnboardingDraftDO draft = CpsPlatformOnboardingDraftDO.builder()
                .platformCode(platformCode)
                .mode(CpsPlatformOnboardingModeEnum.CREATE.getCode())
                .payloadCiphertext(payload)
                .draftVersion(1)
                .configFingerprint("fingerprint")
                .status(CpsPlatformOnboardingStatusEnum.DRAFT.getCode())
                .build();
        draft.setTenantId(TenantContextHolder.getRequiredTenantId());
        return draft;
    }

    private String queryString(String sql, Object... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new AssertionError("Expected one JDBC row");
                }
                return resultSet.getString(1);
            }
        } catch (SQLException exception) {
            throw new AssertionError("Raw JDBC query failed", exception);
        }
    }

    private int queryInt(String sql, Object... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new AssertionError("Expected one JDBC row");
                }
                return resultSet.getInt(1);
            }
        } catch (SQLException exception) {
            throw new AssertionError("Raw JDBC query failed", exception);
        }
    }

    private void bind(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }

}
