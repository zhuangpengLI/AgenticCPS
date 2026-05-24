package com.qiji.cps.module.cps.service.selection;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareGoodsRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemImportReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemSortReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeSaveReqVO;
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
