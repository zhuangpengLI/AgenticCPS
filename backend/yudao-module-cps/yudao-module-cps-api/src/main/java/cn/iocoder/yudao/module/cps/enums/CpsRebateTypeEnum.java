package cn.iocoder.yudao.module.cps.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CPS 返利类型枚举
 *
 * @author CPS System
 */
@Getter
@AllArgsConstructor
public enum CpsRebateTypeEnum implements ArrayValuable<String> {

    REBATE("rebate", "返利入账"),
    REFUND("refund", "返利扣回"),
    ADJUST("adjust", "系统调整"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(CpsRebateTypeEnum::getType).toArray(String[]::new);

    /**
     * 返利类型
     */
    private final String type;
    /**
     * 类型名称
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
