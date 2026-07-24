package com.qiji.cps.module.cps.service.onboarding;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorDescriptor;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsOnboardingVendorRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDetailRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingPageReqVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingPayloadRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsOnboardingPlatformRespVO;
import com.qiji.cps.module.cps.service.adzone.CpsAdzoneService;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_PLATFORM_ENABLED;

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
                        .appKeyConfigured(true).appSecretConfigured(true).build()))
                .adzones(java.util.List.of(CpsOnboardingAdzone.builder()
                        .platformCode("taobao").adzoneId("general").adzoneType("general")
                        .status(1).build()))
                .build();
        CpsPlatformOnboardingDetailRespVO detail = new CpsPlatformOnboardingDetailRespVO();
        detail.setPayload(payload);
        when(draftService.getRuntimeDetail("taobao")).thenReturn(detail);
        when(draftService.getDetail("taobao")).thenReturn(detail);
        when(clientFactory.getVendorDescriptor("dataoke", "taobao")).thenReturn(null);

        var item = service.getPage(new CpsPlatformOnboardingPageReqVO())
                .getList().get(0);
        assertEquals(60, item.getCompletionPercent());
        assertEquals(java.util.List.of("DEFAULT_REBATE", "CONNECTION_TEST"),
                item.getMissingItems());
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
    }
}
