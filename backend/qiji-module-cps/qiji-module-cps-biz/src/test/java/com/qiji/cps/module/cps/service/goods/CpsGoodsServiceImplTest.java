package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock
    private CpsGoodsAggregationExecutor goodsAggregationExecutor;

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

    @Test
    void searchGoods_rejectsPlatformWithoutSearchCapability() {
        mockEnabledPlatform("didi", "2002");
        when(platformClientFactory.getRequiredClient("didi")).thenReturn(platformClient);
        when(platformClient.supportsGoodsSearch()).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> service.searchGoods("didi", new com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest()));
        verify(platformClient, never()).searchGoods(any());
    }

    @Test
    void searchGoodsAllPlatforms_skipsClientsWithoutSearchCapability() {
        when(platformClientFactory.getEnabledClients()).thenReturn(java.util.List.of(platformClient));
        when(platformClient.supportsGoodsSearch()).thenReturn(false);

        assertEquals(java.util.List.of(), service.searchGoodsAllPlatforms(
                new com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest()));
        verify(platformClient, never()).searchGoods(any());
    }

    @Test
    @DisplayName("searchGoodsAllPlatforms - 支持平台并发搜索且同价商品按平台编码稳定排序")
    void searchGoodsAllPlatforms_runsConcurrentlyAndKeepsPlatformCodeOrderForEqualPrices() throws Exception {
        CpsPlatformClient taobao = mock(CpsPlatformClient.class);
        CpsPlatformClient jd = mock(CpsPlatformClient.class);
        when(taobao.getPlatformCode()).thenReturn("taobao");
        when(jd.getPlatformCode()).thenReturn("jd");
        when(taobao.supportsGoodsSearch()).thenReturn(true);
        when(jd.supportsGoodsSearch()).thenReturn(true);
        when(platformClientFactory.getEnabledClients()).thenReturn(List.of(taobao, jd));

        CountDownLatch bothStarted = new CountDownLatch(2);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        when(taobao.searchGoods(any())).thenAnswer(invocation -> concurrentResult(
                bothStarted, active, maxActive, "taobao-goods", "taobao"));
        when(jd.searchGoods(any())).thenAnswer(invocation -> concurrentResult(
                bothStarted, active, maxActive, "jd-goods", "jd"));

        try (CpsGoodsAggregationExecutor executor = new CpsGoodsAggregationExecutor(2, 4, Duration.ofSeconds(1))) {
            CpsGoodsServiceImpl concurrentService = new CpsGoodsServiceImpl(
                    platformClientFactory, platformService, adzoneService, executor);

            List<CpsGoodsItem> result = concurrentService.searchGoodsAllPlatforms(new CpsGoodsSearchRequest());

            assertTrue(maxActive.get() >= 2, "平台查询应发生重叠，而不是串行执行");
            assertEquals(List.of("jd-goods", "taobao-goods"),
                    result.stream().map(CpsGoodsItem::getGoodsId).toList());
        }
    }

    @Test
    @DisplayName("searchGoodsAllPlatforms - 慢平台超时和异常平台不会阻断健康平台")
    void searchGoodsAllPlatforms_timesOutAndIsolatesPlatformFailures() {
        CpsPlatformClient slow = searchableClient("slow");
        CpsPlatformClient failed = searchableClient("failed");
        CpsPlatformClient healthy = searchableClient("healthy");
        when(platformClientFactory.getEnabledClients()).thenReturn(List.of(slow, failed, healthy));
        when(slow.searchGoods(any())).thenAnswer(invocation -> {
            Thread.sleep(1_000);
            return goodsResult("slow-goods", "slow");
        });
        when(failed.searchGoods(any())).thenThrow(new IllegalStateException("upstream unavailable"));
        when(healthy.searchGoods(any())).thenReturn(goodsResult("healthy-goods", "healthy"));

        try (CpsGoodsAggregationExecutor executor = new CpsGoodsAggregationExecutor(3, 4, Duration.ofMillis(100))) {
            CpsGoodsServiceImpl concurrentService = new CpsGoodsServiceImpl(
                    platformClientFactory, platformService, adzoneService, executor);
            long startedAt = System.nanoTime();

            List<CpsGoodsItem> result = concurrentService.searchGoodsAllPlatforms(new CpsGoodsSearchRequest());

            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            assertTrue(elapsedMillis < 500, "聚合不应等待慢平台完整返回，耗时=" + elapsedMillis + "ms");
            assertEquals(List.of("healthy-goods"), result.stream().map(CpsGoodsItem::getGoodsId).toList());
        }
    }

    private CpsGoodsSearchResult concurrentResult(CountDownLatch bothStarted, AtomicInteger active,
                                                   AtomicInteger maxActive, String goodsId,
                                                   String platformCode) throws InterruptedException {
        int current = active.incrementAndGet();
        maxActive.accumulateAndGet(current, Math::max);
        bothStarted.countDown();
        bothStarted.await(300, TimeUnit.MILLISECONDS);
        active.decrementAndGet();
        return goodsResult(goodsId, platformCode);
    }

    private CpsPlatformClient searchableClient(String platformCode) {
        CpsPlatformClient client = mock(CpsPlatformClient.class);
        lenient().when(client.getPlatformCode()).thenReturn(platformCode);
        when(client.supportsGoodsSearch()).thenReturn(true);
        return client;
    }

    private CpsGoodsSearchResult goodsResult(String goodsId, String platformCode) {
        return CpsGoodsSearchResult.builder()
                .list(List.of(CpsGoodsItem.builder()
                        .goodsId(goodsId)
                        .platformCode(platformCode)
                        .actualPrice(new BigDecimal("10.00"))
                        .build()))
                .build();
    }

    private void mockEnabledPlatform(String platformCode, String defaultAdzoneId) {
        CpsPlatformDO platform = new CpsPlatformDO();
        platform.setPlatformCode(platformCode);
        platform.setDefaultAdzoneId(defaultAdzoneId);
        platform.setStatus(1);
        when(platformService.getPlatformByCode(platformCode)).thenReturn(platform);
    }

}
