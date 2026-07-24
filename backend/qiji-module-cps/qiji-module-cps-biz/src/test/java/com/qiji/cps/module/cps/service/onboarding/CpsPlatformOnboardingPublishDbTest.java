package com.qiji.cps.module.cps.service.onboarding;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.framework.mybatis.core.util.MyBatisUtils;
import com.qiji.cps.framework.tenant.config.TenantProperties;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.qiji.cps.framework.test.core.ut.BaseDbUnitTest;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingCheckRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDetailRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingPublishReqVO;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.onboarding.CpsPlatformOnboardingDraftDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.vendor.CpsApiVendorDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import com.qiji.cps.module.cps.dal.mysql.onboarding.CpsPlatformOnboardingDraftMapper;
import com.qiji.cps.module.cps.dal.mysql.platform.CpsPlatformMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateConfigMapper;
import com.qiji.cps.module.cps.dal.mysql.vendor.CpsApiVendorMapper;
import com.qiji.cps.module.cps.enums.onboarding.CpsPlatformOnboardingModeEnum;
import com.qiji.cps.module.cps.enums.onboarding.CpsPlatformOnboardingStatusEnum;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingRebateRule;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingVendor;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import com.qiji.cps.module.cps.service.adzone.CpsAdzoneServiceImpl;
import com.qiji.cps.module.cps.service.platform.CpsPlatformServiceImpl;
import com.qiji.cps.module.cps.service.rebate.CpsRebateConfigServiceImpl;
import com.qiji.cps.module.cps.service.vendor.CpsApiVendorServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_DRAFT_NOT_EXISTS;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_CONFIG_INVALID;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_PUBLISH_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(CpsPlatformOnboardingPublishDbTest.TenantTestConfiguration.class)
class CpsPlatformOnboardingPublishDbTest extends BaseDbUnitTest {

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

