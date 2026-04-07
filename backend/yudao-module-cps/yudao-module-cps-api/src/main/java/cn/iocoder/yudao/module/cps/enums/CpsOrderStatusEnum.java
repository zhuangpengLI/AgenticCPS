package cn.iocoder.yudao.module.cps.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CPS 订单状态枚举
 *
 * @author CPS System
 */
@Getter
@AllArgsConstructor
public enum CpsOrderStatusEnum implements ArrayValuable<String> {

    CREATED("created", "已下单"),
    PAID("paid", "已付款"),
    RECEIVED("received", "已收货"),
    SETTLED("settled", "已结算"),
    REBATE_RECEIVED("rebate_received", "已到账"),
    REFUNDED("refunded", "已退款"),
    INVALID("invalid", "已失效"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(CpsOrderStatusEnum::getStatus).toArray(String[]::new);

    /**
     * 订单状态
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

    public static CpsOrderStatusEnum getByStatus(String status) {
        return Arrays.stream(values()).filter(e -> e.getStatus().equals(status)).findFirst().orElse(null);
    }

}
