package com.qiji.cps.module.cps.service.goods.master;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsMasterDO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsPriceSnapshotDO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsSourceMappingDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.mysql.goods.CpsGoodsMasterMapper;
import com.qiji.cps.module.cps.dal.mysql.goods.CpsGoodsPriceSnapshotMapper;
import com.qiji.cps.module.cps.dal.mysql.goods.CpsGoodsSourceMappingMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeItemMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsGoodsMasterServiceImplTest {

    @InjectMocks
    private CpsGoodsMasterServiceImpl service;

    @Mock
    private CpsSelectionThemeItemMapper selectionThemeItemMapper;

    @Mock
    private CpsGoodsMasterMapper goodsMasterMapper;

    @Mock
    private CpsGoodsSourceMappingMapper sourceMappingMapper;

    @Mock
    private CpsGoodsPriceSnapshotMapper priceSnapshotMapper;

    @Test
    @DisplayName("importSelectionItem - 新来源快照创建主档、来源映射和价格快照")
    void importSelectionItem_createsMasterMappingAndPriceSnapshot() {
        CpsSelectionThemeItemDO item = buildSelectionItem();
        when(selectionThemeItemMapper.selectById(8L)).thenReturn(item);
        when(sourceMappingMapper.selectBySourceKey("taobao", "dataoke", "123456", "sign-1"))
                .thenReturn(null);
        when(goodsMasterMapper.insert(any(CpsGoodsMasterDO.class))).thenAnswer(invocation -> {
            CpsGoodsMasterDO master = invocation.getArgument(0);
            master.setId(500L);
            return 1;
        });
        when(sourceMappingMapper.insert(any(CpsGoodsSourceMappingDO.class))).thenAnswer(invocation -> {
            CpsGoodsSourceMappingDO mapping = invocation.getArgument(0);
            mapping.setId(600L);
            return 1;
        });

        Long masterId = service.importSelectionItem(8L);

        ArgumentCaptor<CpsGoodsMasterDO> masterCaptor = ArgumentCaptor.forClass(CpsGoodsMasterDO.class);
        ArgumentCaptor<CpsGoodsSourceMappingDO> mappingCaptor = ArgumentCaptor.forClass(CpsGoodsSourceMappingDO.class);
        ArgumentCaptor<CpsGoodsPriceSnapshotDO> snapshotCaptor = ArgumentCaptor.forClass(CpsGoodsPriceSnapshotDO.class);
        verify(goodsMasterMapper).insert(masterCaptor.capture());
        verify(sourceMappingMapper).insert(mappingCaptor.capture());
        verify(priceSnapshotMapper).insert(snapshotCaptor.capture());

        assertEquals(500L, masterId);
        assertEquals("TAOBAO_DATAOKE_123456_SIGN_1", masterCaptor.getValue().getMasterCode());
        assertEquals("防晒霜旗舰款", masterCaptor.getValue().getStandardTitle());
        assertEquals("品牌A", masterCaptor.getValue().getBrandName());
        assertEquals("美妆", masterCaptor.getValue().getCategoryName());
        assertEquals(500L, mappingCaptor.getValue().getMasterId());
        assertEquals("taobao", mappingCaptor.getValue().getPlatformCode());
        assertEquals("dataoke", mappingCaptor.getValue().getVendorCode());
        assertEquals("123456", mappingCaptor.getValue().getExternalGoodsId());
        assertEquals("sign-1", mappingCaptor.getValue().getGoodsSign());
        assertEquals(500L, snapshotCaptor.getValue().getMasterId());
        assertEquals(600L, snapshotCaptor.getValue().getSourceMappingId());
        assertEquals(1299, snapshotCaptor.getValue().getOriginalPrice());
        assertEquals(999, snapshotCaptor.getValue().getActualPrice());
        assertEquals(200, snapshotCaptor.getValue().getCouponPrice());
        assertEquals(new BigDecimal("18.50"), snapshotCaptor.getValue().getCommissionRate());
        assertEquals(188, snapshotCaptor.getValue().getCommissionAmount());
        assertEquals(321L, snapshotCaptor.getValue().getMonthSales());
    }

    @Test
    @DisplayName("importSelectionItem - 已存在来源映射时复用主档并追加价格快照")
    void importSelectionItem_reusesExistingMappingAndAppendsPriceSnapshot() {
        CpsSelectionThemeItemDO item = buildSelectionItem();
        when(selectionThemeItemMapper.selectById(8L)).thenReturn(item);
        when(sourceMappingMapper.selectBySourceKey("taobao", "dataoke", "123456", "sign-1"))
                .thenReturn(CpsGoodsSourceMappingDO.builder()
                        .id(600L)
                        .masterId(500L)
                        .platformCode("taobao")
                        .vendorCode("dataoke")
                        .externalGoodsId("123456")
                        .goodsSign("sign-1")
                        .build());

        Long masterId = service.importSelectionItem(8L);

        ArgumentCaptor<CpsGoodsPriceSnapshotDO> snapshotCaptor = ArgumentCaptor.forClass(CpsGoodsPriceSnapshotDO.class);
        assertEquals(500L, masterId);
        verify(goodsMasterMapper, never()).insert(any(CpsGoodsMasterDO.class));
        verify(sourceMappingMapper, never()).insert(any(CpsGoodsSourceMappingDO.class));
        verify(sourceMappingMapper).updateById(any(CpsGoodsSourceMappingDO.class));
        verify(priceSnapshotMapper).insert(snapshotCaptor.capture());
        assertEquals(500L, snapshotCaptor.getValue().getMasterId());
        assertEquals(600L, snapshotCaptor.getValue().getSourceMappingId());
    }

    @Test
    @DisplayName("importSelectionItem - 选品快照不存在时拒绝导入")
    void importSelectionItem_rejectsMissingSelectionItem() {
        when(selectionThemeItemMapper.selectById(8L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> service.importSelectionItem(8L));

        verify(goodsMasterMapper, never()).insert(any(CpsGoodsMasterDO.class));
        verify(sourceMappingMapper, never()).insert(any(CpsGoodsSourceMappingDO.class));
        verify(priceSnapshotMapper, never()).insert(any(CpsGoodsPriceSnapshotDO.class));
    }

    private CpsSelectionThemeItemDO buildSelectionItem() {
        return CpsSelectionThemeItemDO.builder()
                .id(8L)
                .themeId(100L)
                .platformCode("taobao")
                .vendorCode("dataoke")
                .goodsId("123456")
                .goodsSign("sign-1")
                .title("防晒霜旗舰款")
                .mainPic("https://img.example/goods.jpg")
                .originalPrice(new BigDecimal("12.99"))
                .actualPrice(new BigDecimal("9.99"))
                .couponPrice(new BigDecimal("2.00"))
                .commissionRate(new BigDecimal("18.50"))
                .commissionAmount(new BigDecimal("1.88"))
                .monthSales(321L)
                .shopName("旗舰店")
                .brandName("品牌A")
                .categoryName("美妆")
                .activityTag("618")
                .itemLink("https://item.example/123456")
                .rawData("{\"source\":\"selection\"}")
                .snapshotTime(LocalDateTime.of(2026, 7, 14, 10, 0))
                .build();
    }
}
