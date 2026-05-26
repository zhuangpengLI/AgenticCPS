package com.qiji.cps.module.cps.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

/**
 * CPX 推广计费模型。
 *
 * <p>CPS 仍是 AgenticCPS 的主业务；其他模型走 CPX 事件、转化和结算账本。</p>
 */
@Getter
@AllArgsConstructor
public enum CpxPromotionMethodEnum {

    CPS("CPS 成交返利", true, false, false, false, false, false),
    CPA("CPA 有效动作", false, true, false, false, false, false),
    CPL("CPL 有效线索", false, true, false, false, false, false),
    CPM("CPM 有效曝光", false, true, true, false, false, false),
    CPC("CPC 有效点击", false, true, false, true, false, false),
    OCPA("oCPA 优化转化", false, true, false, false, true, false),
    OCPC("oCPC 优化点击", false, true, false, true, true, false),
    MIXED("混合展示", false, true, false, false, false, true);

    private final String name;
    private final boolean cpsPrimary;
    private final boolean requiresEventLedger;
    private final boolean impressionBased;
    private final boolean clickBased;
    private final boolean optimized;
    private final boolean displayOnly;

    public boolean requiresEventLedger() {
        return requiresEventLedger;
    }

    public static CpxPromotionMethodEnum of(String value) {
        if (value == null || value.isBlank()) {
            return CPS;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (CpxPromotionMethodEnum method : values()) {
            if (method.name().equals(normalized)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unsupported CPX promotion method: " + value);
    }
}
