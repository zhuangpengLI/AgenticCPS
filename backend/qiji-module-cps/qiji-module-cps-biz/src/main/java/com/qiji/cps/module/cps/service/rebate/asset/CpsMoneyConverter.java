package com.qiji.cps.module.cps.service.rebate.asset;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * CPS 资金元分转换的唯一入口。
 */
@Component
public class CpsMoneyConverter {

    private static final BigDecimal CENTS_PER_YUAN = BigDecimal.valueOf(100L);

    public long yuanToCent(BigDecimal amountYuan) {
        if (amountYuan == null) {
            return 0L;
        }
        return amountYuan.multiply(CENTS_PER_YUAN).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public BigDecimal centToYuan(long amountCent) {
        return BigDecimal.valueOf(amountCent).divide(CENTS_PER_YUAN, 2, RoundingMode.UNNECESSARY);
    }
}
