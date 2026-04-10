package com.qiji.cps.module.cps.enums;

import com.qiji.cps.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CPS 冻结状态枚举
 *
 * @author CPS System
 */
@Getter
@AllArgsConstructor
public enum CpsFreezeStatusEnum implements ArrayValuable<String> {

    PENDING("pending", "待冻结"),
    FROZEN("frozen", "已冻结"),
    UNFREEZING("unfreezing", "解冻中"),
    UNFREEZED("unfreezed", "已解冻"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(CpsFreezeStatusEnum::getStatus).toArray(String[]::new);

    /**
     * 冻结状态
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
