package com.qiji.cps.module.cps.service.onboarding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDetailRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDraftSaveReqVO;
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
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_CONFIG_INVALID;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_DRAFT_NOT_EXISTS;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_DRAFT_VERSION_CONFLICT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CpsPlatformOnboardingDraftServiceImplTest {

    @Mock
    private CpsPlatformOnboardingDraftMapper draftMapper;
    @Mock
    private CpsPlatformMapper platformMapper;
    @Mock
    private CpsApiVendorMapper vendorMapper;
    @Mock
    private CpsAdzoneMapper adzoneMapper;
    @Mock
    private CpsRebateConfigMapper rebateMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CpsPlatformOnboardingDraftServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CpsPlatformOnboardingDraftServiceImpl(
                draftMapper, platformMapper, vendorMapper, adzoneMapper, rebateMapper,
                objectMapper, new CpsPlatformOnboardingFingerprint(objectMapper));
    }

    @Test
    void saveDraft_shouldResetValidationAndIncrementVersion() throws Exception {
        CpsPlatformOnboardingDraftDO existing = persistedDraft(
                8L, 4, CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode(),
                CpsPlatformOnboardingStatusEnum.READY.getCode(),
                CpsPlatformOnboardingTestFixtures.validPayload());
        existing.setValidatedFingerprint("old-fingerprint");
        existing.setValidatedAt(LocalDateTime.of(2026, 7, 23, 10, 0));
        existing.setCheckSummary("old result");
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(existing);
        when(draftMapper.updatePayload(eq(8L), eq(4), anyString(), anyString(), eq("DRAFT")))
                .thenReturn(1);

        CpsPlatformOnboardingDetailRespVO saved = service.saveDraft(
                saveRequest("taobao", 4L, CpsPlatformOnboardingTestFixtures.validPayload()));

        assertEquals(5L, saved.getDraftVersion());
        assertEquals(CpsPlatformOnboardingStatusEnum.DRAFT.getCode(), saved.getStatus());
        assertNull(saved.getValidatedFingerprint());
        assertNull(saved.getValidatedAt());
        assertNull(saved.getCheckSummary());
        assertTrue(saved.getPayload().getVendors().stream()
                .allMatch(vendor -> Boolean.TRUE.equals(vendor.getAppSecretConfigured())
                        && Boolean.TRUE.equals(vendor.getAuthTokenConfigured())));
    }

    @Test
    void saveDraft_whenVersionIsStale_shouldFailWithoutOverwrite() throws Exception {
        CpsPlatformOnboardingDraftDO existing = persistedDraft(
                8L, 6, CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode(),
                CpsPlatformOnboardingStatusEnum.DRAFT.getCode(),
                CpsPlatformOnboardingTestFixtures.validPayload());
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(existing);
        when(draftMapper.updatePayload(eq(8L), eq(5), anyString(), anyString(), eq("DRAFT")))
                .thenReturn(0);

        assertServiceCode(ONBOARDING_DRAFT_VERSION_CONFLICT.getCode(),
                () -> service.saveDraft(saveRequest(
                        "taobao", 5L, CpsPlatformOnboardingTestFixtures.validPayload())));

        verify(draftMapper).updatePayload(eq(8L), eq(5), anyString(), anyString(), eq("DRAFT"));
        verifyNoRuntimeWrites();
    }

    @Test
    void getDetail_withoutDraft_shouldBuildReconfigurePayloadFromRuntimeTables() {
        CpsPlatformDO platform = CpsPlatformDO.builder()
                .id(7L)
                .platformCode("jd")
                .platformName("京东联盟")
                .extraConfig("{\"platformSecret\":\"runtime-platform-extra\"}")
                .activeVendorCode("official")
                .defaultAdzoneId("jd-pid")
                .status(0)
                .build();
        CpsApiVendorDO vendor = CpsApiVendorDO.builder()
                .vendorCode("official")
                .vendorName("京东官方")
                .vendorType("official")
                .platformCode("jd")
                .appKey("runtime-key")
                .appSecret("runtime-secret")
                .authToken("runtime-token")
                .extraConfig("{\"vendorSecret\":\"runtime-vendor-extra\"}")
                .status(0)
                .build();
        when(draftMapper.selectByPlatformCode("jd")).thenReturn(null);
        when(platformMapper.selectByPlatformCode("jd")).thenReturn(platform);
        when(vendorMapper.selectAllByPlatformCode("jd")).thenReturn(List.of(vendor));
        when(adzoneMapper.selectAllByPlatformCode("jd")).thenReturn(List.of(
                CpsAdzoneDO.builder().platformCode("jd").adzoneId("jd-pid").isDefault(1).status(0).build()));
        when(rebateMapper.selectManagedRulesByPlatformCode("jd")).thenReturn(List.of(
                CpsRebateConfigDO.builder().platformCode("jd").memberId(null).memberLevelId(null)
                        .rebateRate(new java.math.BigDecimal("60")).status(1).build(),
                CpsRebateConfigDO.builder().platformCode("jd").memberId(null).memberLevelId(10L)
                        .rebateRate(new java.math.BigDecimal("70")).status(1).build(),
                CpsRebateConfigDO.builder().platformCode("jd").memberId(null).memberLevelId(20L)
                        .rebateRate(new java.math.BigDecimal("80")).status(0).build()));

        CpsPlatformOnboardingDetailRespVO result = service.getDetail(" JD ");

        assertEquals(CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode(), result.getMode());
        assertEquals("jd", result.getPayload().getPlatform().getPlatformCode());
        assertNull(result.getDraftVersion());
        assertTrue(result.getPayload().getVendors().get(0).getAppSecretConfigured());
        assertTrue(result.getPayload().getVendors().get(0).getAuthTokenConfigured());
        assertEquals(1, result.getPayload().getAdzones().size());
        assertEquals(3, result.getPayload().getRebateRules().size());
        assertTrue(result.getPayload().getRebateRules().stream()
                .allMatch(rule -> rule.getMemberId() == null));
        assertTrue(result.getPayload().getRebateRules().stream()
                .anyMatch(rule -> rule.getMemberLevelId() == null && rule.getStatus() == 1));
        assertTrue(result.getPayload().getRebateRules().stream()
                .anyMatch(rule -> Long.valueOf(20L).equals(rule.getMemberLevelId())
                        && rule.getStatus() == 0));
        assertFalse(result.toString().contains("runtime-secret"));
        assertFalse(result.toString().contains("runtime-token"));
        verify(draftMapper, never()).insert(any(CpsPlatformOnboardingDraftDO.class));
    }

    @Test
    void saveDraft_shouldCreateFirstPlatformDraft() {
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(platformMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(draftMapper.insert(any(CpsPlatformOnboardingDraftDO.class))).thenAnswer(invocation -> {
            invocation.<CpsPlatformOnboardingDraftDO>getArgument(0).setId(11L);
            return 1;
        });

        CpsPlatformOnboardingDetailRespVO saved = service.saveDraft(
                saveRequest(" TAOBAO ", null, CpsPlatformOnboardingTestFixtures.validPayload()));

        ArgumentCaptor<CpsPlatformOnboardingDraftDO> captor =
                ArgumentCaptor.forClass(CpsPlatformOnboardingDraftDO.class);
        verify(draftMapper).insert(captor.capture());
        assertEquals(CpsPlatformOnboardingModeEnum.CREATE.getCode(), captor.getValue().getMode());
        assertEquals(1, captor.getValue().getDraftVersion());
        assertEquals(CpsPlatformOnboardingStatusEnum.DRAFT.getCode(), captor.getValue().getStatus());
        assertEquals(1L, saved.getDraftVersion());
        verifyNoRuntimeWrites();
    }

    @Test
    void getDetail_jsonShouldOmitSecretKeysAndExposeConfiguredFlags() throws Exception {
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(persistedDraft(
                8L, 3, CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode(),
                CpsPlatformOnboardingStatusEnum.DRAFT.getCode(),
                CpsPlatformOnboardingTestFixtures.validPayload()));

        String json = objectMapper.writeValueAsString(service.getDetail("taobao"));
        JsonNode platform = objectMapper.readTree(json).path("payload").path("platform");
        JsonNode vendor = objectMapper.readTree(json).path("payload").path("vendors").get(0);

        assertFalse(platform.has("extraConfig"));
        assertTrue(platform.path("extraConfigConfigured").asBoolean());
        assertFalse(vendor.has("appKey"));
        assertFalse(vendor.has("appSecret"));
        assertFalse(vendor.has("authToken"));
        assertFalse(vendor.has("extraConfig"));
        assertTrue(vendor.path("appKeyConfigured").asBoolean());
        assertTrue(vendor.path("appSecretConfigured").asBoolean());
        assertTrue(vendor.path("authTokenConfigured").asBoolean());
        assertTrue(vendor.path("extraConfigConfigured").asBoolean());
        assertFalse(json.contains("dataoke-key"));
        assertFalse(json.contains("dataoke-secret"));
        assertFalse(json.contains("dataoke-token"));
        assertFalse(json.contains("\"source\":\"onboarding\""));
        assertFalse(json.contains("\"vendor\":\"dataoke\""));
    }

    @Test
    void saveDraft_withoutStoredDraftButWithVersion_shouldRejectAsConflict() {
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(null);

        assertServiceCode(ONBOARDING_DRAFT_VERSION_CONFLICT.getCode(),
                () -> service.saveDraft(saveRequest(
                        "taobao", 2L, CpsPlatformOnboardingTestFixtures.validPayload())));

        verify(draftMapper, never()).insert(any(CpsPlatformOnboardingDraftDO.class));
        verifyNoInteractions(platformMapper);
        verifyNoRuntimeWrites();
    }

    @Test
    void saveDraft_whenConcurrentFirstInsertWins_shouldTranslateDuplicateKeyToConflict() {
        when(draftMapper.selectByPlatformCode("taobao"))
                .thenReturn(null)
                .thenReturn(CpsPlatformOnboardingDraftDO.builder()
                        .id(12L).platformCode("taobao").draftVersion(1).build());
        when(platformMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(draftMapper.insert(any(CpsPlatformOnboardingDraftDO.class)))
                .thenThrow(new DuplicateKeyException("uq_tenant_platform"));

        assertServiceCode(ONBOARDING_DRAFT_VERSION_CONFLICT.getCode(),
                () -> service.saveDraft(saveRequest(
                        "taobao", null, CpsPlatformOnboardingTestFixtures.validPayload())));

        verifyNoRuntimeWrites();
    }

    @Test
    void saveDraft_whenDuplicateKeyHasNoActiveTenantDraft_shouldRethrowOriginalException() {
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(null).thenReturn(null);
        when(platformMapper.selectByPlatformCode("taobao")).thenReturn(null);
        DuplicateKeyException duplicate = new DuplicateKeyException("unrelated duplicate");
        when(draftMapper.insert(any(CpsPlatformOnboardingDraftDO.class))).thenThrow(duplicate);

        DuplicateKeyException thrown = assertThrows(DuplicateKeyException.class,
                () -> service.saveDraft(saveRequest(
                        "taobao", null, CpsPlatformOnboardingTestFixtures.validPayload())));

        assertSame(duplicate, thrown);
        verifyNoRuntimeWrites();
    }

    @Test
    void saveDraft_shouldPersistReconfigureModeForExistingRuntimePlatform() {
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(platformMapper.selectByPlatformCode("taobao"))
                .thenReturn(CpsPlatformDO.builder().id(3L).platformCode("taobao").status(1).build());
        when(draftMapper.insert(any(CpsPlatformOnboardingDraftDO.class))).thenReturn(1);

        CpsPlatformOnboardingDetailRespVO saved = service.saveDraft(
                saveRequest("taobao", null, CpsPlatformOnboardingTestFixtures.validPayload()));

        ArgumentCaptor<CpsPlatformOnboardingDraftDO> captor =
                ArgumentCaptor.forClass(CpsPlatformOnboardingDraftDO.class);
        verify(draftMapper).insert(captor.capture());
        assertEquals(CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode(), captor.getValue().getMode());
        assertEquals(CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode(), saved.getMode());
    }

    @Test
    void saveDraft_shouldMergeBlankSecretsFromStoredDraft() throws Exception {
        CpsPlatformOnboardingPayload storedPayload = CpsPlatformOnboardingTestFixtures.validPayload();
        CpsPlatformOnboardingDraftDO existing = persistedDraft(
                8L, 2, CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode(),
                CpsPlatformOnboardingStatusEnum.DRAFT.getCode(), storedPayload);
        CpsPlatformOnboardingPayload incoming = CpsPlatformOnboardingTestFixtures.validPayload();
        incoming.getPlatform().setExtraConfig(" ");
        incoming.getVendors().get(0).setAppKey("");
        incoming.getVendors().get(0).setAppSecret(" ");
        incoming.getVendors().get(0).setAuthToken(null);
        incoming.getVendors().get(0).setExtraConfig("");
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(existing);
        when(draftMapper.updatePayload(eq(8L), eq(2), anyString(), anyString(), eq("DRAFT")))
                .thenReturn(1);

        service.saveDraft(saveRequest("taobao", 2L, incoming));

        ArgumentCaptor<String> payloadJson = ArgumentCaptor.forClass(String.class);
        verify(draftMapper).updatePayload(eq(8L), eq(2), payloadJson.capture(), anyString(), eq("DRAFT"));
        CpsPlatformOnboardingPayload persisted =
                objectMapper.readValue(payloadJson.getValue(), CpsPlatformOnboardingPayload.class);
        assertEquals("{\"source\":\"onboarding\"}", persisted.getPlatform().getExtraConfig());
        assertEquals("dataoke-key", persisted.getVendors().get(0).getAppKey());
        assertEquals("dataoke-secret", persisted.getVendors().get(0).getAppSecret());
        assertEquals("dataoke-token", persisted.getVendors().get(0).getAuthToken());
        assertEquals("{\"vendor\":\"dataoke\"}", persisted.getVendors().get(0).getExtraConfig());
    }

    @Test
    void saveDraft_shouldMergeBlankCredentialsAndExtraConfigFromRuntime() throws Exception {
        CpsPlatformOnboardingPayload incoming = CpsPlatformOnboardingTestFixtures.validPayload();
        incoming.getPlatform().setExtraConfig(null);
        incoming.getVendors().get(0).setAppKey(" ");
        incoming.getVendors().get(0).setAppSecret(null);
        incoming.getVendors().get(0).setAuthToken("");
        incoming.getVendors().get(0).setExtraConfig(" ");
        CpsPlatformDO runtimePlatform = CpsPlatformDO.builder()
                .platformCode("taobao")
                .extraConfig("{\"runtime\":\"platform\"}")
                .status(1)
                .build();
        CpsApiVendorDO runtimeVendor = CpsApiVendorDO.builder()
                .platformCode("taobao")
                .vendorCode("dataoke")
                .appKey("runtime-key")
                .appSecret("runtime-secret")
                .authToken("runtime-token")
                .extraConfig("{\"runtime\":\"vendor\"}")
                .status(1)
                .build();
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(platformMapper.selectByPlatformCode("taobao")).thenReturn(runtimePlatform);
        when(vendorMapper.selectAllByPlatformCode("taobao")).thenReturn(List.of(runtimeVendor));
        when(draftMapper.insert(any(CpsPlatformOnboardingDraftDO.class))).thenReturn(1);

        service.saveDraft(saveRequest("taobao", null, incoming));

        ArgumentCaptor<CpsPlatformOnboardingDraftDO> captor =
                ArgumentCaptor.forClass(CpsPlatformOnboardingDraftDO.class);
        verify(draftMapper).insert(captor.capture());
        CpsPlatformOnboardingPayload persisted =
                objectMapper.readValue(captor.getValue().getPayloadCiphertext(),
                        CpsPlatformOnboardingPayload.class);
        assertEquals("{\"runtime\":\"platform\"}", persisted.getPlatform().getExtraConfig());
        assertEquals("runtime-key", persisted.getVendors().get(0).getAppKey());
        assertEquals("runtime-secret", persisted.getVendors().get(0).getAppSecret());
        assertEquals("runtime-token", persisted.getVendors().get(0).getAuthToken());
        assertEquals("{\"runtime\":\"vendor\"}", persisted.getVendors().get(0).getExtraConfig());
    }

    @Test
    void getDetail_shouldPreferPersistedDraftOverRuntime() throws Exception {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getPlatform().setPlatformName("草稿名称");
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(persistedDraft(
                8L, 3, CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode(),
                CpsPlatformOnboardingStatusEnum.DRAFT.getCode(), payload));

        CpsPlatformOnboardingDetailRespVO result = service.getDetail("taobao");

        assertEquals("草稿名称", result.getPayload().getPlatform().getPlatformName());
        assertEquals(3L, result.getDraftVersion());
        verifyNoInteractions(platformMapper, vendorMapper, adzoneMapper, rebateMapper);
    }

    @Test
    void getDetail_whenStoredPayloadIsCorrupted_shouldReportInvalidConfig() {
        CpsPlatformOnboardingDraftDO corrupted = CpsPlatformOnboardingDraftDO.builder()
                .id(8L)
                .platformCode("taobao")
                .payloadCiphertext("{not-json")
                .draftVersion(1)
                .status(CpsPlatformOnboardingStatusEnum.DRAFT.getCode())
                .build();
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(corrupted);

        assertServiceCode(ONBOARDING_CONFIG_INVALID.getCode(),
                () -> service.getDetail("taobao"));
    }

    @Test
    void deleteDraft_shouldDeleteMatchingVersionOnly() {
        CpsPlatformOnboardingDraftDO draft = CpsPlatformOnboardingDraftDO.builder()
                .id(8L).platformCode("taobao").draftVersion(2).build();
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(draft);
        when(draftMapper.deleteByIdAndVersion(8L, 2)).thenReturn(1);

        service.deleteDraft("taobao", 2L);

        verify(draftMapper).deleteByIdAndVersion(8L, 2);
        verifyNoRuntimeWrites();
    }

    @Test
    void deleteDraft_whenVersionIsStale_shouldReportConflict() {
        CpsPlatformOnboardingDraftDO draft = CpsPlatformOnboardingDraftDO.builder()
                .id(8L).platformCode("taobao").draftVersion(3).build();
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(draft);
        when(draftMapper.deleteByIdAndVersion(8L, 2)).thenReturn(0);
        when(draftMapper.selectById(8L)).thenReturn(draft);

        assertServiceCode(ONBOARDING_DRAFT_VERSION_CONFLICT.getCode(),
                () -> service.deleteDraft("taobao", 2L));
    }

    @Test
    void deleteDraft_whenDraftDoesNotExist_shouldReportNotExists() {
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(null);

        assertServiceCode(ONBOARDING_DRAFT_NOT_EXISTS.getCode(),
                () -> service.deleteDraft("taobao", 1L));
    }

    @Test
    void getRequiredPayload_shouldReturnDraftAndRejectMissingDraft() throws Exception {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(persistedDraft(
                8L, 1, CpsPlatformOnboardingModeEnum.CREATE.getCode(),
                CpsPlatformOnboardingStatusEnum.DRAFT.getCode(), payload));

        assertEquals("taobao", service.getRequiredPayload("taobao").getPlatform().getPlatformCode());

        when(draftMapper.selectByPlatformCode("jd")).thenReturn(null);
        assertServiceCode(ONBOARDING_DRAFT_NOT_EXISTS.getCode(),
                () -> service.getRequiredPayload("jd"));
    }

    @Test
    void markMethods_shouldUseVersionConstraintAndRejectStaleResults() {
        when(draftMapper.markValidating(8L, 4, "VALIDATING")).thenReturn(1);
        service.markValidating(8L, 4L);
        verify(draftMapper).markValidating(8L, 4, "VALIDATING");

        LocalDateTime validatedAt = LocalDateTime.of(2026, 7, 23, 11, 0);
        when(draftMapper.markChecked(8L, 4, "READY", "fingerprint", "passed", validatedAt))
                .thenReturn(1);
        service.markChecked(8L, 4L, "READY", "fingerprint", "passed", validatedAt);

        when(draftMapper.markChecked(eq(8L), eq(3), anyString(), any(), any(), any()))
                .thenReturn(0);
        when(draftMapper.selectById(8L)).thenReturn(
                CpsPlatformOnboardingDraftDO.builder().id(8L).draftVersion(4).build());
        assertServiceCode(ONBOARDING_DRAFT_VERSION_CONFLICT.getCode(),
                () -> service.markChecked(8L, 3L, "FAILED", null, "stale", null));
    }

    @Test
    void markValidating_whenDraftDoesNotExist_shouldReportNotExists() {
        when(draftMapper.markValidating(8L, 4, "VALIDATING")).thenReturn(0);
        when(draftMapper.selectById(8L)).thenReturn(null);

        assertServiceCode(ONBOARDING_DRAFT_NOT_EXISTS.getCode(),
                () -> service.markValidating(8L, 4L));
    }

    @Test
    void markValidating_whenVersionIsStale_shouldReportConflict() {
        when(draftMapper.markValidating(8L, 3, "VALIDATING")).thenReturn(0);
        when(draftMapper.selectById(8L)).thenReturn(
                CpsPlatformOnboardingDraftDO.builder().id(8L).draftVersion(4).build());

        assertServiceCode(ONBOARDING_DRAFT_VERSION_CONFLICT.getCode(),
                () -> service.markValidating(8L, 3L));
    }

    @Test
    void markChecked_whenDraftDoesNotExist_shouldReportNotExists() {
        when(draftMapper.markChecked(8L, 4, "READY", "fingerprint", "passed", null))
                .thenReturn(0);
        when(draftMapper.selectById(8L)).thenReturn(null);

        assertServiceCode(ONBOARDING_DRAFT_NOT_EXISTS.getCode(),
                () -> service.markChecked(8L, 4L, "READY", "fingerprint", "passed", null));
    }

    @Test
    void markChecked_whenVersionIsStale_shouldReportConflict() {
        when(draftMapper.markChecked(8L, 3, "READY", "fingerprint", "passed", null))
                .thenReturn(0);
        when(draftMapper.selectById(8L)).thenReturn(
                CpsPlatformOnboardingDraftDO.builder().id(8L).draftVersion(4).build());

        assertServiceCode(ONBOARDING_DRAFT_VERSION_CONFLICT.getCode(),
                () -> service.markChecked(8L, 3L, "READY", "fingerprint", "passed", null));
    }

    @Test
    void saveDraft_shouldNormalizeEveryNestedPlatformCode() throws Exception {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getPlatform().setPlatformCode(" TAOBAO ");
        payload.getVendors().forEach(vendor -> vendor.setPlatformCode(" TAOBAO "));
        payload.getAdzones().forEach(adzone -> adzone.setPlatformCode(" TAOBAO "));
        payload.getRebateRules().forEach(rule -> rule.setPlatformCode(" TAOBAO "));
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(platformMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(draftMapper.insert(any(CpsPlatformOnboardingDraftDO.class))).thenReturn(1);

        service.saveDraft(saveRequest(" TAOBAO ", null, payload));

        ArgumentCaptor<CpsPlatformOnboardingDraftDO> captor =
                ArgumentCaptor.forClass(CpsPlatformOnboardingDraftDO.class);
        verify(draftMapper).insert(captor.capture());
        CpsPlatformOnboardingPayload stored =
                objectMapper.readValue(captor.getValue().getPayloadCiphertext(),
                        CpsPlatformOnboardingPayload.class);
        assertEquals("taobao", stored.getPlatform().getPlatformCode());
        assertTrue(stored.getVendors().stream()
                .allMatch(vendor -> "taobao".equals(vendor.getPlatformCode())));
        assertTrue(stored.getAdzones().stream()
                .allMatch(adzone -> "taobao".equals(adzone.getPlatformCode())));
        assertTrue(stored.getRebateRules().stream()
                .allMatch(rule -> "taobao".equals(rule.getPlatformCode())));
    }

    @Test
    void saveDraft_shouldRejectPlatformCodeMismatchBeforeWriting() {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getPlatform().setPlatformCode("jd");

        assertServiceCode(ONBOARDING_CONFIG_INVALID.getCode(),
                () -> service.saveDraft(saveRequest("taobao", null, payload)));

        verifyNoInteractions(draftMapper, platformMapper, vendorMapper, adzoneMapper, rebateMapper);
    }

    @Test
    void saveDraft_shouldRejectVendorPlatformCodeMismatchBeforeWriting() {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getVendors().get(0).setPlatformCode("jd");

        assertServiceCode(ONBOARDING_CONFIG_INVALID.getCode(),
                () -> service.saveDraft(saveRequest("taobao", null, payload)));

        verifyNoInteractions(draftMapper, platformMapper, vendorMapper, adzoneMapper, rebateMapper);
    }

    @Test
    void saveDraft_shouldRejectMissingVendorPlatformCodeBeforeWriting() {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getVendors().get(0).setPlatformCode(" ");

        assertServiceCode(ONBOARDING_CONFIG_INVALID.getCode(),
                () -> service.saveDraft(saveRequest("taobao", null, payload)));

        verifyNoInteractions(draftMapper, platformMapper, vendorMapper, adzoneMapper, rebateMapper);
    }

    @Test
    void saveDraft_shouldRejectAdzonePlatformCodeMismatchBeforeWriting() {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getAdzones().get(0).setPlatformCode("jd");

        assertServiceCode(ONBOARDING_CONFIG_INVALID.getCode(),
                () -> service.saveDraft(saveRequest("taobao", null, payload)));

        verifyNoInteractions(draftMapper, platformMapper, vendorMapper, adzoneMapper, rebateMapper);
    }

    @Test
    void saveDraft_shouldRejectMissingAdzonePlatformCodeBeforeWriting() {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getAdzones().get(0).setPlatformCode(null);

        assertServiceCode(ONBOARDING_CONFIG_INVALID.getCode(),
                () -> service.saveDraft(saveRequest("taobao", null, payload)));

        verifyNoInteractions(draftMapper, platformMapper, vendorMapper, adzoneMapper, rebateMapper);
    }

    @Test
    void saveDraft_shouldRejectRebatePlatformCodeMismatchBeforeWriting() {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getRebateRules().get(0).setPlatformCode("jd");

        assertServiceCode(ONBOARDING_CONFIG_INVALID.getCode(),
                () -> service.saveDraft(saveRequest("taobao", null, payload)));

        verifyNoInteractions(draftMapper, platformMapper, vendorMapper, adzoneMapper, rebateMapper);
    }

    @Test
    void saveDraft_shouldRejectMissingRebatePlatformCodeBeforeWriting() {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getRebateRules().get(0).setPlatformCode("");

        assertServiceCode(ONBOARDING_CONFIG_INVALID.getCode(),
                () -> service.saveDraft(saveRequest("taobao", null, payload)));

        verifyNoInteractions(draftMapper, platformMapper, vendorMapper, adzoneMapper, rebateMapper);
    }

    @Test
    void saveDraft_shouldRejectNullNestedElementAsInvalidConfig() {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getVendors().set(0, null);

        assertServiceCode(ONBOARDING_CONFIG_INVALID.getCode(),
                () -> service.saveDraft(saveRequest("taobao", null, payload)));

        verifyNoInteractions(draftMapper, platformMapper, vendorMapper, adzoneMapper, rebateMapper);
    }

    @Test
    void saveDraft_shouldNormalizeNullCollectionsToEmptyLists() throws Exception {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.setVendors(null);
        payload.setAdzones(null);
        payload.setRebateRules(null);
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(platformMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(draftMapper.insert(any(CpsPlatformOnboardingDraftDO.class))).thenReturn(1);

        service.saveDraft(saveRequest("taobao", null, payload));

        ArgumentCaptor<CpsPlatformOnboardingDraftDO> captor =
                ArgumentCaptor.forClass(CpsPlatformOnboardingDraftDO.class);
        verify(draftMapper).insert(captor.capture());
        CpsPlatformOnboardingPayload stored =
                objectMapper.readValue(captor.getValue().getPayloadCiphertext(),
                        CpsPlatformOnboardingPayload.class);
        assertNotNull(stored.getVendors());
        assertNotNull(stored.getAdzones());
        assertNotNull(stored.getRebateRules());
        assertTrue(stored.getVendors().isEmpty());
        assertTrue(stored.getAdzones().isEmpty());
        assertTrue(stored.getRebateRules().isEmpty());
    }

    private CpsPlatformOnboardingDraftSaveReqVO saveRequest(
            String platformCode, Long version, CpsPlatformOnboardingPayload payload) {
        CpsPlatformOnboardingDraftSaveReqVO request = new CpsPlatformOnboardingDraftSaveReqVO();
        request.setPlatformCode(platformCode);
        request.setDraftVersion(version);
        request.setPayload(payload);
        return request;
    }

    private CpsPlatformOnboardingDraftDO persistedDraft(
            Long id, Integer version, String mode, String status, CpsPlatformOnboardingPayload payload)
            throws Exception {
        return CpsPlatformOnboardingDraftDO.builder()
                .id(id)
                .platformCode(payload.getPlatform().getPlatformCode())
                .mode(mode)
                .payloadCiphertext(objectMapper.writeValueAsString(payload))
                .draftVersion(version)
                .configFingerprint("old-config-fingerprint")
                .status(status)
                .build();
    }

    private void assertServiceCode(int code, org.junit.jupiter.api.function.Executable executable) {
        ServiceException exception = assertThrows(ServiceException.class, executable);
        assertEquals(code, exception.getCode());
    }

    private void verifyNoRuntimeWrites() {
        verify(platformMapper, never()).insert(any(CpsPlatformDO.class));
        verify(platformMapper, never()).updateById(any(CpsPlatformDO.class));
        verify(platformMapper, never()).deleteById(any());
        verify(vendorMapper, never()).insert(any(CpsApiVendorDO.class));
        verify(vendorMapper, never()).updateById(any(CpsApiVendorDO.class));
        verify(vendorMapper, never()).deleteById(any());
        verify(adzoneMapper, never()).insert(any(CpsAdzoneDO.class));
        verify(adzoneMapper, never()).updateById(any(CpsAdzoneDO.class));
        verify(adzoneMapper, never()).deleteById(any());
        verify(rebateMapper, never()).insert(any(CpsRebateConfigDO.class));
        verify(rebateMapper, never()).updateById(any(CpsRebateConfigDO.class));
        verify(rebateMapper, never()).deleteById(any());
    }

}
