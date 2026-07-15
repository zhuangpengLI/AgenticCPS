package com.qiji.cps.module.cps.service.goods.coupon;

import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolUsableReqVO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsCouponPoolDO;
import com.qiji.cps.module.cps.dal.mysql.goods.CpsCouponPoolMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsCouponPoolServiceImplTest {

    @InjectMocks
    private CpsCouponPoolServiceImpl service;

    @Mock
    private CpsCouponPoolMapper couponPoolMapper;

    @Test
    @DisplayName("listUsableCoupons - 仅返回有效期内、启用且有库存的券")
    void listUsableCoupons_filtersInvalidExpiredFutureAndOutOfStockCoupons() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 15, 0);
        service.setClockForTest(() -> now);
        CpsCouponPoolUsableReqVO reqVO = new CpsCouponPoolUsableReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setVendorCode("dataoke");
        reqVO.setExternalGoodsId("123456");
        reqVO.setGoodsSign("sign-1");
        when(couponPoolMapper.selectListByGoods("taobao", "dataoke", "123456", "sign-1"))
                .thenReturn(List.of(
                        coupon(1L, "VALID", now.minusDays(1), now.plusDays(1), 10),
                        coupon(2L, "VALID", now.minusDays(1), now.plusDays(1), null),
                        coupon(3L, "DISABLED", now.minusDays(1), now.plusDays(1), 10),
                        coupon(4L, "VALID", now.minusDays(3), now.minusDays(1), 10),
                        coupon(5L, "VALID", now.plusHours(1), now.plusDays(1), 10),
                        coupon(6L, "VALID", now.minusDays(1), now.plusDays(1), 0)));

        List<CpsCouponPoolDO> result = service.listUsableCoupons(reqVO);

        assertEquals(List.of(1L, 2L), result.stream().map(CpsCouponPoolDO::getId).toList());
    }

    private CpsCouponPoolDO coupon(Long id, String status, LocalDateTime startTime,
                                   LocalDateTime endTime, Integer stockRemain) {
        return CpsCouponPoolDO.builder()
                .id(id)
                .platformCode("taobao")
                .vendorCode("dataoke")
                .externalGoodsId("123456")
                .goodsSign("sign-1")
                .couponId("coupon-" + id)
                .couponName("券" + id)
                .couponAmount(200)
                .thresholdAmount(1000)
                .startTime(startTime)
                .endTime(endTime)
                .stockRemain(stockRemain)
                .status(status)
                .sourceType("VENDOR_SYNC")
                .lastSyncTime(startTime)
                .build();
    }
}
