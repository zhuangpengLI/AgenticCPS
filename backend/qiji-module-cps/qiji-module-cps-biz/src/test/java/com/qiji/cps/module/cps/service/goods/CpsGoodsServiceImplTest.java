package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.service.adzone.CpsAdzoneService;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CpsGoodsServiceImplTest {

    @InjectMocks
    private CpsGoodsServiceImpl service;

    @Mock
    private CpsPlatformClientFactory platformClientFactory;

    @Mock
    private CpsPlatformService platformService;

    @Mock
    private CpsAdzoneService adzoneService;

    @Mock
    private CpsPlatformClient platformClient;

    @BeforeEach
    void setUp() {
        lenient().when(platformClientFactory.withVendorCode(any(), any())).thenAnswer(invocation -> {
            java.util.function.Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });
    }

    @Test
    @DisplayName("generatePromotionLink - 未传推广位时使用平台默认推广位")
    void generatePromotionLink_usesPlatformDefaultAdzoneWhenNotSpecified() {
        mockEnabledPlatform("jd", "platform-default-pid");
        when(platformClientFactory.getRequiredClient("jd")).thenReturn(platformClient);
        when(platformClient.generatePromotionLink(any())).thenReturn(CpsPromotionLinkResult.builder().build());

        service.generatePromotionLink("jd", "goods-1", null, 100L, null);

        ArgumentCaptor<CpsPromotionLinkRequest> captor = ArgumentCaptor.forClass(CpsPromotionLinkRequest.class);
        verify(platformClient).generatePromotionLink(captor.capture());
        assertEquals("platform-default-pid", captor.getValue().getAdzoneId());
        verifyNoInteractions(adzoneService);
    }

    @Test
    @DisplayName("generatePromotionLink - 显式推广位覆盖平台默认推广位")
    void generatePromotionLink_usesExplicitAdzoneBeforePlatformDefault() {
        mockEnabledPlatform("jd", "platform-default-pid");
        when(platformClientFactory.getRequiredClient("jd")).thenReturn(platformClient);
        when(platformClient.generatePromotionLink(any())).thenReturn(CpsPromotionLinkResult.builder().build());

        service.generatePromotionLink("jd", "goods-1", null, 100L, "explicit-pid");

        ArgumentCaptor<CpsPromotionLinkRequest> captor = ArgumentCaptor.forClass(CpsPromotionLinkRequest.class);
        verify(platformClient).generatePromotionLink(captor.capture());
        assertEquals("explicit-pid", captor.getValue().getAdzoneId());
        verifyNoInteractions(adzoneService);
    }

    @Test
    @DisplayName("resolvePromotionAdzoneId - 会员专属推广位不覆盖平台默认推广位")
    void resolvePromotionAdzoneId_doesNotUseMemberAdzoneAsDefault() {
        mockEnabledPlatform("jd", "platform-default-pid");

        String adzoneId = service.resolvePromotionAdzoneId("jd", 100L, null);

        assertEquals("platform-default-pid", adzoneId);
        verifyNoInteractions(adzoneService);
    }

    @Test
    @DisplayName("generatePromotionLink - 指定供应商只作用于本次转链")
    void generatePromotionLink_usesVendorOverrideForCurrentCall() {
        mockEnabledPlatform("taobao", "platform-default-pid");
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.generatePromotionLink(any())).thenReturn(CpsPromotionLinkResult.builder().build());

        service.generatePromotionLink("taobao", "goods-1", null, 100L, null, "haodanku");

        verify(platformClientFactory).withVendorCode(eq("haodanku"), any());
        verify(platformClient).generatePromotionLink(any());
    }

    @Test
    @DisplayName("generatePromotionLink - 会员ID同时写入 externalId 和 channelId 用于各平台订单归因")
    void generatePromotionLink_setsExternalIdAndChannelIdForAttribution() {
        mockEnabledPlatform("jd", "platform-default-pid");
        when(platformClientFactory.getRequiredClient("jd")).thenReturn(platformClient);
        when(platformClient.generatePromotionLink(any())).thenReturn(CpsPromotionLinkResult.builder().build());

        service.generatePromotionLink("jd", "goods-1", null, 100L, null);

        ArgumentCaptor<CpsPromotionLinkRequest> captor = ArgumentCaptor.forClass(CpsPromotionLinkRequest.class);
        verify(platformClient).generatePromotionLink(captor.capture());
        assertEquals("100", captor.getValue().getExternalId());
        assertEquals("100", captor.getValue().getChannelId());
    }

    private void mockEnabledPlatform(String platformCode, String defaultAdzoneId) {
        CpsPlatformDO platform = new CpsPlatformDO();
        platform.setPlatformCode(platformCode);
        platform.setDefaultAdzoneId(defaultAdzoneId);
        platform.setStatus(1);
        when(platformService.getPlatformByCode(platformCode)).thenReturn(platform);
    }

}
