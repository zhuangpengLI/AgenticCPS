package com.qiji.cps.module.cps.service.rebate.asset;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CpsMoneyConverterTest {

    private final CpsMoneyConverter converter = new CpsMoneyConverter();

    @Test
    void convertsYuanAndCentWithFinancialRounding() {
        assertEquals(1235L, converter.yuanToCent(new BigDecimal("12.345")));
        assertEquals(new BigDecimal("12.35"), converter.centToYuan(1235L));
        assertEquals(0L, converter.yuanToCent(null));
    }
}
