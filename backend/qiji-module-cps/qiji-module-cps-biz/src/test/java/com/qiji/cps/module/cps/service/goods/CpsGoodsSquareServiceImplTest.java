package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSelectionMeta;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSelectionOption;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.dataoke.DtkSelectionLibraryClient;
import com.qiji.cps.module.cps.client.selection.CpsTaobaoSelectionVendorClient;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareGoodsRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareLinkReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.selection.CpsSearchAssistVendorClient;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeItemMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeMapper;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.service.selection.CpsSelectionRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsGoodsSquareServiceImplTest {

    @InjectMocks
    private CpsGoodsSquareServiceImpl service;

    @Mock
    private CpsGoodsService goodsService;

    @Mock
    private CpsTransferRecordMapper transferRecordMapper;

    @Mock
    private CpsPlatformClientFactory platformClientFactory;

    @Mock
    private DtkSelectionLibraryClient dtkSelectionLibraryClient;

    @Mock
    private CpsSelectionThemeMapper selectionThemeMapper;

    @Mock
    private CpsSelectionThemeItemMapper selectionThemeItemMapper;

    @Test
    @DisplayName("getMeta - 淘宝选品元数据优先使用供应商真实接口")
    void getMeta_usesTaobaoSelectionVendorWhenAvailable() {
        TestSelectionVendorClient vendorClient = new TestSelectionVendorClient(CpsGoodsSelectionMeta.builder()
                .hotKeywords(List.of(CpsGoodsSelectionOption.of("phone", "手机")))
                .categories(List.of(CpsGoodsSelectionOption.of("10", "居家")))
                .metaSource("haodanku")
                .build());
        when(platformClientFactory.getVendorClient("haodanku", "taobao")).thenReturn(vendorClient);
        when(platformClientFactory.getVendorConfig("haodanku", "taobao")).thenReturn(CpsVendorConfig.builder().build());

        var result = service.getMeta("taobao", "haodanku");

        assertEquals("taobao", result.getPlatformCode());
        assertEquals("haodanku", result.getVendorCode());
        assertEquals("haodanku", result.getMetaSource());
        assertEquals("手机", result.getHotKeywords().get(0).getLabel());
        assertEquals("居家", result.getCategories().get(0).getLabel());
    }

    @Test
    @DisplayName("getMeta - 供应商异常时返回本地默认推荐")
    void getMeta_fallsBackToDefaultWhenVendorFails() {
        TestSelectionVendorClient vendorClient = new TestSelectionVendorClient(null);
        vendorClient.throwOnMeta = true;
        when(platformClientFactory.getVendorClient("haodanku", "taobao")).thenReturn(vendorClient);
        when(platformClientFactory.getVendorConfig("haodanku", "taobao")).thenReturn(CpsVendorConfig.builder().build());

        var result = service.getMeta("taobao", "haodanku");

        assertEquals("default", result.getMetaSource());
        assertEquals("haodanku", result.getVendorCode());
        assertEquals(false, result.getUsingVendorMeta());
        assertEquals(false, result.getHotKeywords().isEmpty());
        assertEquals(false, result.getCategories().isEmpty());
    }

    @Test
    @DisplayName("searchGoods - 淘宝选品筛选应透传到通用搜索请求")
    void searchGoods_passesTaobaoSelectionFilters() {
        CpsGoodsSquareSearchReqVO reqVO = new CpsGoodsSquareSearchReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setVendorCode("dataoke");
        reqVO.setKeyword("洗衣液");
        reqVO.setCategoryId("10");
        reqVO.setChannelCode("hot");
        reqVO.setMinCommissionRate(new BigDecimal("20"));
        reqVO.setMinMonthSales(1000L);
        reqVO.setCouponAmountMin(new BigDecimal("5"));
        reqVO.setTmallOnly(true);
        reqVO.setHaitaoOnly(true);
        reqVO.setGoldSellerOnly(true);
        reqVO.setTchaoshiOnly(true);
        reqVO.setJuhuasuanOnly(true);
        reqVO.setTaoqianggouOnly(true);
        reqVO.setInspectedGoodsOnly(true);
        reqVO.setFreeshipRemoteDistrict(true);
        reqVO.setCouponPriceUpperLimit(new BigDecimal("50"));
        reqVO.setHotRankMin(2L);
        reqVO.setCouponExpireDays(7);
        reqVO.setShopType("tmall");
        reqVO.setGoodsPerformance("coupon");
        reqVO.setCommercialOnly(true);
        reqVO.setPreSaleOnly(true);
        when(goodsService.searchGoods(eq("taobao"), any(), eq("dataoke"))).thenReturn(CpsGoodsSearchResult.builder()
                .list(List.of(buildGoodsItem("taobao", "goods-1")))
                .total(1L)
                .build());

        service.searchGoods(reqVO);

        ArgumentCaptor<com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest> captor =
                ArgumentCaptor.forClass(com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest.class);
        verify(goodsService).searchGoods(eq("taobao"), captor.capture(), eq("dataoke"));
        assertEquals("10", captor.getValue().getCategoryId());
        assertEquals("hot", captor.getValue().getChannelCode());
        assertEquals(new BigDecimal("20"), captor.getValue().getMinCommissionRate());
        assertEquals(1000L, captor.getValue().getMinMonthSales());
        assertEquals(new BigDecimal("5"), captor.getValue().getCouponAmountMin());
        assertEquals(true, captor.getValue().getTmallOnly());
        assertEquals(true, captor.getValue().getHaitaoOnly());
        assertEquals(true, captor.getValue().getGoldSellerOnly());
        assertEquals(true, captor.getValue().getTchaoshiOnly());
        assertEquals(true, captor.getValue().getJuhuasuanOnly());
        assertEquals(true, captor.getValue().getTaoqianggouOnly());
        assertEquals(true, captor.getValue().getInspectedGoodsOnly());
        assertEquals(true, captor.getValue().getFreeshipRemoteDistrict());
        assertEquals(new BigDecimal("50"), captor.getValue().getCouponPriceUpperLimit());
        assertEquals(2L, captor.getValue().getHotRankMin());
        assertEquals(7, captor.getValue().getCouponExpireDays());
        assertEquals("tmall", captor.getValue().getShopType());
        assertEquals("coupon", captor.getValue().getGoodsPerformance());
        assertEquals(true, captor.getValue().getCommercialOnly());
        assertEquals(true, captor.getValue().getPreSaleOnly());
    }

    @Test
    @DisplayName("searchByImage - 只允许淘宝大淘客图片搜并复用分页响应")
    void searchByImage_usesTaobaoDataokeImageSearch() {
        CpsGoodsSquareSearchReqVO reqVO = new CpsGoodsSquareSearchReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setVendorCode("dataoke");
        reqVO.setImageBase64("data:image/png;base64,QUJD");
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        when(goodsService.searchGoods(eq("taobao"), any(), eq("dataoke"))).thenReturn(CpsGoodsSearchResult.builder()
                .list(List.of(buildGoodsItem("taobao", "image-goods-1")))
                .total(1L)
                .build());

        var result = service.searchByImage(reqVO);

        ArgumentCaptor<com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest> captor =
                ArgumentCaptor.forClass(com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest.class);
        verify(goodsService).searchGoods(eq("taobao"), captor.capture(), eq("dataoke"));
        assertEquals("dataoke_image", captor.getValue().getSearchMode());
        assertEquals("QUJD", captor.getValue().getImageBase64());
        assertEquals(1L, result.getTotal());
        assertEquals("image-goods-1", result.getList().get(0).getGoodsId());
    }

    @Test
    @DisplayName("getHotKeywords - 应从大淘客搜索辅助接口获取热搜记录")
    void getHotKeywords_usesSearchAssistVendorClient() {
        TestSearchAssistVendorClient vendorClient = new TestSearchAssistVendorClient();
        when(platformClientFactory.getVendorClient("dataoke", "taobao")).thenReturn(vendorClient);
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(CpsVendorConfig.builder().build());

        var result = service.getHotKeywords("taobao", "dataoke", 2);

        assertEquals(2, vendorClient.hotKeywordType);
        assertEquals(2, result.size());
        assertEquals("螺蛳粉", result.get(0).getLabel());
    }

    @Test
    @DisplayName("suggestKeywords - 应从大淘客搜索辅助接口获取联想词")
    void suggestKeywords_usesSearchAssistVendorClient() {
        TestSearchAssistVendorClient vendorClient = new TestSearchAssistVendorClient();
        when(platformClientFactory.getVendorClient("dataoke", "taobao")).thenReturn(vendorClient);
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(CpsVendorConfig.builder().build());

        var result = service.suggestKeywords("taobao", "dataoke", "裙子", 1);

        assertEquals("裙子", vendorClient.suggestionKeyword);
        assertEquals(1, result.size());
        assertEquals("裙子套装", result.get(0).getLabel());
        assertEquals("128 个商品", result.get(0).getDescription());
    }

    @Test
    @DisplayName("getVendorGoods - 朋友圈素材应复用大淘客 friends-circle-list 清单")
    void getVendorGoods_usesDataokeFriendsCircleList() {
        CpsVendorConfig config = CpsVendorConfig.builder().build();
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(config);
        when(dtkSelectionLibraryClient.fetchThemeGoods(any(), eq(20), eq(config))).thenReturn(List.of(buildSquareGoods("goods-1")));

        var result = service.getVendorGoods("FRIENDS_CIRCLE", "taobao", "dataoke", 20);

        ArgumentCaptor<CpsSelectionRule> captor = ArgumentCaptor.forClass(CpsSelectionRule.class);
        verify(dtkSelectionLibraryClient).fetchThemeGoods(captor.capture(), eq(20), eq(config));
        assertEquals("/api/goods/friends-circle-list", captor.getValue().getGoodsListUrl());
        assertEquals(0, captor.getValue().getGoodsListParams().get("sort"));
        assertEquals("dataoke:FRIENDS_CIRCLE", result.getList().get(0).getSource());
    }

    @Test
    @DisplayName("getVendorGoods - 爆品榜单应复用大淘客 ranking-list 清单")
    void getVendorGoods_usesDataokeRankingList() {
        CpsVendorConfig config = CpsVendorConfig.builder().build();
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(config);
        when(dtkSelectionLibraryClient.fetchThemeGoods(any(), eq(30), eq(config))).thenReturn(List.of(buildSquareGoods("rank-1")));

        var result = service.getVendorGoods("RANKING", "taobao", "dataoke", 30);

        ArgumentCaptor<CpsSelectionRule> captor = ArgumentCaptor.forClass(CpsSelectionRule.class);
        verify(dtkSelectionLibraryClient).fetchThemeGoods(captor.capture(), eq(30), eq(config));
        assertEquals("/api/goods/get-ranking-list", captor.getValue().getGoodsListUrl());
        assertEquals(1, captor.getValue().getGoodsListParams().get("rankType"));
        assertEquals(1L, result.getTotal());
    }

    @Test
    @DisplayName("getSelectionThemeGoods - 商品广场主题应读取已启用的选品快照")
    void getSelectionThemeGoods_readsEnabledSelectionSnapshots() {
        CpsSelectionThemeDO theme = CpsSelectionThemeDO.builder()
                .id(107L)
                .themeCode("DTK_SCENE_PALLET_107")
                .themeName("爆品商品_团长主推")
                .vendorCode("dataoke")
                .build();
        CpsSelectionThemeItemDO item = CpsSelectionThemeItemDO.builder()
                .themeId(107L)
                .platformCode("taobao")
                .goodsId("goods-107")
                .title("斐思妮眼霜组合装")
                .actualPrice(new BigDecimal("68.00"))
                .commissionRate(new BigDecimal("20.00"))
                .status("ENABLED")
                .build();
        when(selectionThemeMapper.selectPublishedGoodsSquareByThemeCode("DTK_SCENE_PALLET_107"))
                .thenReturn(theme);
        when(selectionThemeItemMapper.selectEnabledListByThemeId(107L)).thenReturn(List.of(item));

        var result = service.getSelectionThemeGoods("DTK_SCENE_PALLET_107", 1, 20);

        assertEquals(1L, result.getTotal());
        assertEquals("goods-107", result.getList().get(0).getGoodsId());
        assertEquals("dataoke", result.getList().get(0).getVendorCode());
        assertEquals("selection-theme:DTK_SCENE_PALLET_107", result.getList().get(0).getSource());
        verify(goodsService, never()).searchGoods(any(), any(), any());
    }

    @Test
    @DisplayName("searchGoods - 非淘宝平台忽略淘系专属筛选")
    void searchGoods_ignoresTaobaoSelectionFiltersForOtherPlatforms() {
        CpsGoodsSquareSearchReqVO reqVO = new CpsGoodsSquareSearchReqVO();
        reqVO.setPlatformCode("jd");
        reqVO.setVendorCode("haodanku");
        reqVO.setKeyword("手机");
        reqVO.setCategoryId("10");
        reqVO.setMinCommissionRate(new BigDecimal("20"));
        when(goodsService.searchGoods(eq("jd"), any(), eq("haodanku"))).thenReturn(CpsGoodsSearchResult.builder()
                .list(List.of(buildGoodsItem("jd", "goods-2")))
                .total(1L)
                .build());

        service.searchGoods(reqVO);

        ArgumentCaptor<com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest> captor =
                ArgumentCaptor.forClass(com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest.class);
        verify(goodsService).searchGoods(eq("jd"), captor.capture(), eq("haodanku"));
        assertNull(captor.getValue().getCategoryId());
        assertNull(captor.getValue().getMinCommissionRate());
    }

    @Test
    @DisplayName("searchGoods - 未选择平台时用今日精选做全平台聚合")
    void searchGoods_usesDefaultKeywordForAllPlatforms() {
        CpsGoodsSquareSearchReqVO reqVO = new CpsGoodsSquareSearchReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        CpsGoodsItem item = buildGoodsItem("taobao", "goods-1");
        when(goodsService.searchGoodsAllPlatforms(any())).thenReturn(List.of(item));

        var result = service.searchGoods(reqVO);

        ArgumentCaptor<com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest> captor =
                ArgumentCaptor.forClass(com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest.class);
        verify(goodsService).searchGoodsAllPlatforms(captor.capture());
        assertEquals("今日精选", captor.getValue().getKeyword());
        assertEquals(1, result.getList().size());
        assertEquals("taobao", result.getList().get(0).getPlatformCode());
    }

    @Test
    @DisplayName("searchGoods - 指定平台时透传供应商覆盖")
    void searchGoods_usesPlatformAndVendorOverride() {
        CpsGoodsSquareSearchReqVO reqVO = new CpsGoodsSquareSearchReqVO();
        reqVO.setPlatformCode("jd");
        reqVO.setVendorCode("haodanku");
        reqVO.setKeyword("手机");
        reqVO.setPageNo(2);
        reqVO.setPageSize(10);
        when(goodsService.searchGoods(eq("jd"), any(), eq("haodanku"))).thenReturn(CpsGoodsSearchResult.builder()
                .list(List.of(buildGoodsItem("jd", "goods-2")))
                .total(1L)
                .pageNo(2)
                .pageSize(10)
                .build());

        var result = service.searchGoods(reqVO);

        verify(goodsService).searchGoods(eq("jd"), any(), eq("haodanku"));
        assertEquals(1L, result.getTotal());
        assertEquals("jd", result.getList().get(0).getPlatformCode());
    }

    @Test
    @DisplayName("generateLink - 转链成功才写入有效转链记录")
    void generateLink_insertsTransferRecordOnSuccess() {
        CpsGoodsSquareLinkReqVO reqVO = buildLinkReqVO();
        when(goodsService.resolvePromotionAdzoneId("taobao", 100L, "pid-1")).thenReturn("pid-1");
        when(goodsService.generatePromotionLink("taobao", "goods-1", null, 100L, "pid-1", "haodanku"))
                .thenReturn(CpsPromotionLinkResult.builder()
                        .shortUrl("https://s.example/1")
                        .longUrl("https://example.com/long")
                        .tpwd("￥abc￥")
                        .actualPrice(new BigDecimal("19.90"))
                        .commissionRate(new BigDecimal("10"))
                        .commissionAmount(new BigDecimal("1.99"))
                        .build());
        when(transferRecordMapper.insert(any(CpsTransferRecordDO.class))).thenAnswer(invocation -> {
            CpsTransferRecordDO record = invocation.getArgument(0);
            record.setId(9L);
            return 1;
        });

        var result = service.generateLink(reqVO);

        ArgumentCaptor<CpsTransferRecordDO> captor = ArgumentCaptor.forClass(CpsTransferRecordDO.class);
        verify(transferRecordMapper).insert(captor.capture());
        assertEquals("SUCCESS", result.getLinkStatus());
        assertEquals(9L, result.getTransferRecordId());
        assertEquals("pid-1", captor.getValue().getAdzoneId());
        assertEquals("https://s.example/1", captor.getValue().getPromotionUrl());
    }

    @Test
    @DisplayName("generateLink - 平台转链失败不写入无效记录")
    void generateLink_doesNotInsertWhenPlatformReturnsNull() {
        CpsGoodsSquareLinkReqVO reqVO = buildLinkReqVO();
        when(goodsService.resolvePromotionAdzoneId("taobao", 100L, "pid-1")).thenReturn("pid-1");
        when(goodsService.generatePromotionLink("taobao", "goods-1", null, 100L, "pid-1", "haodanku"))
                .thenReturn(null);

        var result = service.generateLink(reqVO);

        verify(transferRecordMapper, never()).insert(any(CpsTransferRecordDO.class));
        assertEquals("FAILED", result.getLinkStatus());
        assertNull(result.getTransferRecordId());
    }

    private CpsGoodsSquareLinkReqVO buildLinkReqVO() {
        CpsGoodsSquareLinkReqVO reqVO = new CpsGoodsSquareLinkReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setGoodsId("goods-1");
        reqVO.setMemberId(100L);
        reqVO.setAdzoneId("pid-1");
        reqVO.setVendorCode("haodanku");
        reqVO.setTitle("测试商品");
        reqVO.setOriginalContent("https://item.example/goods-1");
        return reqVO;
    }

    private CpsGoodsItem buildGoodsItem(String platformCode, String goodsId) {
        return CpsGoodsItem.builder()
                .platformCode(platformCode)
                .goodsId(goodsId)
                .title("测试商品")
                .actualPrice(new BigDecimal("19.90"))
                .commissionRate(new BigDecimal("10"))
                .commissionAmount(new BigDecimal("1.99"))
                .build();
    }

    private CpsGoodsSquareGoodsRespVO buildSquareGoods(String goodsId) {
        CpsGoodsSquareGoodsRespVO goods = new CpsGoodsSquareGoodsRespVO();
        goods.setPlatformCode("taobao");
        goods.setVendorCode("dataoke");
        goods.setGoodsId(goodsId);
        goods.setTitle("测试商品");
        goods.setSource("dataoke:FRIENDS_CIRCLE");
        return goods;
    }

    private static class TestSelectionVendorClient implements CpsApiVendorClient, CpsTaobaoSelectionVendorClient {

        private final CpsGoodsSelectionMeta meta;
        private boolean throwOnMeta;

        private TestSelectionVendorClient(CpsGoodsSelectionMeta meta) {
            this.meta = meta;
        }

        @Override
        public CpsGoodsSelectionMeta getSelectionMeta(CpsVendorConfig config) {
            if (throwOnMeta) {
                throw new IllegalStateException("vendor down");
            }
            return meta;
        }

        @Override public String getVendorCode() { return "haodanku"; }
        @Override public String getVendorType() { return "aggregator"; }
        @Override public String getPlatformCode() { return "taobao"; }
        @Override public CpsGoodsSearchResult searchGoods(com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest request, CpsVendorConfig config) { return null; }
        @Override public CpsPromotionLinkResult generatePromotionLink(com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest request, CpsVendorConfig config) { return null; }
        @Override public java.util.List<com.qiji.cps.module.cps.client.dto.CpsOrderDTO> queryOrders(com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest request, CpsVendorConfig config) { return List.of(); }
        @Override public boolean testConnection(CpsVendorConfig config) { return true; }
    }

    private static class TestSearchAssistVendorClient implements CpsApiVendorClient, CpsSearchAssistVendorClient {

        private Integer hotKeywordType;
        private String suggestionKeyword;

        @Override
        public List<CpsGoodsSelectionOption> getHotKeywords(Integer type, CpsVendorConfig config) {
            this.hotKeywordType = type;
            return List.of(CpsGoodsSelectionOption.of("螺蛳粉", "螺蛳粉"), CpsGoodsSelectionOption.of("耳机", "耳机"));
        }

        @Override
        public List<CpsGoodsSelectionOption> suggestKeywords(String keyword, Integer type, CpsVendorConfig config) {
            this.suggestionKeyword = keyword;
            return List.of(CpsGoodsSelectionOption.builder()
                    .value("裙子套装")
                    .label("裙子套装")
                    .description("128 个商品")
                    .build());
        }

        @Override public String getVendorCode() { return "dataoke"; }
        @Override public String getVendorType() { return "aggregator"; }
        @Override public String getPlatformCode() { return "taobao"; }
        @Override public CpsGoodsSearchResult searchGoods(com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest request, CpsVendorConfig config) { return null; }
        @Override public CpsPromotionLinkResult generatePromotionLink(com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest request, CpsVendorConfig config) { return null; }
        @Override public java.util.List<com.qiji.cps.module.cps.client.dto.CpsOrderDTO> queryOrders(com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest request, CpsVendorConfig config) { return List.of(); }
        @Override public boolean testConnection(CpsVendorConfig config) { return true; }
    }

}