    @Resource private CpsPlatformOnboardingDraftMapper draftMapper;
    @Resource private CpsPlatformMapper platformMapper;
    @Resource private CpsApiVendorMapper vendorMapper;
    @Resource private CpsAdzoneMapper adzoneMapper;
    @Resource private CpsRebateConfigMapper rebateMapper;
    @Resource private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CpsPlatformOnboardingFingerprint fingerprint;
    private CpsPlatformOnboardingService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        fingerprint = new CpsPlatformOnboardingFingerprint(objectMapper);
        CpsPlatformOnboardingDraftService draftService =
                new CpsPlatformOnboardingDraftServiceImpl(draftMapper, platformMapper, vendorMapper,
                        adzoneMapper, rebateMapper, objectMapper, fingerprint);
        CpsPlatformOnboardingValidator validator = mock(CpsPlatformOnboardingValidator.class);
        when(validator.validateNormalized(any())).thenAnswer(invocation -> {
            CpsPlatformOnboardingPayload payload = invocation.getArgument(0);
            if (payload.getVendors().stream().anyMatch(java.util.Objects::isNull)
                    || payload.getAdzones().stream().anyMatch(java.util.Objects::isNull)) {
                return new CpsPlatformOnboardingValidator.ValidationResult(
                        CpsPlatformOnboardingCheckRespVO.of(false, List.of()), null);
            }
            return new CpsPlatformOnboardingValidator.ValidationResult(
                    CpsPlatformOnboardingCheckRespVO.of(true, List.of()),
                    CpsPlatformOnboardingPayloadNormalizer.normalizeCopy(payload, objectMapper));
        });
        CpsApiVendorServiceImpl vendorService = new CpsApiVendorServiceImpl();
        ReflectionTestUtils.setField(vendorService, "vendorMapper", vendorMapper);
        ReflectionTestUtils.setField(vendorService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(vendorService, "vendorClients", List.of());
        CpsAdzoneServiceImpl adzoneService = new CpsAdzoneServiceImpl();
        ReflectionTestUtils.setField(adzoneService, "adzoneMapper", adzoneMapper);
        CpsRebateConfigServiceImpl rebateService = new CpsRebateConfigServiceImpl();
        ReflectionTestUtils.setField(rebateService, "rebateConfigMapper", rebateMapper);
        CpsPlatformServiceImpl platformService = new CpsPlatformServiceImpl();
        ReflectionTestUtils.setField(platformService, "platformMapper", platformMapper);
        ReflectionTestUtils.setField(platformService, "vendorService", vendorService);
        ReflectionTestUtils.setField(platformService, "adzoneService", adzoneService);
        CpsPlatformOnboardingService target = new CpsPlatformOnboardingServiceImpl(
                draftService, validator, fingerprint, platformService, vendorService,
                adzoneService, rebateService, mock(CpsPlatformOnboardingCacheInvalidator.class));
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        service = request -> transactionTemplate.execute(status -> target.publish(request));
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void publishReconfigure_shouldKeepRuntimeRowsUntilPublish() {
        seedRuntimeBundle("taobao", "dataoke", "old-pid", "20");
        saveReadyDraft(bundle("taobao", "haodanku", "new-pid", "35", true));

        assertEquals("dataoke", platformMapper.selectByPlatformCode("taobao").getActiveVendorCode());
        assertEquals("old-pid", platformMapper.selectByPlatformCode("taobao").getDefaultAdzoneId());
        assertNull(vendorMapper.selectByVendorAndPlatform("haodanku", "taobao"));
    }

    @Test
    void publishReconfigure_shouldSwitchAllFourGroupsTogether() {
        seedRuntimeBundle("taobao", "dataoke", "old-pid", "20");
        CpsPlatformOnboardingPublishReqVO request =
                saveReadyDraft(bundle("taobao", "haodanku", "new-pid", "35", true));

        service.publish(request);

        CpsPlatformDO platform = platformMapper.selectByPlatformCode("taobao");
        assertEquals("haodanku", platform.getActiveVendorCode());
        assertEquals("new-pid", platform.getDefaultAdzoneId());
        assertEquals("new-pid", vendorMapper
                .selectByVendorAndPlatform("haodanku", "taobao").getDefaultAdzoneId());
        assertEquals(0, rebateMapper.selectManagedRulesByPlatformCode("taobao").getFirst()
                .getRebateRate().compareTo(new BigDecimal("35")));
        assertNull(vendorMapper.selectByVendorAndPlatform("dataoke", "taobao"));
        assertEquals(1, adzoneMapper.selectAllByPlatformCode("taobao").size());
        assertEquals(CpsPlatformOnboardingStatusEnum.PUBLISHED.getCode(),
                draftMapper.selectByPlatformCode("taobao").getStatus());
    }

    @Test
    void publish_whenFinalPlatformWriteFails_shouldRollbackVendorAdzoneAndRebateWrites() {
        CpsPlatformOnboardingPayload invalidAtDatabase =
                bundle("jd", "jdunion", "jd-pid", "30", true);
        invalidAtDatabase.getPlatform().setPlatformName(null);
        CpsPlatformOnboardingPublishReqVO request = saveReadyDraft(invalidAtDatabase);

        assertThrows(Exception.class, () -> service.publish(request));

        assertEquals(0, vendorMapper.selectAllByPlatformCode("jd").size());
        assertEquals(0, adzoneMapper.selectAllByPlatformCode("jd").size());
        assertEquals(0, rebateMapper.selectManagedRulesByPlatformCode("jd").size());
        assertNull(platformMapper.selectByPlatformCode("jd"));
        assertEquals(CpsPlatformOnboardingStatusEnum.READY.getCode(),
                draftMapper.selectByPlatformCode("jd").getStatus());
    }

    @Test
    void publish_nullVendor_shouldRejectBeforeRuntimeWrites() {
        CpsPlatformOnboardingPayload payload = bundle("jd", "jdunion", "jd-pid", "30", true);
        payload.getVendors().set(0, null);
        CpsPlatformOnboardingPublishReqVO request = saveReadyDraft(payload);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.publish(request));

        assertEquals(ONBOARDING_CONFIG_INVALID.getCode(), exception.getCode());
        assertNull(platformMapper.selectByPlatformCode("jd"));
        assertEquals(0, vendorMapper.selectAllByPlatformCode("jd").size());
        assertEquals(0, adzoneMapper.selectAllByPlatformCode("jd").size());
        assertEquals(0, rebateMapper.selectManagedRulesByPlatformCode("jd").size());
    }

