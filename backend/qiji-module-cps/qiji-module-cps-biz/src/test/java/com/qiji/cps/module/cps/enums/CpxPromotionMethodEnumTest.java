package com.qiji.cps.module.cps.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpxPromotionMethodEnumTest {

    @Test
    @DisplayName("promotionMethod - 支持 CPS 主导的全量 CPX 计费模型")
    void promotionMethod_supportsCpsPrimaryCpxMethods() {
        assertEquals(8, CpxPromotionMethodEnum.values().length);
        assertTrue(Arrays.asList(CpxPromotionMethodEnum.values()).containsAll(Arrays.asList(
                CpxPromotionMethodEnum.CPS,
                CpxPromotionMethodEnum.CPA,
                CpxPromotionMethodEnum.CPL,
                CpxPromotionMethodEnum.CPM,
                CpxPromotionMethodEnum.CPC,
                CpxPromotionMethodEnum.OCPA,
                CpxPromotionMethodEnum.OCPC,
                CpxPromotionMethodEnum.MIXED)));

        assertEquals(CpxPromotionMethodEnum.CPS, CpxPromotionMethodEnum.of("cps"));
        assertEquals(CpxPromotionMethodEnum.OCPA, CpxPromotionMethodEnum.of("oCPA"));
    }

    @Test
    @DisplayName("promotionMethod - 标识 CPS 主线、事件账本、曝光/点击/优化模型")
    void promotionMethod_exposesBusinessFlags() {
        assertTrue(CpxPromotionMethodEnum.CPS.isCpsPrimary());
        assertFalse(CpxPromotionMethodEnum.CPS.requiresEventLedger());

        assertTrue(CpxPromotionMethodEnum.CPA.requiresEventLedger());
        assertTrue(CpxPromotionMethodEnum.CPL.requiresEventLedger());
        assertTrue(CpxPromotionMethodEnum.CPM.requiresEventLedger());
        assertTrue(CpxPromotionMethodEnum.CPC.requiresEventLedger());
        assertTrue(CpxPromotionMethodEnum.OCPA.requiresEventLedger());
        assertTrue(CpxPromotionMethodEnum.OCPC.requiresEventLedger());

        assertTrue(CpxPromotionMethodEnum.CPM.isImpressionBased());
        assertTrue(CpxPromotionMethodEnum.CPC.isClickBased());
        assertTrue(CpxPromotionMethodEnum.OCPC.isClickBased());
        assertTrue(CpxPromotionMethodEnum.OCPA.isOptimized());
        assertTrue(CpxPromotionMethodEnum.OCPC.isOptimized());
        assertTrue(CpxPromotionMethodEnum.MIXED.isDisplayOnly());
    }

    @Test
    @DisplayName("promotionMethod - 拒绝未知计费模型")
    void promotionMethod_rejectsUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> CpxPromotionMethodEnum.of("CPV"));
    }
}
