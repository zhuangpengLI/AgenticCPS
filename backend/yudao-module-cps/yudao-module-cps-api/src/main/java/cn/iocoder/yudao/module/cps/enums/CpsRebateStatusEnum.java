package cn.iocoder.yudao.module.cps.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CPS 返利状态枚举
 *
 * @author CPS System
 */
@Getter
@AllArgsConstructor
public enum CpsRebateStatusEnum implements ArrayValuable<String> {

    PENDING("pending", "待结算"),
    RECEIVED("received", "已到账"),
    REFUNDED("refunded", "已扣回"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(CpsRebateStatusEnum::getStatus).toArray(String[]::new);

    /**
     * 返利状态
     */
    private final String status;
    /**
     * 状态名称
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