    @Test
    void publish_nullAdzone_shouldRejectBeforeRuntimeWrites() {
        CpsPlatformOnboardingPayload payload = bundle("jd", "jdunion", "jd-pid", "30", true);
        payload.getAdzones().set(0, null);
        CpsPlatformOnboardingPublishReqVO request = saveReadyDraft(payload);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.publish(request));

        assertEquals(ONBOARDING_CONFIG_INVALID.getCode(), exception.getCode());
        assertNull(platformMapper.selectByPlatformCode("jd"));
        assertEquals(0, vendorMapper.selectAllByPlatformCode("jd").size());
        assertEquals(0, adzoneMapper.selectAllByPlatformCode("jd").size());
        assertEquals(0, rebateMapper.selectManagedRulesByPlatformCode("jd").size());
    }

    @Test
    void publishSameVersionTwice_shouldBeIdempotent() {
        CpsPlatformOnboardingPublishReqVO request =
                saveReadyDraft(bundle("jd", "jdunion", "jd-pid", "30", true));

        service.publish(request);
        CpsApiVendorDO runtimeVendor = vendorMapper.selectByVendorAndPlatform("jdunion", "jd");
        runtimeVendor.setVendorName("runtime-edited");
        vendorMapper.updateById(runtimeVendor);

        CpsPlatformOnboardingDetailRespVO response = service.publish(request);

        assertNotNull(platformMapper.selectByPlatformCode("jd"));
        assertEquals(1, vendorMapper.selectAllByPlatformCode("jd").size());
        assertEquals(1, adzoneMapper.selectAllByPlatformCode("jd").size());
        assertEquals(1, rebateMapper.selectManagedRulesByPlatformCode("jd").size());
        assertEquals("runtime-edited", response.getPayload().getVendors().getFirst().getVendorName());
        assertTrue(response.getPayload().getVendors().getFirst().getAppSecretConfigured());
    }

    @Test
    void publishWithDifferentVersion_shouldRejectBeforeRuntimeWrites() {
        CpsPlatformOnboardingPublishReqVO request =
                saveReadyDraft(bundle("jd", "jdunion", "jd-pid", "30", true));
        request.setDraftVersion(2L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.publish(request));

        assertEquals(ONBOARDING_PUBLISH_CONFLICT.getCode(), exception.getCode());
        assertNull(platformMapper.selectByPlatformCode("jd"));
        assertEquals(0, vendorMapper.selectAllByPlatformCode("jd").size());
    }

    @Test
    void publishWithDifferentRequestFingerprint_shouldRejectBeforeRuntimeWrites() {
        CpsPlatformOnboardingPublishReqVO request =
                saveReadyDraft(bundle("jd", "jdunion", "jd-pid", "30", true));
        request.setConfigFingerprint("different-request-fingerprint");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.publish(request));

        assertEquals(ONBOARDING_PUBLISH_CONFLICT.getCode(), exception.getCode());
        assertNull(platformMapper.selectByPlatformCode("jd"));
        assertEquals(0, vendorMapper.selectAllByPlatformCode("jd").size());
    }

    @Test
    void publishWithDifferentValidatedFingerprint_shouldRejectBeforeRuntimeWrites() {
        CpsPlatformOnboardingPublishReqVO request =
                saveReadyDraft(bundle("jd", "jdunion", "jd-pid", "30", true));
        CpsPlatformOnboardingDraftDO draft = draftMapper.selectByPlatformCode("jd");
        draft.setValidatedFingerprint("different-validated-fingerprint");
        draftMapper.updateById(draft);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.publish(request));

        assertEquals(ONBOARDING_PUBLISH_CONFLICT.getCode(), exception.getCode());
        assertNull(platformMapper.selectByPlatformCode("jd"));
        assertEquals(0, vendorMapper.selectAllByPlatformCode("jd").size());
    }

    @Test
    void publishWithPayloadFingerprintDrift_shouldRejectBeforeRuntimeWrites() {
        CpsPlatformOnboardingPublishReqVO request =
                saveReadyDraft(bundle("jd", "jdunion", "jd-pid", "30", true));
        CpsPlatformOnboardingDraftDO draft = draftMapper.selectByPlatformCode("jd");
        draft.setPayloadCiphertext(writePayload(
                bundle("jd", "jdunion", "different-pid", "30", true)));
        draftMapper.updateById(draft);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.publish(request));

        assertEquals(ONBOARDING_PUBLISH_CONFLICT.getCode(), exception.getCode());
        assertNull(platformMapper.selectByPlatformCode("jd"));
        assertEquals(0, vendorMapper.selectAllByPlatformCode("jd").size());
    }

    @Test
    void publish_shouldNotReadOrWriteAnotherTenantBundle() {
        saveReadyDraft(bundle("pdd", "official", "pdd-pid", "25", true));
        TenantContextHolder.setTenantId(2L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.publish(publishRequest("pdd", 1L, "irrelevant", true)));

        assertEquals(ONBOARDING_DRAFT_NOT_EXISTS.getCode(), exception.getCode());
        assertNull(platformMapper.selectByPlatformCode("pdd"));
    }

    @Test
    void publish_shouldKeepPersonalRebateRulesOutsideOnboardingOwnership() {
        seedRuntimeBundle("taobao", "dataoke", "old-pid", "20");
        CpsRebateConfigDO personal = rebate("taobao", "90", 10);
        personal.setMemberId(99L);
        rebateMapper.insert(personal);
        CpsPlatformOnboardingPublishReqVO request =
                saveReadyDraft(bundle("taobao", "haodanku", "new-pid", "35", true));

        service.publish(request);

        assertNotNull(rebateMapper.selectById(personal.getId()));
        assertEquals(1, rebateMapper.selectManagedRulesByPlatformCode("taobao").size());
    }

    @Test
    void publishBlankVendorSecrets_shouldPreserveRuntimeCredentials() {
        seedRuntimeBundle("taobao", "dataoke", "old-pid", "20");
        CpsPlatformOnboardingPayload payload = bundle("taobao", "dataoke", "new-pid", "35", true);
        payload.getVendors().getFirst().setAppSecret(" ");
        payload.getVendors().getFirst().setAuthToken(null);
        CpsPlatformOnboardingPublishReqVO request = saveReadyDraft(payload);

        service.publish(request);

        CpsApiVendorDO vendor = vendorMapper.selectByVendorAndPlatform("dataoke", "taobao");
        assertEquals("runtime-secret", vendor.getAppSecret());
        assertEquals("runtime-token", vendor.getAuthToken());
    }

    private CpsPlatformOnboardingPublishReqVO saveReadyDraft(CpsPlatformOnboardingPayload payload) {
        String configFingerprint = fingerprint.calculate(payload);
        CpsPlatformOnboardingDraftDO draft = CpsPlatformOnboardingDraftDO.builder()
                .platformCode(payload.getPlatform().getPlatformCode())
                .mode(platformMapper.selectByPlatformCode(payload.getPlatform().getPlatformCode()) == null
                        ? CpsPlatformOnboardingModeEnum.CREATE.getCode()
                        : CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode())
                .payloadCiphertext(writePayload(payload))
                .draftVersion(1)
                .configFingerprint(configFingerprint)
                .validatedFingerprint(configFingerprint)
                .status(CpsPlatformOnboardingStatusEnum.READY.getCode())
                .build();
        draft.setTenantId(TenantContextHolder.getRequiredTenantId());
        draftMapper.insert(draft);
        return publishRequest(payload.getPlatform().getPlatformCode(), 1L, configFingerprint, true);
    }

    private CpsPlatformOnboardingPublishReqVO publishRequest(
            String platformCode, Long version, String configFingerprint, boolean enable) {
        CpsPlatformOnboardingPublishReqVO request = new CpsPlatformOnboardingPublishReqVO();
        request.setPlatformCode(platformCode);
        request.setDraftVersion(version);
        request.setConfigFingerprint(configFingerprint);
        request.setEnableAfterPublish(enable);
        return request;
    }

    private CpsPlatformOnboardingPayload bundle(
            String platformCode, String vendorCode, String adzoneId,
            String rebateRate, boolean enabled) {
        CpsPlatformSaveReqVO platform = new CpsPlatformSaveReqVO();
        platform.setPlatformCode(platformCode);
        platform.setPlatformName(platformCode + " platform");
        platform.setDefaultAdzoneId(adzoneId);
        platform.setActiveVendorCode(vendorCode);
        platform.setStatus(enabled ? 1 : 0);
        CpsOnboardingVendor vendor = CpsOnboardingVendor.builder()
                .vendorCode(vendorCode).vendorName(vendorCode).vendorType("official")
                .platformCode(platformCode).appKey("new-key").appSecret("new-secret")
                .authToken("new-token").apiBaseUrl("https://api.example.test")
                .defaultAdzoneId(adzoneId).priority(100).status(1).build();
        CpsOnboardingAdzone adzone = CpsOnboardingAdzone.builder()
                .platformCode(platformCode).adzoneId(adzoneId).adzoneName(adzoneId)
                .adzoneType("general").isDefault(1).status(1).build();
        CpsOnboardingRebateRule rebate = CpsOnboardingRebateRule.builder()
                .platformCode(platformCode).rebateRate(new BigDecimal(rebateRate))
                .status(1).priority(0).build();
        return CpsPlatformOnboardingPayload.builder()
                .platform(platform).primaryVendorCode(vendorCode)
                .runtimeDefaultAdzoneId(adzoneId)
                .vendors(new ArrayList<>(List.of(vendor)))
                .adzones(new ArrayList<>(List.of(adzone)))
                .rebateRules(new ArrayList<>(List.of(rebate))).build();
    }

    private void seedRuntimeBundle(
            String platformCode, String vendorCode, String adzoneId, String rebateRate) {
        CpsApiVendorDO vendor = CpsApiVendorDO.builder()
                .vendorCode(vendorCode).vendorName(vendorCode).vendorType("aggregator")
                .platformCode(platformCode).appKey("runtime-key").appSecret("runtime-secret")
                .authToken("runtime-token").apiBaseUrl("https://old.example.test")
                .defaultAdzoneId(adzoneId).priority(100).status(1).build();
        vendor.setTenantId(TenantContextHolder.getRequiredTenantId());
        vendorMapper.insert(vendor);
        CpsAdzoneDO adzone = CpsAdzoneDO.builder()
                .platformCode(platformCode).adzoneId(adzoneId).adzoneName(adzoneId)
                .adzoneType("general").isDefault(1).status(1).build();
        adzone.setTenantId(TenantContextHolder.getRequiredTenantId());
        adzoneMapper.insert(adzone);
        rebateMapper.insert(rebate(platformCode, rebateRate, 0));
        CpsPlatformDO platform = CpsPlatformDO.builder()
                .platformCode(platformCode).platformName(platformCode + " runtime")
                .activeVendorCode(vendorCode).defaultAdzoneId(adzoneId).status(1).build();
        platform.setTenantId(TenantContextHolder.getRequiredTenantId());
        platformMapper.insert(platform);
    }

    private CpsRebateConfigDO rebate(String platformCode, String rebateRate, int priority) {
        CpsRebateConfigDO rebate = CpsRebateConfigDO.builder()
                .platformCode(platformCode).rebateRate(new BigDecimal(rebateRate))
                .status(1).priority(priority).build();
        rebate.setTenantId(TenantContextHolder.getRequiredTenantId());
        return rebate;
    }

    private String writePayload(CpsPlatformOnboardingPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

}
