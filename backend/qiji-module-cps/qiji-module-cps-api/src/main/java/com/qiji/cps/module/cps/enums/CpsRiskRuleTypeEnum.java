package com.qiji.cps.module.cps.enums;

import com.qiji.cps.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CPS 风控规则类型枚举
 *
 * @author CPS System
 */
@Getter
@AllArgsConstructor
public enum CpsRiskRuleTypeEnum implements ArrayValuable<String> {

    RATE_LIMIT("rate_limit", "频率限制"),
    BLACKLIST("blacklist", "黑名单"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(CpsRiskRuleTypeEnum::getType).toArray(String[]::new);

    /**
     * 规则类型编码
     */
    private final String type;
    /**
     * 规则类型名称
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
