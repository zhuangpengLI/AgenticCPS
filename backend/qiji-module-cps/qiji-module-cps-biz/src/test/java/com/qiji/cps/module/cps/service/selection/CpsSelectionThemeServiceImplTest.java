package com.qiji.cps.module.cps.service.selection;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dataoke.DtkActivityVendorClient;
import com.qiji.cps.module.cps.client.dataoke.DtkSelectionLibraryClient;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.haodanku.activity.HaodankuActivityVendorClient;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareGoodsRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemImportReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemSortReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeSyncReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeVendorPullReqVO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeItemMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeMapper;
import com.qiji.cps.module.cps.service.goods.CpsGoodsSquareService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsSelectionThemeServiceImplTest {

    @InjectMocks
    private CpsSelectionThemeServiceImpl service;

    @Mock
    private CpsSelectionThemeMapper themeMapper;

    @Mock
    private CpsSelectionThemeItemMapper itemMapper;

    @Mock
    private CpsGoodsSquareService goodsSquareService;

    @Mock
    private CpsSelectionAiRecommendService aiRecommendService;

    @Mock
    private DtkActivityVendorClient dtkActivityVendorClient;

    @Mock
    private DtkSelectionLibraryClient dtkSelectionLibraryClient;

    @Mock
    private HaodankuActivityVendorClient haodankuActivityVendorClient;

    @Mock
    private CpsPlatformClientFactory platformClientFactory;

    @Test
    @DisplayName("createTheme - 主题编码租户内唯一并默认草稿")
    void createTheme_savesDraftThemeWithUniqueCode() {
        CpsSelectionThemeSaveReqVO reqVO = buildThemeReq();
        when(themeMapper.selectByThemeCode("618_PRE")).thenReturn(null);
        when(themeMapper.insert(any(CpsSelectionThemeDO.class))).thenAnswer(invocation -> {
            CpsSelectionThemeDO theme = invocation.getArgument(0);
            theme.setId(100L);
            return 1;
        });

        Long id = service.createTheme(reqVO);

        ArgumentCaptor<CpsSelectionThemeDO> captor = ArgumentCaptor.forClass(CpsSelectionThemeDO.class);
        verify(themeMapper).insert(captor.capture());
        assertEquals(100L, id);
        assertEquals("618_PRE", captor.getValue().getThemeCode());
        assertEquals(CpsSelectionConstants.ThemeStatus.DRAFT, captor.getValue().getStatus());
        assertEquals("{\"keywords\":[\"防晒霜\"]}", captor.getValue().getRuleJson());
    }

    @Test
    @DisplayName("createTheme - 重复主题编码拒绝保存")
    void createTheme_rejectsDuplicateCode() {
        CpsSelectionThemeSaveReqVO reqVO = buildThemeReq();
        when(themeMapper.selectByThemeCode("618_PRE")).thenReturn(CpsSelectionThemeDO.builder().id(1L).build());

        assertThrows(ServiceException.class, () -> service.createTheme(reqVO));

        verify(themeMapper, never()).insert(any(CpsSelectionThemeDO.class));
    }

    @Test
    @DisplayName("publishTheme - 只有存在启用商品快照时才允许发布")
    void publishTheme_requiresEnabledSnapshotItems() {
        when(themeMapper.selectById(100L)).thenReturn(CpsSelectionThemeDO.builder()
                .id(100L)
                .themeCode("618_PRE")
                .status(CpsSelectionConstants.ThemeStatus.DRAFT)
                .build());
        when(itemMapper.selectEnabledListByThemeId(100L)).thenReturn(List.of(
                CpsSelectionThemeItemDO.builder().id(1L).status(CpsSelectionConstants.ItemStatus.ENABLED).build()));

        service.publishTheme(100L);

        ArgumentCaptor<CpsSelectionThemeDO> captor = ArgumentCaptor.forClass(CpsSelectionThemeDO.class);
        verify(themeMapper).updateById(captor.capture());
        assertEquals(100L, captor.getValue().getId());
        assertEquals(CpsSelectionConstants.ThemeStatus.PUBLISHED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("updateTheme - 草稿编辑时可改为已发布并复用发布校验")
    void updateTheme_allowsDraftToPublishedWhenEnabledItemsExist() {
        when(themeMapper.selectById(100L)).thenReturn(CpsSelectionThemeDO.builder()
                .id(100L)
                .themeCode("618_PRE")
                .status(CpsSelectionConstants.ThemeStatus.DRAFT)
                .build());
        when(itemMapper.selectEnabledListByThemeId(100L)).thenReturn(List.of(
                CpsSelectionThemeItemDO.builder().id(1L).status(CpsSelectionConstants.ItemStatus.ENABLED).build()));
        CpsSelectionThemeSaveReqVO reqVO = buildThemeReq();
        reqVO.setId(100L);
        reqVO.setThemeName("618抢先购精选");
        reqVO.setStatus(CpsSelectionConstants.ThemeStatus.PUBLISHED);

        service.updateTheme(reqVO);

        ArgumentCaptor<CpsSelectionThemeDO> captor = ArgumentCaptor.forClass(CpsSelectionThemeDO.class);
        verify(themeMapper).updateById(captor.capture());
        assertEquals(100L, captor.getValue().getId());
        assertEquals("618抢先购精选", captor.getValue().getThemeName());
        assertEquals(CpsSelectionConstants.ThemeStatus.PUBLISHED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("importItems - 以主题+平台+供应商+商品ID+goodsSign 去重更新快照")
    void importItems_upsertsByThemeAndGoodsIdentity() {
        when(themeMapper.selectById(100L)).thenReturn(CpsSelectionThemeDO.builder().id(100L).build());
        when(itemMapper.selectOneByUnique(100L, "pdd", "haodanku", "goods-1", "sign-1"))
                .thenReturn(CpsSelectionThemeItemDO.builder().id(9L).build());

        CpsSelectionThemeItemImportReqVO reqVO = new CpsSelectionThemeItemImportReqVO();
        reqVO.setThemeId(100L);
        reqVO.setSourceType(CpsSelectionConstants.SourceType.MANUAL);
        reqVO.setItems(List.of(buildImportItem("pdd", "goods-1", "sign-1")));

        int count = service.importItems(reqVO);

        assertEquals(1, count);
        ArgumentCaptor<CpsSelectionThemeItemDO> captor = ArgumentCaptor.forClass(CpsSelectionThemeItemDO.class);
        verify(itemMapper).updateById(captor.capture());
        assertEquals(9L, captor.getValue().getId());
        assertEquals("goods-1", captor.getValue().getGoodsId());
        assertEquals(CpsSelectionConstants.SourceType.MANUAL, captor.getValue().getSourceType());
    }

    @Test
    @DisplayName("vendorPull - 主题规则转换为商品广场搜索并写入供应商快照")
    void vendorPull_searchesGoodsSquareAndImportsSnapshots() {
        when(themeMapper.selectById(100L)).thenReturn(CpsSelectionThemeDO.builder()
                .id(100L)
                .themeCode("618_PRE")
                .themeName("618预售")
                .platformCodes("taobao")
                .vendorCode("dataoke")
                .ruleJson("{\"keywords\":[\"防晒霜\"],\"minCommissionRate\":20,\"pullCount\":2,\"onlyCoupon\":true}")
                .build());
        when(goodsSquareService.searchGoods(any())).thenReturn(CpsGoodsSquareSearchRespVO.builder()
                .list(List.of(buildPulledGoods("taobao", "goods-1"), buildPulledGoods("taobao", "goods-2")))
                .total(2L)
                .build());

        CpsSelectionThemeVendorPullReqVO reqVO = new CpsSelectionThemeVendorPullReqVO();
        reqVO.setThemeId(100L);
        var result = service.vendorPull(reqVO);

        assertEquals(2, result.getImportedCount());
        ArgumentCaptor<com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO> searchCaptor =
                ArgumentCaptor.forClass(com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO.class);
        verify(goodsSquareService).searchGoods(searchCaptor.capture());
        assertEquals("防晒霜", searchCaptor.getValue().getKeyword());
        assertEquals("taobao", searchCaptor.getValue().getPlatformCode());
        assertEquals("dataoke", searchCaptor.getValue().getVendorCode());
        assertEquals(new BigDecimal("20"), searchCaptor.getValue().getMinCommissionRate());
        assertEquals(1, searchCaptor.getValue().getHasCoupon());
        verify(itemMapper, times(2)).insert(any(CpsSelectionThemeItemDO.class));
    }

    @Test
    @DisplayName("vendorPull - 活动标题搜索为空时提炼商品词并放宽条件重试")
    void vendorPull_fallsBackToDerivedGoodsKeywordsWhenActivityTitleReturnsEmpty() {
        when(themeMapper.selectById(100L)).thenReturn(CpsSelectionThemeDO.builder()
                .id(100L)
                .themeCode("DTK_GLOBAL")
                .themeName("天猫国际自营优惠福利攻略")
                .platformCodes("taobao")
                .vendorCode("dataoke")
                .ruleJson("{\"keywords\":[\"天猫国际自营优惠福利攻略\"],\"pullCount\":2,\"onlyCoupon\":true}")
                .build());
        when(goodsSquareService.searchGoods(any()))
                .thenReturn(CpsGoodsSquareSearchRespVO.builder().list(List.of()).total(0L).build())
                .thenReturn(CpsGoodsSquareSearchRespVO.builder().list(List.of()).total(0L).build())
                .thenReturn(CpsGoodsSquareSearchRespVO.builder()
                        .list(List.of(buildPulledGoods("taobao", "import-goods-1")))
                        .total(1L)
                        .build());

        CpsSelectionThemeVendorPullReqVO reqVO = new CpsSelectionThemeVendorPullReqVO();
        reqVO.setThemeId(100L);
        var result = service.vendorPull(reqVO);

        assertEquals(1, result.getImportedCount());
        ArgumentCaptor<CpsGoodsSquareSearchReqVO> searchCaptor =
                ArgumentCaptor.forClass(CpsGoodsSquareSearchReqVO.class);
        verify(goodsSquareService, times(3)).searchGoods(searchCaptor.capture());
        assertEquals("天猫国际自营优惠福利攻略", searchCaptor.getAllValues().get(0).getKeyword());
        assertEquals(1, searchCaptor.getAllValues().get(0).getHasCoupon());
        assertEquals("天猫国际", searchCaptor.getAllValues().get(1).getKeyword());
        assertNull(searchCaptor.getAllValues().get(1).getHasCoupon());
        assertEquals("进口", searchCaptor.getAllValues().get(2).getKeyword());
        assertNull(searchCaptor.getAllValues().get(2).getActivityTag());
        verify(itemMapper).insert(any(CpsSelectionThemeItemDO.class));
    }

    @Test
    @DisplayName("syncDataokeThemes - 按选品库清单同步大淘客主题并通过主题商品接口拉快照")
    void syncDataokeThemes_upsertsThemesAndImportsGoodsSnapshots() {
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(CpsVendorConfig.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .appKey("app-key")
                .appSecret("app-secret")
                .apiBaseUrl("https://openapi.dataoke.com/api")
                .build());
        when(dtkSelectionLibraryClient.fetchThemes(any(), any(), any())).thenReturn(CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .list(List.of(
                        CpsThirdPartyActivity.builder()
                                .sourceType("dataoke")
                                .externalActivityId("dtk:scene_pallet:2")
                                .activityName("爆品商品_淘金币玩法")
                                .activityType("爆品商品")
                                .platformCode("taobao")
                                .mainPic("https://img.example/coin.png")
                                .shortDesc("大淘客爆品商品清单")
                                .promotionCount(2)
                                .tagText("爆品商品")
                                .searchKeyword("淘金币玩法")
                                .extraFields(java.util.Map.of(
                                        "vendorThemeSource", "SCENE_PALLET",
                                        "externalThemeId", "2",
                                        "externalThemeName", "淘金币玩法",
                                        "themeListUrl", "/open-api/scene-pallet",
                                        "themeListParams", java.util.Map.of("version", "v1.0.0"),
                                        "goodsListUrl", "/open-api/goods/scene-pallet",
                                        "goodsListParams", java.util.Map.of("version", "v1.0.0", "id", 2, "sortType", 4)
                                ))
                                .build(),
                        CpsThirdPartyActivity.builder()
                                .sourceType("dataoke")
                                .externalActivityId("dtk:scene_pallet:3")
                                .activityName("爆品商品_红包签到")
                                .activityType("爆品商品")
                                .platformCode("taobao")
                                .searchKeyword("红包签到")
                                .extraFields(java.util.Map.of(
                                        "vendorThemeSource", "SCENE_PALLET",
                                        "externalThemeId", "3",
                                        "externalThemeName", "红包签到",
                                        "themeListUrl", "/open-api/scene-pallet",
                                        "themeListParams", java.util.Map.of("version", "v1.0.0"),
                                        "goodsListUrl", "/open-api/goods/scene-pallet",
                                        "goodsListParams", java.util.Map.of("version", "v1.0.0", "id", 3, "sortType", 4)
                                ))
                                .build()))
                .total(2L)
                .pageNo(1)
                .pageSize(20)
                .build());
        when(themeMapper.selectByThemeCode("DTK_SCENE_PALLET_2")).thenReturn(null);
        when(themeMapper.insert(any(CpsSelectionThemeDO.class))).thenAnswer(invocation -> {
            CpsSelectionThemeDO theme = invocation.getArgument(0);
            theme.setId(100L);
            return 1;
        });
        when(themeMapper.selectByThemeCode("DTK_SCENE_PALLET_3")).thenReturn(CpsSelectionThemeDO.builder()
                .id(200L)
                .themeCode("DTK_SCENE_PALLET_3")
                .status(CpsSelectionConstants.ThemeStatus.DRAFT)
                .build());
        when(dtkSelectionLibraryClient.fetchThemeGoods(any(), eq(1), any()))
                .thenReturn(List.of(buildPulledGoods("taobao", "goods-1")))
                .thenReturn(List.of(buildPulledGoods("taobao", "goods-2")));

        CpsSelectionThemeSyncReqVO reqVO = new CpsSelectionThemeSyncReqVO();
        reqVO.setSourceCode("SCENE_PALLET");
        reqVO.setThemeNamePrefix("爆品商品");
        reqVO.setSyncGoods(true);
        reqVO.setGoodsPullCount(1);
        var result = service.syncDataokeThemes(reqVO);

        assertEquals(2, result.getPulledCount());
        assertEquals(2, result.getImportedCount());
        ArgumentCaptor<CpsSelectionThemeDO> insertCaptor = ArgumentCaptor.forClass(CpsSelectionThemeDO.class);
        verify(themeMapper).insert(insertCaptor.capture());
        assertEquals("DTK_SCENE_PALLET_2", insertCaptor.getValue().getThemeCode());
        assertEquals("爆品商品_淘金币玩法", insertCaptor.getValue().getThemeName());
        assertEquals("VENDOR_COLUMN", insertCaptor.getValue().getThemeType());
        assertEquals("dataoke", insertCaptor.getValue().getVendorCode());
        assertEquals(CpsSelectionConstants.ThemeStatus.PUBLISHED, insertCaptor.getValue().getStatus());
        assertEquals(true, insertCaptor.getValue().getRuleJson().contains("/open-api/goods/scene-pallet"));
        assertEquals(true, insertCaptor.getValue().getRuleJson().contains("\"id\":2"));
        ArgumentCaptor<CpsSelectionThemeDO> updateCaptor = ArgumentCaptor.forClass(CpsSelectionThemeDO.class);
        verify(themeMapper, times(5)).updateById(updateCaptor.capture());
        assertEquals(CpsSelectionConstants.ThemeStatus.PUBLISHED, updateCaptor.getAllValues().stream()
                .filter(item -> Long.valueOf(200L).equals(item.getId()))
                .findFirst()
                .orElseThrow()
                .getStatus());
        verify(itemMapper, times(2)).insert(any(CpsSelectionThemeItemDO.class));
        verify(dtkActivityVendorClient, never()).fetchActivities(any(), any());
        verify(goodsSquareService, never()).searchGoods(any());
    }

    @Test
    @DisplayName("syncVendorThemes - 兼容好单库特色栏目主题并按主题规则导入商品")
    void syncVendorThemes_supportsHaodankuColumnThemes() {
        when(haodankuActivityVendorClient.fetchActivities(any(), any())).thenReturn(
                CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                        .list(List.of(
                                CpsThirdPartyActivity.builder()
                                        .sourceType("haodanku")
                                        .externalActivityId("hdk:featured")
                                        .activityName("精选活动")
                                        .activityType("特色栏目")
                                        .platformCode("taobao")
                                        .mainPic("https://img.example/featured.png")
                                        .shortDesc("好单库首页人工选品专题页数据")
                                        .promotionCount(20)
                                        .tagText("人工选品")
                                        .searchKeyword("精选活动")
                                        .build()))
                        .total(1L)
                        .pageNo(1)
                        .pageSize(20)
                        .build());
        when(themeMapper.selectByThemeCode("HDK_FEATURED")).thenReturn(null);
        when(themeMapper.insert(any(CpsSelectionThemeDO.class))).thenAnswer(invocation -> {
            CpsSelectionThemeDO theme = invocation.getArgument(0);
            theme.setId(300L);
            return 1;
        });
        when(goodsSquareService.searchGoods(any())).thenReturn(CpsGoodsSquareSearchRespVO.builder()
                .list(List.of(buildPulledGoods("taobao", "hdk-goods-1")))
                .total(1L)
                .build());

        CpsSelectionThemeSyncReqVO reqVO = new CpsSelectionThemeSyncReqVO();
        reqVO.setVendorCode("haodanku");
        reqVO.setSyncGoods(true);
        reqVO.setGoodsPullCount(1);
        var result = service.syncVendorThemes(reqVO);

        assertEquals(1, result.getPulledCount());
        assertEquals(1, result.getImportedCount());
        ArgumentCaptor<CpsSelectionThemeDO> themeCaptor = ArgumentCaptor.forClass(CpsSelectionThemeDO.class);
        verify(themeMapper).insert(themeCaptor.capture());
        assertEquals("HDK_FEATURED", themeCaptor.getValue().getThemeCode());
        assertEquals("haodanku", themeCaptor.getValue().getVendorCode());
        assertEquals("VENDOR_COLUMN", themeCaptor.getValue().getThemeType());
        assertEquals(CpsSelectionConstants.ThemeStatus.PUBLISHED, themeCaptor.getValue().getStatus());
        ArgumentCaptor<CpsGoodsSquareSearchReqVO> searchCaptor =
                ArgumentCaptor.forClass(CpsGoodsSquareSearchReqVO.class);
        verify(goodsSquareService).searchGoods(searchCaptor.capture());
        assertEquals("精选活动", searchCaptor.getValue().getKeyword());
        assertEquals("taobao", searchCaptor.getValue().getPlatformCode());
        assertEquals("haodanku", searchCaptor.getValue().getVendorCode());
    }

    @Test
    @DisplayName("updateItemSort - 批量更新商品排序和置顶状态")
    void updateItemSort_updatesSortAndTopFlag() {
        when(itemMapper.selectById(8L)).thenReturn(CpsSelectionThemeItemDO.builder().id(8L).themeId(100L).build());
        CpsSelectionThemeItemSortReqVO reqVO = new CpsSelectionThemeItemSortReqVO();
        reqVO.setThemeId(100L);
        reqVO.setItems(List.of(new CpsSelectionThemeItemSortReqVO.SortItem(8L, 1, 1)));

        service.updateItemSort(reqVO);

        ArgumentCaptor<CpsSelectionThemeItemDO> captor = ArgumentCaptor.forClass(CpsSelectionThemeItemDO.class);
        verify(itemMapper).updateById(captor.capture());
        assertEquals(8L, captor.getValue().getId());
        assertEquals(1, captor.getValue().getSort());
        assertEquals(1, captor.getValue().getTopFlag());
    }

    private CpsSelectionThemeSaveReqVO buildThemeReq() {
        CpsSelectionThemeSaveReqVO reqVO = new CpsSelectionThemeSaveReqVO();
        reqVO.setThemeCode("618_PRE");
        reqVO.setThemeName("618预售");
        reqVO.setThemeType("PROMOTION");
        reqVO.setPromotionEvent("618");
        reqVO.setPlatformCodes("taobao");
        reqVO.setVendorCode("dataoke");
        reqVO.setRuleJson("{\"keywords\":[\"防晒霜\"]}");
        reqVO.setSort(10);
        return reqVO;
    }

    private CpsSelectionThemeItemImportReqVO.ImportItem buildImportItem(String platformCode, String goodsId, String goodsSign) {
        CpsSelectionThemeItemImportReqVO.ImportItem item = new CpsSelectionThemeItemImportReqVO.ImportItem();
        item.setPlatformCode(platformCode);
        item.setVendorCode("haodanku");
        item.setGoodsId(goodsId);
        item.setGoodsSign(goodsSign);
        item.setTitle("测试商品");
        item.setActualPrice(new BigDecimal("19.90"));
        item.setCommissionRate(new BigDecimal("20"));
        item.setCommissionAmount(new BigDecimal("3.98"));
        return item;
    }

    private CpsGoodsSquareGoodsRespVO buildPulledGoods(String platformCode, String goodsId) {
        CpsGoodsSquareGoodsRespVO item = new CpsGoodsSquareGoodsRespVO();
        item.setPlatformCode(platformCode);
        item.setVendorCode("dataoke");
        item.setGoodsId(goodsId);
        item.setTitle("防晒霜");
        item.setActualPrice(new BigDecimal("49.90"));
        item.setCouponPrice(new BigDecimal("10"));
        item.setCommissionRate(new BigDecimal("20"));
        item.setCommissionAmount(new BigDecimal("9.98"));
        item.setMonthSales(5000L);
        return item;
    }
}
