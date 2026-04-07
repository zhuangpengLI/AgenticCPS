package cn.iocoder.yudao.module.cps.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CPS 提现状态枚举
 *
 * @author CPS System
 */
@Getter
@AllArgsConstructor
public enum CpsWithdrawStatusEnum implements ArrayValuable<String> {

    CREATED("created", "已申请"),
    REVIEWING("reviewing", "审核中"),
    PASSED("passed", "审核通过"),
    REJECTED("rejected", "审核驳回"),
    SUCCESS("success", "提现成功"),
    FAILED("failed", "提现失败"),
    REFUNDED("refunded", "已退回"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(CpsWithdrawStatusEnum::getStatus).toArray(String[]::new);

    /**
     * 提现状态
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
