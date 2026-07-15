package com.qiji.cps.module.cps.service.order.status;

import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsPlatformOrderStatusMapperTest {

    @Test
    @DisplayName("mapStatus - 淘宝原始 tk_status 独立映射")
    void mapStatus_mapsTaobaoRawTkStatus() {
        assertEquals(CpsOrderStatusEnum.PAID.getStatus(), map("taobao", 12, 0));
        assertEquals(CpsOrderStatusEnum.RECEIVED.getStatus(), map("taobao", 14, 0));
        assertEquals(CpsOrderStatusEnum.SETTLED.getStatus(), map("taobao", 3, 0));
        assertEquals(CpsOrderStatusEnum.INVALID.getStatus(), map("taobao", 13, 0));
        assertEquals(CpsOrderStatusEnum.REFUNDED.getStatus(), map("taobao", 12, 1));
    }

    @Test
    @DisplayName("mapStatus - 京东 validCode 独立映射")
    void mapStatus_mapsJdValidCode() {
        assertEquals(CpsOrderStatusEnum.PAID.getStatus(), map("jd", 16, 0));
        assertEquals(CpsOrderStatusEnum.SETTLED.getStatus(), map("jd", 17, 0));
        assertEquals(CpsOrderStatusEnum.SETTLED.getStatus(), map("jingdong", 18, 0));
        assertEquals(CpsOrderStatusEnum.INVALID.getStatus(), map("jd", -1, 0));
    }

    @Test
    @DisplayName("mapStatus - 拼多多 orderStatus 独立映射")
    void mapStatus_mapsPddOrderStatus() {
        assertEquals(CpsOrderStatusEnum.CREATED.getStatus(), map("pdd", 0, 0));
        assertEquals(CpsOrderStatusEnum.PAID.getStatus(), map("pinduoduo", 1, 0));
        assertEquals(CpsOrderStatusEnum.SETTLED.getStatus(), map("pdd", 3, 0));
        assertEquals(CpsOrderStatusEnum.REBATE_RECEIVED.getStatus(), map("pdd", 4, 0));
    }

    @Test
    @DisplayName("mapStatus - 抖音与本地生活使用各自平台映射")
    void mapStatus_mapsDouyinAndLocalLifeStatuses() {
        assertEquals(CpsOrderStatusEnum.PAID.getStatus(), map("douyin", 1, 0));
        assertEquals(CpsOrderStatusEnum.SETTLED.getStatus(), map("douyin", 3, 0));
        assertEquals(CpsOrderStatusEnum.PAID.getStatus(), map("didi", 1, 0));
        assertEquals(CpsOrderStatusEnum.SETTLED.getStatus(), map("meituan", 3, 0));
        assertEquals(CpsOrderStatusEnum.REFUNDED.getStatus(), map("didi", -1, 1));
    }

    @Test
    @DisplayName("isLegalMigration - 已到账不可被低阶状态回退，退款/失效允许冲正")
    void isLegalMigration_rejectsDowngradeButAllowsReversal() {
        assertFalse(CpsPlatformOrderStatusMapper.isLegalMigration("taobao",
                CpsOrderStatusEnum.REBATE_RECEIVED.getStatus(), CpsOrderStatusEnum.SETTLED.getStatus()));
        assertFalse(CpsPlatformOrderStatusMapper.isLegalMigration("jd",
                CpsOrderStatusEnum.SETTLED.getStatus(), CpsOrderStatusEnum.PAID.getStatus()));
        assertTrue(CpsPlatformOrderStatusMapper.isLegalMigration("pdd",
                CpsOrderStatusEnum.SETTLED.getStatus(), CpsOrderStatusEnum.REFUNDED.getStatus()));
        assertTrue(CpsPlatformOrderStatusMapper.isLegalMigration("douyin",
                CpsOrderStatusEnum.PAID.getStatus(), CpsOrderStatusEnum.SETTLED.getStatus()));
    }

    private String map(String platformCode, Integer platformStatus, Integer refundTag) {
        return CpsPlatformOrderStatusMapper.mapStatus(CpsOrderDTO.builder()
                .platformCode(platformCode)
                .platformStatus(platformStatus)
                .refundTag(refundTag)
                .build());
    }
}
