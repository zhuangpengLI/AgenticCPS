package com.qiji.cps.module.cps.service.membergoods;

import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsIdentityReqVO;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsRecordSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.membergoods.CpsMemberGoodsRecordDO;
import com.qiji.cps.module.cps.dal.mysql.membergoods.CpsMemberGoodsRecordMapper;
import com.qiji.cps.module.cps.service.rebate.asset.CpsMoneyConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsMemberGoodsRecordServiceImplTest {

    @InjectMocks
    private CpsMemberGoodsRecordServiceImpl service;

    @Mock
    private CpsMemberGoodsRecordMapper recordMapper;

    @Spy
    private CpsMoneyConverter moneyConverter = new CpsMoneyConverter();

    @Test
    void duplicateFavoriteRefreshesSnapshotWithoutCreatingAnotherActiveRow() {
        AppCpsMemberGoodsRecordSaveReqVO reqVO = snapshotReq();
        CpsMemberGoodsRecordDO existing = CpsMemberGoodsRecordDO.builder().id(88L).build();
        when(recordMapper.selectActiveByIdentity(eq(1001L), eq("FAVORITE"), anyString()))
                .thenReturn(null, existing);

        service.createFavorite(1001L, reqVO);
        service.createFavorite(1001L, reqVO);

        ArgumentCaptor<CpsMemberGoodsRecordDO> inserted = ArgumentCaptor.forClass(CpsMemberGoodsRecordDO.class);
        verify(recordMapper).insert(inserted.capture());
        verify(recordMapper).updateById(any(CpsMemberGoodsRecordDO.class));
        assertEquals(64, inserted.getValue().getIdentityKey().length());
        assertEquals("taobao", inserted.getValue().getPlatformCode());
        assertEquals(1235L, inserted.getValue().getActualPriceCent());
        assertNull(inserted.getValue().getCouponPriceCent());
    }

    @Test
    void deleteFavoriteIsIdempotent() {
        AppCpsMemberGoodsIdentityReqVO reqVO = new AppCpsMemberGoodsIdentityReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setGoodsId("goods-1");
        when(recordMapper.deleteActiveByIdentity(eq(1001L), eq("FAVORITE"), anyString()))
                .thenReturn(1, 0);

        service.deleteFavorite(1001L, reqVO);
        service.deleteFavorite(1001L, reqVO);

        verify(recordMapper, times(2)).deleteActiveByIdentity(eq(1001L), eq("FAVORITE"), anyString());
    }

    @Test
    void recordBrowsePrunesEverythingAfterNewestOneHundred() {
        when(recordMapper.selectActiveByIdentity(eq(1001L), eq("BROWSE"), anyString())).thenReturn(null);
        List<CpsMemberGoodsRecordDO> newestFirst = new ArrayList<>();
        for (long id = 1; id <= 101; id++) {
            newestFirst.add(CpsMemberGoodsRecordDO.builder().id(id).build());
        }
        when(recordMapper.selectMemberList(1001L, "BROWSE")).thenReturn(newestFirst);

        service.recordBrowse(1001L, snapshotReq());

        verify(recordMapper).deleteById(101L);
    }

    private AppCpsMemberGoodsRecordSaveReqVO snapshotReq() {
        AppCpsMemberGoodsRecordSaveReqVO reqVO = new AppCpsMemberGoodsRecordSaveReqVO();
        reqVO.setPlatformCode(" Taobao ");
        reqVO.setGoodsId("goods-1");
        reqVO.setTitle("测试商品");
        reqVO.setOriginalPrice(new BigDecimal("20.00"));
        reqVO.setActualPrice(new BigDecimal("12.345"));
        reqVO.setCouponPrice(null);
        reqVO.setEstimateRebateAmount(new BigDecimal("1.20"));
        return reqVO;
    }
}
