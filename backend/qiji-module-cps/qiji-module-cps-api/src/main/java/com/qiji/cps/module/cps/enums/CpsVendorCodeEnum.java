package com.qiji.cps.module.cps.enums;

import com.qiji.cps.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CPS API 供应商编码枚举
 *
 * <p>定义系统支持的所有 API 供应商（聚合平台 + 官方API）</p>
 *
 * @author CPS System
 */
@Getter
@AllArgsConstructor
public enum CpsVendorCodeEnum implements ArrayValuable<String> {

    DATAOKE("dataoke", "大淘客", "aggregator"),
    HAODANKU("haodanku", "好单库", "aggregator"),
    MIAOYOUQUAN("miaoyouquan", "喵有卷", "aggregator"),
    SHIHUIZHU("shihuizhu", "实惠猪", "aggregator"),
    OFFICIAL("official", "官方API", "official"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(CpsVendorCodeEnum::getCode).toArray(String[]::new);

    /**
     * 供应商编码
     */
    private final String code;
    /**
     * 供应商名称
     */
    private final String name;
    /**
     * 供应商类型：aggregator(聚合平台) / official(官方API)
     */
    private final String type;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static CpsVendorCodeEnum getByCode(String code) {
        return Arrays.stream(values()).filter(e -> e.getCode().equals(code)).findFirst().orElse(null);
    }

}
