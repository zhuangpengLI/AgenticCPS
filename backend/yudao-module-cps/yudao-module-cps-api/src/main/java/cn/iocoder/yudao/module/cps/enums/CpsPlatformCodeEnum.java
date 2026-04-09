package cn.iocoder.yudao.module.cps.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CPS 平台编码枚举
 *
 * @author CPS System
 */
@Getter
@AllArgsConstructor
public enum CpsPlatformCodeEnum implements ArrayValuable<String> {

    TAOBAO("taobao", "淘宝联盟"),
    JD("jd", "京东联盟"),
    PDD("pdd", "拼多多联盟"),
    DOUYIN("douyin", "抖音联盟"),
    VIP("vip", "唯品会联盟"),
    MEITUAN("meituan", "美团联盟"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(CpsPlatformCodeEnum::getCode).toArray(String[]::new);

    /**
     * 平台编码
     */
    private final String code;
    /**
     * 平台名称
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static CpsPlatformCodeEnum getByCode(String code) {
        return Arrays.stream(values()).filter(e -> e.getCode().equals(code)).findFirst().orElse(null);
    }

}
