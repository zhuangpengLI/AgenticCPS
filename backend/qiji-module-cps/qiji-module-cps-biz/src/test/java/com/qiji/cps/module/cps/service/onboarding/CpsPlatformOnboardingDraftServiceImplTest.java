package com.qiji.cps.module.cps.service.onboarding;

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
                .allMatch(vendor -> vendor.getAppSecret() == null && vendor.getAuthToken() == null));
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
                .status(0)
                .build();
        when(draftMapper.selectByPlatformCode("jd")).thenReturn(null);
        when(platformMapper.selectByPlatformCode("jd")).thenReturn(platform);
        when(vendorMapper.selectAllByPlatformCode("jd")).thenReturn(List.of(vendor));
        when(adzoneMapper.selectAllByPlatformCode("jd")).thenReturn(List.of(
                CpsAdzoneDO.builder().platformCode("jd").adzoneId("jd-pid").isDefault(1).status(0).build()));
        when(rebateMapper.selectListByPlatformCode("jd")).thenReturn(List.of(
                CpsRebateConfigDO.builder().platformCode("jd").rebateRate(new java.math.BigDecimal("60")).status(0).build()));

        CpsPlatformOnboardingDetailRespVO result = service.getDetail(" JD ");

        assertEquals(CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode(), result.getMode());
        assertEquals("jd", result.getPayload().getPlatform().getPlatformCode());
        assertNull(result.getDraftVersion());
        assertNull(result.getPayload().getVendors().get(0).getAppSecret());
        assertNull(result.getPayload().getVendors().get(0).getAuthToken());
        assertTrue(result.getPayload().getVendors().get(0).getAppSecretConfigured());
        assertTrue(result.getPayload().getVendors().get(0).getAuthTokenConfigured());
        assertEquals(1, result.getPayload().getAdzones().size());
        assertEquals(1, result.getPayload().getRebateRules().size());
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
        incoming.getVendors().get(0).setAppSecret(" ");
        incoming.getVendors().get(0).setAuthToken(null);
        when(draftMapper.selectByPlatformCode("taobao")).thenReturn(existing);
        when(draftMapper.updatePayload(eq(8L), eq(2), anyString(), anyString(), eq("DRAFT")))
                .thenReturn(1);

        service.saveDraft(saveRequest("taobao", 2L, incoming));

        ArgumentCaptor<String> payloadJson = ArgumentCaptor.forClass(String.class);
        verify(draftMapper).updatePayload(eq(8L), eq(2), payloadJson.capture(), anyString(), eq("DRAFT"));
        CpsPlatformOnboardingPayload persisted =
                objectMapper.readValue(payloadJson.getValue(), CpsPlatformOnboardingPayload.class);
        assertEquals("dataoke-secret", persisted.getVendors().get(0).getAppSecret());
        assertEquals("dataoke-token", persisted.getVendors().get(0).getAuthToken());
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
    void deleteDraft_shouldDeleteOnlyDraftAndRejectMissingDraft() {
        CpsPlatformOnboardingDraftDO draft = CpsPlatformOnboardingDraftDO.builder().id(8L).platformCode("taobao").build();
        when(draftMapper.selectByPlatformCode("taobao"))
                .thenReturn(draft)
                .thenReturn(null);
        when(draftMapper.deleteById(8L)).thenReturn(1);

        service.deleteDraft("taobao");
        verify(draftMapper).deleteById(8L);
        verifyNoRuntimeWrites();

        assertServiceCode(ONBOARDING_DRAFT_NOT_EXISTS.getCode(), () -> service.deleteDraft("taobao"));
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
        assertServiceCode(ONBOARDING_DRAFT_VERSION_CONFLICT.getCode(),
                () -> service.markChecked(8L, 3L, "FAILED", null, "stale", null));
    }

    @Test
    void saveDraft_shouldRejectPlatformCodeMismatchBeforeWriting() {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getPlatform().setPlatformCode("jd");

        assertServiceCode(ONBOARDING_CONFIG_INVALID.getCode(),
                () -> service.saveDraft(saveRequest("taobao", null, payload)));

        verifyNoInteractions(draftMapper, platformMapper, vendorMapper, adzoneMapper, rebateMapper);
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
