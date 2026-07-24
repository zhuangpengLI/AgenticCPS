package com.qiji.cps.module.cps.service.onboarding;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorConfigField;
import com.qiji.cps.module.cps.client.CpsVendorConfigSchema;
import com.qiji.cps.module.cps.client.CpsVendorDescriptor;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.*;
import com.qiji.cps.module.cps.service.adzone.CpsAdzoneService;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingRebateRule;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import com.qiji.cps.module.cps.service.rebate.CpsRebateConfigService;
import com.qiji.cps.module.cps.service.vendor.CpsApiVendorService;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CpsPlatformOnboardingLifecycleServiceTest {

    @Mock
    private CpsPlatformService platformService;
    @Mock
    private CpsPlatformClientFactory clientFactory;
    @Mock
    private CpsPlatformOnboardingDraftService draftService;
    @Mock
    private CpsPlatformOnboardingService onboardingService;
    @Mock
    private CpsPlatformOnboardingValidator validator;
    @Mock
    private CpsPlatformOnboardingConnectionTester connectionTester;
    @Mock
    private CpsApiVendorService vendorService;
    @Mock
    private CpsAdzoneService adzoneService;
    @Mock
    private CpsRebateConfigService rebateConfigService;

    @InjectMocks
    private CpsPlatformOnboardingLifecycleService service;

    @Test
    void page_shouldBeEmptyBeforeRuntimeRowsExist() {
        when(clientFactory.getRegisteredPlatformCodes()).thenReturn(Set.of());
        when(clientFactory.getRegisteredVendorDescriptors()).thenReturn(java.util.List.of());
        when(platformService.getPlatformPage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(PageResult.empty());
        PageResult<?> result = service.getPage(new CpsPlatformOnboardingPageReqVO());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void page_shouldKeepCapabilityOnlyRowsWhenOnePlatformRuntimeReadFails() {
        when(clientFactory.getRegisteredPlatformCodes()).thenReturn(
                new java.util.LinkedHashSet<>(List.of("broken", "healthy")));
        when(clientFactory.getRegisteredVendorDescriptors()).thenReturn(List.of());
        when(platformService.getPlatformPage(any())).thenReturn(PageResult.empty());
        when(draftService.getRuntimeDetail("broken"))
                .thenThrow(new IllegalStateException("runtime unavailable"));
        when(draftService.getDetail("broken"))
                .thenThrow(new IllegalStateException("draft unavailable"));
        when(platformService.getPlatformByCode("broken"))
                .thenThrow(new IllegalStateException("platform unavailable"));

        PageResult<CpsPlatformOnboardingPageRespVO> result =
                service.getPage(new CpsPlatformOnboardingPageReqVO());

        assertEquals(2L, result.getTotal());
        assertEquals(List.of("broken", "healthy"),
                result.getList().stream()
                        .map(CpsPlatformOnboardingPageRespVO::getPlatformCode)
                        .toList());
        assertEquals(0, result.getList().get(0).getCompletionPercent());
    }

    @Test
    void deletePlatform_whenEnabled_shouldReject() {
        when(platformService.getPlatformByCode("taobao"))
                .thenReturn(CpsPlatformDO.builder().platformCode("taobao").status(1).build());
        assertThrows(RuntimeException.class, () -> service.deletePlatformBundle("taobao"));
    }

    @Test
    void page_shouldComputeSixtyPercentWhenDefaultRebateAndConnectionAreMissing() {
        when(clientFactory.getRegisteredPlatformCodes()).thenReturn(Set.of("taobao"));
        when(clientFactory.getRegisteredVendorDescriptors()).thenReturn(java.util.List.of());
        when(platformService.getPlatformPage(any())).thenReturn(PageResult.empty());
        when(platformService.getPlatformByCode("taobao"))
                .thenReturn(CpsPlatformDO.builder().platformCode("taobao").status(0).build());
        CpsPlatformOnboardingPayloadRespVO payload = CpsPlatformOnboardingPayloadRespVO.builder()
                .platform(CpsOnboardingPlatformRespVO.builder()
                        .platformCode("taobao").platformName("淘宝").status(0).build())
                .primaryVendorCode("dataoke")
                .runtimeDefaultAdzoneId("general")
                .vendors(java.util.List.of(CpsOnboardingVendorRespVO.builder()
                        .vendorCode("dataoke").platformCode("taobao").status(1)
                        .appKeyConfigured(true).appSecretConfigured(true)
                        .apiBaseUrl("https://api.example.com").build()))
                .adzones(java.util.List.of(CpsOnboardingAdzone.builder()
                        .platformCode("taobao").adzoneId("general").adzoneType("general")
                        .status(1).build()))
                .build();
        CpsPlatformOnboardingDetailRespVO detail = new CpsPlatformOnboardingDetailRespVO();
        detail.setPayload(payload);
        when(draftService.getRuntimeDetail("taobao")).thenReturn(detail);
        when(draftService.getDetail("taobao")).thenReturn(detail);
        when(clientFactory.getVendorDescriptor("dataoke", "taobao")).thenReturn(
                CpsVendorDescriptor.builder()
                        .vendorCode("dataoke")
                        .platformCode("taobao")
                        .configSchema(CpsVendorConfigSchema.standard())
                        .build());

        var item = service.getPage(new CpsPlatformOnboardingPageReqVO())
                .getList().get(0);
        assertEquals(60, item.getCompletionPercent());
        assertEquals(java.util.List.of("DEFAULT_REBATE", "CONNECTION_TEST"),
                item.getMissingItems());
    }

    @Test
    void page_shouldUseCurrentDraftPayloadAndExactDraftTestEvidence() {
        when(clientFactory.getRegisteredPlatformCodes()).thenReturn(Set.of("taobao"));
        when(clientFactory.getRegisteredVendorDescriptors()).thenReturn(List.of());
        when(platformService.getPlatformPage(any())).thenReturn(PageResult.empty());
        when(platformService.getPlatformByCode("taobao"))
                .thenReturn(CpsPlatformDO.builder().platformCode("taobao").status(0).build());
        when(clientFactory.getVendorDescriptor("dataoke", "taobao")).thenReturn(null);

        CpsPlatformOnboardingPayloadRespVO runtime = completePayload();
        runtime.setRebateRules(List.of());
        CpsPlatformOnboardingPayloadRespVO draft = completePayload();
        CpsPlatformOnboardingDetailRespVO runtimeDetail = detail(null, runtime, "PUBLISHED",
                "runtime-fingerprint", "runtime-fingerprint");
        CpsPlatformOnboardingDetailRespVO draftDetail = detail(8L, draft, "READY",
                "draft-fingerprint", "draft-fingerprint");
        when(draftService.getRuntimeDetail("taobao")).thenReturn(runtimeDetail);
        when(draftService.getDetail("taobao")).thenReturn(draftDetail);

        CpsPlatformOnboardingPageRespVO item =
                service.getPage(new CpsPlatformOnboardingPageReqVO()).getList().get(0);

        assertEquals(100, item.getCompletionPercent());
        assertEquals(List.of(), item.getMissingItems());
        assertEquals("PASSED", item.getConnectionStatus());
        assertEquals("READY", item.getDraftStatus());
    }

    @Test
    void page_shouldResolveDefaultRebateByExactScopeThenHighestPriority() {
        when(clientFactory.getRegisteredPlatformCodes()).thenReturn(Set.of("taobao"));
        when(clientFactory.getRegisteredVendorDescriptors()).thenReturn(List.of());
        when(platformService.getPlatformPage(any())).thenReturn(PageResult.empty());
        when(platformService.getPlatformByCode("taobao"))
                .thenReturn(CpsPlatformDO.builder().platformCode("taobao").status(0).build());
        when(clientFactory.getVendorDescriptor("dataoke", "taobao")).thenReturn(null);

        CpsPlatformOnboardingPayloadRespVO payload = completePayload();
        payload.setRebateRules(List.of(
                rebateRule("taobao", "10.00", 1),
                rebateRule(null, "99.00", 999),
                rebateRule("taobao", "30.00", 10)));
        CpsPlatformOnboardingDetailRespVO draft =
                detail(8L, payload, "READY", "fingerprint", "fingerprint");
        when(draftService.getRuntimeDetail("taobao")).thenReturn(null);
        when(draftService.getDetail("taobao")).thenReturn(draft);

        CpsPlatformOnboardingPageRespVO item =
                service.getPage(new CpsPlatformOnboardingPageReqVO()).getList().get(0);

        assertEquals(new BigDecimal("30.00"), item.getDefaultRebateRate());
        assertEquals(100, item.getCompletionPercent());
    }

    @Test
    void page_shouldEvaluateEachDescriptorRequiredExtensionFieldIndividually() {
        CpsVendorDescriptor descriptor = CpsVendorDescriptor.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .configSchema(new CpsVendorConfigSchema(List.of(
                        CpsVendorConfigField.required("appKey", true),
                        CpsVendorConfigField.required("appSecret", true),
                        CpsVendorConfigField.required("apiBaseUrl", false),
                        CpsVendorConfigField.required("customToken", true))))
                .build();
        when(clientFactory.getRegisteredPlatformCodes()).thenReturn(Set.of("taobao"));
        when(clientFactory.getRegisteredVendorDescriptors()).thenReturn(List.of(descriptor));
        when(platformService.getPlatformPage(any())).thenReturn(PageResult.empty());
        when(platformService.getPlatformByCode("taobao"))
                .thenReturn(CpsPlatformDO.builder().platformCode("taobao").status(0).build());
        when(clientFactory.getVendorDescriptor("dataoke", "taobao")).thenReturn(descriptor);

        CpsPlatformOnboardingPayloadRespVO payload = completePayload();
        CpsOnboardingVendorRespVO vendor = payload.getVendors().get(0);
        vendor.setApiBaseUrl("https://api.example.com");
        vendor.setConfiguredFields(List.of("customToken"));
        CpsPlatformOnboardingDetailRespVO draft =
                detail(8L, payload, "READY", "fingerprint", "fingerprint");
        when(draftService.getRuntimeDetail("taobao")).thenReturn(null);
        when(draftService.getDetail("taobao")).thenReturn(draft);

        CpsPlatformOnboardingPageRespVO complete =
                service.getPage(new CpsPlatformOnboardingPageReqVO()).getList().get(0);
        assertEquals(100, complete.getCompletionPercent());

        vendor.setConfiguredFields(List.of("anotherField"));
        CpsPlatformOnboardingPageRespVO incomplete =
                service.getPage(new CpsPlatformOnboardingPageReqVO()).getList().get(0);
        assertEquals(80, incomplete.getCompletionPercent());
        assertEquals(List.of("PRIMARY_VENDOR"), incomplete.getMissingItems());
    }

    @Test
    void deleteDraft_withoutVersion_shouldResolveCurrentVersionBeforeCasDelete() {
        CpsPlatformOnboardingDraftDeleteReqVO request =
                new CpsPlatformOnboardingDraftDeleteReqVO();
        request.setPlatformCode(" Taobao ");
        when(draftService.getRequiredSnapshot("taobao")).thenReturn(
                new CpsPlatformOnboardingDraftService.DraftSnapshot(
                        8L, 4L, "fingerprint", new CpsPlatformOnboardingPayload()));

        service.deleteDraft(request);

        verify(draftService).getRequiredSnapshot("taobao");
        verify(draftService).deleteDraft("taobao", 4L);
    }

    @Test
    void deleteDraft_withVersion_shouldKeepCallerCasEvidence() {
        CpsPlatformOnboardingDraftDeleteReqVO request =
                new CpsPlatformOnboardingDraftDeleteReqVO();
        request.setPlatformCode("taobao");
        request.setDraftVersion(3L);

        service.deleteDraft(request);

        verify(draftService, never()).getRequiredSnapshot(anyString());
        verify(draftService).deleteDraft("taobao", 3L);
    }

    @Test
    void enable_requiresPublishedExactFingerprintEvidence() {
        when(platformService.getPlatformByCode("taobao"))
                .thenReturn(CpsPlatformDO.builder().platformCode("taobao").status(0).build());
        when(draftService.getRequiredSnapshot("taobao"))
                .thenReturn(new CpsPlatformOnboardingDraftService.DraftSnapshot(
                        1L, 2L, "fp", "different", "READY", null,
                        new CpsPlatformOnboardingPayload()));
        assertThrows(RuntimeException.class, () -> service.enablePlatform("taobao"));
        verify(platformService, never()).updatePlatform(any(CpsPlatformSaveReqVO.class));
    }

    @Test
    void disable_updatesOnlyRuntimeStatus() {
        CpsPlatformDO platform = CpsPlatformDO.builder().id(1L).platformCode("taobao")
                .platformName("淘宝").status(1).build();
        when(platformService.getPlatformByCode("taobao")).thenReturn(platform);
        service.disablePlatform("taobao");
        verify(platformService).updatePlatform(argThat(req -> Integer.valueOf(0).equals(req.getStatus())
                && "taobao".equals(req.getPlatformCode())));
    }

    @Test
    void deleteBundle_deletesOnlyManagedPlatformRows() {
        CpsPlatformDO platform = CpsPlatformDO.builder().id(1L).platformCode("taobao")
                .platformName("淘宝").status(0).build();
        when(platformService.getPlatformByCode("taobao")).thenReturn(platform);
        CpsPlatformOnboardingDetailRespVO noDraft = new CpsPlatformOnboardingDetailRespVO();
        when(draftService.getDetail("taobao")).thenReturn(noDraft);

        service.deletePlatformBundle("taobao");

        verify(vendorService).deleteVendorsNotIn("taobao", Set.of());
        verify(adzoneService).deleteAdzonesNotIn("taobao", Set.of());
        verify(rebateConfigService).deleteManagedRebateRulesNotIn("taobao", Set.of());
        verify(platformService).deletePlatform(1L);
        verify(draftService, never()).deleteDraft(anyString(), anyLong());
        verifyNoInteractions(onboardingService, validator, connectionTester, clientFactory);
        assertFalse(Arrays.stream(CpsPlatformOnboardingLifecycleService.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getType)
                .map(Class::getSimpleName)
                .anyMatch(name -> name.contains("Order")
                        || name.contains("RebateRecord")
                        || name.contains("Settlement")
                        || name.contains("Audit")
                        || name.endsWith("Mapper")));
    }

    @Test
    void capabilities_shouldUnionPlatformClientsAndVendorDescriptors() {
        CpsPlatformClient nativeClient = mock(CpsPlatformClient.class);
        when(nativeClient.getCapabilities()).thenReturn(Set.of(CpsVendorCapability.GOODS_SEARCH));
        CpsVendorDescriptor vendorOnly = CpsVendorDescriptor.builder()
                .platformCode("vendor-only").vendorCode("adapter")
                .capabilities(Set.of(CpsVendorCapability.PROMOTION_LINK)).build();
        when(clientFactory.getRegisteredPlatformCodes()).thenReturn(Set.of("native"));
        when(clientFactory.getRegisteredVendorDescriptors()).thenReturn(java.util.List.of(vendorOnly));
        when(clientFactory.getClient("native")).thenReturn(nativeClient);

        var capabilities = service.getPlatformCapabilities(null);
        assertEquals(2, capabilities.size());
        assertEquals("vendor-only", capabilities.get(1).getPlatformCode());
        assertEquals(List.of(CpsVendorCapability.PROMOTION_LINK.getCode()),
                capabilities.get(1).getCapabilities());
        Set<String> descriptorFields = Arrays.stream(
                        CpsVendorDescriptorRespVO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertFalse(descriptorFields.contains("appKey"));
        assertFalse(descriptorFields.contains("appSecret"));
        assertFalse(descriptorFields.contains("authToken"));
        assertFalse(descriptorFields.contains("extraConfig"));
    }

    @Test
    void tenantOwnership_cannotBeSelectedThroughLifecycleRequests() {
        for (Class<?> requestType : List.of(
                CpsPlatformOnboardingPageReqVO.class,
                CpsPlatformOnboardingDraftSaveReqVO.class,
                CpsPlatformOnboardingDraftDeleteReqVO.class,
                CpsPlatformOnboardingTestReqVO.class,
                CpsPlatformOnboardingPublishReqVO.class,
                CpsPlatformOnboardingLifecycleReqVO.class)) {
            assertFalse(Arrays.stream(requestType.getDeclaredFields())
                    .anyMatch(field -> field.getName().equalsIgnoreCase("tenantId")),
                    requestType.getSimpleName());
        }
    }

    private static CpsPlatformOnboardingPayloadRespVO completePayload() {
        return CpsPlatformOnboardingPayloadRespVO.builder()
                .platform(CpsOnboardingPlatformRespVO.builder()
                        .platformCode("taobao").platformName("淘宝").status(0).build())
                .primaryVendorCode("dataoke")
                .runtimeDefaultAdzoneId("general")
                .vendors(List.of(CpsOnboardingVendorRespVO.builder()
                        .vendorCode("dataoke").platformCode("taobao").status(1)
                        .appKeyConfigured(true).appSecretConfigured(true).build()))
                .adzones(List.of(CpsOnboardingAdzone.builder()
                        .platformCode("taobao").adzoneId("general")
                        .adzoneType("general").status(1).build()))
                .rebateRules(List.of(CpsOnboardingRebateRule.builder()
                        .platformCode("taobao").rebateRate(new BigDecimal("50.00"))
                        .priority(0).status(1).build()))
                .build();
    }

    private static CpsPlatformOnboardingDetailRespVO detail(
            Long id, CpsPlatformOnboardingPayloadRespVO payload, String status,
            String fingerprint, String validatedFingerprint) {
        CpsPlatformOnboardingDetailRespVO detail =
                new CpsPlatformOnboardingDetailRespVO();
        detail.setId(id);
        detail.setPlatformCode("taobao");
        detail.setPayload(payload);
        detail.setStatus(status);
        detail.setConfigFingerprint(fingerprint);
        detail.setValidatedFingerprint(validatedFingerprint);
        detail.setDraftVersion(1L);
        return detail;
    }

    private static CpsOnboardingRebateRule rebateRule(
            String platformCode, String rate, int priority) {
        return CpsOnboardingRebateRule.builder()
                .platformCode(platformCode)
                .rebateRate(new BigDecimal(rate))
                .priority(priority)
                .status(1)
                .build();
    }
}
