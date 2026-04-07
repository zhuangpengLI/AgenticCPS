package cn.iocoder.yudao.module.cps.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CPS 推广位类型枚举
 *
 * @author CPS System
 */
@Getter
@AllArgsConstructor
public enum CpsAdzoneTypeEnum implements ArrayValuable<String> {

    GENERAL("general", "通用"),
    CHANNEL("channel", "渠道专属"),
    MEMBER("member", "用户专属"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(CpsAdzoneTypeEnum::getType).toArray(String[]::new);

    /**
     * 推广位类型
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
