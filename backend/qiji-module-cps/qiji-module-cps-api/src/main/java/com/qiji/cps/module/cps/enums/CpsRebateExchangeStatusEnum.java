package com.qiji.cps.module.cps.enums;

import com.qiji.cps.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CpsRebateExchangeStatusEnum implements ArrayValuable<String> {

    INIT("INIT", "已创建"),
    FROZEN("FROZEN", "返利已冻结"),
    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "兑换成功"),
    FAILED("FAILED", "兑换失败"),
    CANCELED("CANCELED", "已取消"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(CpsRebateExchangeStatusEnum::getStatus).toArray(String[]::new);

    private final String status;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
