package com.qiji.cps.module.cps.service.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingCheckRespVO;
import com.qiji.cps.module.cps.enums.onboarding.CpsPlatformOnboardingStatusEnum;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;

import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_DRAFT_VERSION_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CpsPlatformOnboardingConnectionTesterTest {

    @Mock
    private CpsPlatformOnboardingDraftService draftService;
    @Mock
    private CpsPlatformOnboardingValidator validator;
    @Mock
    private CpsPlatformClientFactory clientFactory;
    @Mock
    private CpsApiVendorClient primaryClient;
    @Mock
    private CpsApiVendorClient backupClient;

    private CpsPlatformOnboardingConnectionTester tester;
    private CpsPlatformOnboardingPayload payload;
    private CpsPlatformOnboardingFingerprint fingerprint;
    private String snapshotFingerprint;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        fingerprint = new CpsPlatformOnboardingFingerprint(objectMapper);
        tester = new CpsPlatformOnboardingConnectionTester(
                draftService, validator, clientFactory, objectMapper, fingerprint);
        payload = CpsPlatformOnboardingTestFixtures.validPayload();
        snapshotFingerprint = fingerprint.calculate(payload);
        when(draftService.getRequiredSnapshot("taobao", 5L))
                .thenReturn(new CpsPlatformOnboardingDraftService.DraftSnapshot(
                        7L, 5L, snapshotFingerprint, payload));
    }

    @Test
    void test_shouldUseExactSnapshotAndMarkReadyOnlyAfterAllEnabledVendorsPass() {
        when(validator.validateNormalized(payload)).thenReturn(validated(payload));
        when(clientFactory.getVendorClient("dataoke", "taobao")).thenReturn(primaryClient);
        when(clientFactory.getVendorClient("official", "taobao")).thenReturn(backupClient);
        when(primaryClient.testConnection(any())).thenReturn(true);
        when(backupClient.testConnection(any())).thenReturn(true);

        CpsPlatformOnboardingCheckRespVO result = tester.test("taobao", 5L);

        assertTrue(result.isSuccess());
        assertEquals("dataoke vendor（主供应商，dataoke）", result.getItems().get(0).getSection());
        assertEquals("official vendor（备用供应商，official）", result.getItems().get(1).getSection());
        verify(draftService).markValidating(7L, 5L);
        verify(draftService).markChecked(eq(7L), eq(5L),
                eq(CpsPlatformOnboardingStatusEnum.READY.getCode()),
                eq(snapshotFingerprint), any(String.class), any(LocalDateTime.class));
        ArgumentCaptor<CpsVendorConfig> primaryConfig =
                ArgumentCaptor.forClass(CpsVendorConfig.class);
        ArgumentCaptor<CpsVendorConfig> backupConfig =
                ArgumentCaptor.forClass(CpsVendorConfig.class);
        verify(primaryClient).testConnection(primaryConfig.capture());
        verify(backupClient).testConnection(backupConfig.capture());
        assertEquals("dataoke-key", primaryConfig.getValue().getAppKey());
        assertEquals("dataoke-secret", primaryConfig.getValue().getAppSecret());
        assertEquals("adzone-primary", primaryConfig.getValue().getDefaultAdzoneId());
        assertEquals("dataoke", primaryConfig.getValue().getExtraConfig().get("vendor"));
        assertEquals("official-key", backupConfig.getValue().getAppKey());
        assertEquals("official-secret", backupConfig.getValue().getAppSecret());
        assertEquals("adzone-primary", backupConfig.getValue().getDefaultAdzoneId());
        assertEquals("official", backupConfig.getValue().getExtraConfig().get("vendor"));
    }

    @Test
    void test_shouldSkipDisabledVendor() {
        payload.getVendors().get(1).setStatus(0);
        when(validator.validateNormalized(payload)).thenReturn(validated(payload));
        when(clientFactory.getVendorClient("dataoke", "taobao")).thenReturn(primaryClient);
        when(primaryClient.testConnection(any())).thenReturn(true);

        assertTrue(tester.test("taobao", 5L).isSuccess());

        verify(clientFactory, never()).getVendorClient("official", "taobao");
        verify(backupClient, never()).testConnection(any());
    }

    @Test
    void testVendor_shouldCallOnlySelectedVendorWithoutChangingDraftLifecycleState() {
        when(clientFactory.getVendorClient("dataoke", "taobao")).thenReturn(primaryClient);
        when(primaryClient.testConnection(any())).thenReturn(true);

        CpsPlatformOnboardingCheckRespVO result =
                tester.testVendor("taobao", 5L, "dataoke");

        assertTrue(result.isSuccess());
        assertEquals("VENDOR_CONNECTION_OK", result.getItems().get(0).getCode());
        verify(primaryClient).testConnection(any());
        verify(backupClient, never()).testConnection(any());
        verify(draftService, never()).markValidating(any(), any());
        verify(draftService, never()).markChecked(any(), any(), any(), any(), any(), any());
    }

    @Test
    void test_structuralFailure_shouldStoreSanitizedFailedStateAndNotCallClient() {
        CpsPlatformOnboardingCheckRespVO structural = CpsPlatformOnboardingCheckRespVO.failed(
                CpsPlatformOnboardingCheckRespVO.Item.builder()
                        .code("VENDOR_CONFIG_INVALID")
                        .fieldPath("vendors[0].appSecret")
                        .section("vendor")
                        .message("凭证不完整")
                        .build());
        when(validator.validateNormalized(payload))
                .thenReturn(new CpsPlatformOnboardingValidator.ValidationResult(structural, null));

        CpsPlatformOnboardingCheckRespVO result = tester.test("taobao", 5L);

        assertFalse(result.isSuccess());
        verify(draftService).markChecked(7L, 5L,
                CpsPlatformOnboardingStatusEnum.FAILED.getCode(),
                null, "VENDOR_CONFIG_INVALID:凭证不完整", null);
        verify(clientFactory, never()).getVendorClient(any(), any());
    }

    @Test
    void test_exceptionOrFalse_shouldMaskSecretsAndNeverValidateFingerprint() {
        when(validator.validateNormalized(payload)).thenReturn(validated(payload));
        when(clientFactory.getVendorClient("dataoke", "taobao")).thenReturn(primaryClient);
        when(clientFactory.getVendorClient("official", "taobao")).thenReturn(backupClient);
        when(primaryClient.testConnection(any())).thenThrow(
                new IllegalStateException("token dataoke-secret and dataoke-token rejected"));
        when(backupClient.testConnection(any())).thenReturn(false);

        CpsPlatformOnboardingCheckRespVO result = tester.test("taobao", 5L);

        assertFalse(result.isSuccess());
        assertFalse(result.toString().contains("dataoke-secret"));
        assertFalse(result.toString().contains("dataoke-token"));
        assertTrue(result.getItems().stream().allMatch(item ->
                !item.getMessage().contains("dataoke-secret")
                        && !item.getMessage().contains("dataoke-token")));
        verify(draftService).markChecked(eq(7L), eq(5L),
                eq(CpsPlatformOnboardingStatusEnum.FAILED.getCode()),
                isNull(), any(String.class), isNull());
        verify(backupClient).testConnection(any());
    }

    @Test
    void test_shouldPersistFingerprintOfExactNormalizedPayload() {
        CpsPlatformOnboardingPayload normalized = CpsPlatformOnboardingTestFixtures.validPayload();
        normalized.getPlatform().setDefaultAdzoneId("adzone-primary");
        normalized.getPlatform().setActiveVendorCode("dataoke");
        String normalizedFingerprint = fingerprint.calculate(normalized);
        when(validator.validateNormalized(payload)).thenReturn(validated(normalized));
        when(clientFactory.getVendorClient("dataoke", "taobao")).thenReturn(primaryClient);
        when(clientFactory.getVendorClient("official", "taobao")).thenReturn(backupClient);
        when(primaryClient.testConnection(any())).thenReturn(true);
        when(backupClient.testConnection(any())).thenReturn(true);

        assertTrue(tester.test("taobao", 5L).isSuccess());

        verify(draftService).markChecked(eq(7L), eq(5L),
                eq(CpsPlatformOnboardingStatusEnum.READY.getCode()),
                eq(normalizedFingerprint), any(String.class), any(LocalDateTime.class));
    }

    @Test
    void test_unexpectedValidatorFailure_shouldMarkSanitizedFailedState() {
        when(validator.validateNormalized(payload)).thenThrow(
                new IllegalStateException("token raw-validator-secret rejected"));

        CpsPlatformOnboardingCheckRespVO result = tester.test("taobao", 5L);

        assertFalse(result.isSuccess());
        assertFalse(result.toString().contains("raw-validator-secret"));
        verify(draftService).markChecked(eq(7L), eq(5L),
                eq(CpsPlatformOnboardingStatusEnum.FAILED.getCode()),
                isNull(), eq("ONBOARDING_TEST_FAILED:平台连接检测异常，请稍后重试"), isNull());
    }

    @Test
    void test_markCheckedCasConflict_shouldPropagateAndNeverReportReady() {
        when(validator.validateNormalized(payload)).thenReturn(validated(payload));
        when(clientFactory.getVendorClient("dataoke", "taobao")).thenReturn(primaryClient);
        when(clientFactory.getVendorClient("official", "taobao")).thenReturn(backupClient);
        when(primaryClient.testConnection(any())).thenReturn(true);
        when(backupClient.testConnection(any())).thenReturn(true);
        doThrow(new ServiceException(ONBOARDING_DRAFT_VERSION_CONFLICT))
                .when(draftService).markChecked(eq(7L), eq(5L), any(), any(), any(), any());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> tester.test("taobao", 5L));

        assertEquals(ONBOARDING_DRAFT_VERSION_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    void connectionTester_shouldSuspendAmbientTransactions() throws Exception {
        Transactional transactional = CpsPlatformOnboardingConnectionTester.class
                .getMethod("test", String.class, Long.class)
                .getAnnotation(Transactional.class);

        assertEquals(Propagation.NOT_SUPPORTED, transactional.propagation());
    }

    private static CpsPlatformOnboardingValidator.ValidationResult validated(
            CpsPlatformOnboardingPayload payload) {
        return new CpsPlatformOnboardingValidator.ValidationResult(
                CpsPlatformOnboardingCheckRespVO.success(), payload);
    }

}
