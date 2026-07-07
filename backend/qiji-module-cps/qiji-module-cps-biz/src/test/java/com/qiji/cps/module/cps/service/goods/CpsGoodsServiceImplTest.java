package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
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
        verify(adzoneService).getMemberAdzone("jd", 100L);
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
    @DisplayName("resolvePromotionAdzoneId - 会员专属推广位优先于平台默认推广位")
    void resolvePromotionAdzoneId_usesMemberAdzoneBeforePlatformDefault() {
        mockEnabledPlatform("jd", "platform-default-pid");
        when(adzoneService.getMemberAdzone("jd", 100L)).thenReturn(CpsAdzoneDO.builder()
                .platformCode("jd")
                .adzoneId("member-pid")
                .relationType("member")
                .relationId(100L)
                .status(1)
                .build());

        String adzoneId = service.resolvePromotionAdzoneId("jd", 100L, null);

        assertEquals("member-pid", adzoneId);
        verify(adzoneService).getMemberAdzone("jd", 100L);
    }

    @Test
    @DisplayName("generatePromotionLink - 未传推广位且存在会员专属推广位时使用会员专属推广位")
    void generatePromotionLink_usesMemberAdzoneWhenAvailable() {
        mockEnabledPlatform("taobao", "platform-default-pid");
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.generatePromotionLink(any())).thenReturn(CpsPromotionLinkResult.builder().build());
        when(adzoneService.getMemberAdzone("taobao", 100L)).thenReturn(CpsAdzoneDO.builder()
                .platformCode("taobao")
                .adzoneId("member-taobao-pid")
                .relationType("member")
                .relationId(100L)
                .externalSpecialId("SPECIAL-100")
                .status(1)
                .build());

        service.generatePromotionLink("taobao", "goods-1", null, 100L, null);

        ArgumentCaptor<CpsPromotionLinkRequest> captor = ArgumentCaptor.forClass(CpsPromotionLinkRequest.class);
        verify(platformClient).generatePromotionLink(captor.capture());
        assertEquals("member-taobao-pid", captor.getValue().getAdzoneId());
        assertEquals("100", captor.getValue().getExternalId());
        assertEquals("SPECIAL-100", captor.getValue().getSpecialId());
        assertEquals(3, captor.getValue().getOrderScene());
        assertEquals(null, captor.getValue().getChannelId());
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

    @Test
    @DisplayName("generatePromotionLink - 淘宝渠道推广位使用 relationId/channelId 场景归因")
    void generatePromotionLink_setsTaobaoChannelRelationAttribution() {
        mockEnabledPlatform("taobao", "platform-default-pid");
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.generatePromotionLink(any())).thenReturn(CpsPromotionLinkResult.builder().build());
        when(adzoneService.getMemberAdzone("taobao", 100L)).thenReturn(CpsAdzoneDO.builder()
                .platformCode("taobao")
                .adzoneId("channel-taobao-pid")
                .relationType("channel")
                .relationId(9001L)
                .externalRelationId("REL-9001")
                .status(1)
                .build());

        service.generatePromotionLink("taobao", "goods-1", null, 100L, null);

        ArgumentCaptor<CpsPromotionLinkRequest> captor = ArgumentCaptor.forClass(CpsPromotionLinkRequest.class);
        verify(platformClient).generatePromotionLink(captor.capture());
        assertEquals("channel-taobao-pid", captor.getValue().getAdzoneId());
        assertEquals("100", captor.getValue().getExternalId());
        assertEquals("REL-9001", captor.getValue().getRelationId());
        assertEquals("REL-9001", captor.getValue().getChannelId());
        assertEquals(2, captor.getValue().getOrderScene());
        assertEquals(null, captor.getValue().getSpecialId());
    }

    private void mockEnabledPlatform(String platformCode, String defaultAdzoneId) {
        CpsPlatformDO platform = new CpsPlatformDO();
        platform.setPlatformCode(platformCode);
        platform.setDefaultAdzoneId(defaultAdzoneId);
        platform.setStatus(1);
        when(platformService.getPlatformByCode(platformCode)).thenReturn(platform);
    }

}
