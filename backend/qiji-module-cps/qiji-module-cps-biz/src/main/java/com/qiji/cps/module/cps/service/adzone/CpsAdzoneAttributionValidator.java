package com.qiji.cps.module.cps.service.adzone;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Side-effect-free attribution validation shared by CRUD and onboarding readiness checks.
 */
public final class CpsAdzoneAttributionValidator {

    private static final String PLATFORM_TAOBAO = "taobao";
    private static final String TYPE_CHANNEL = "channel";
    private static final String TYPE_MEMBER = "member";
    private static final Pattern TAOBAO_PID_PATTERN = Pattern.compile("^mm_\\d+_\\d+_\\d+$");
    private static final Pattern TAOBAO_SPECIAL_ID_PATTERN = Pattern.compile("^\\d+$");

    private CpsAdzoneAttributionValidator() {
    }

    public static List<Violation> validate(String platformCode, String adzoneType,
                                           String relationType, Long relationId,
                                           String adzoneId, String externalRelationId,
                                           String externalSpecialId) {
        List<Violation> violations = new ArrayList<>();
        if (isTaobaoChannel(platformCode, adzoneType, relationType)) {
            if (relationId == null) {
                violations.add(new Violation(ViolationType.RELATION_REQUIRED,
                        "relationId", "渠道推广位必须填写渠道关系 ID"));
            }
            if (!StringUtils.hasText(externalRelationId)) {
                violations.add(new Violation(ViolationType.RELATION_REQUIRED,
                        "externalRelationId", "淘宝渠道推广位必须填写 relationId"));
            }
            return violations;
        }
        if (!isMember(adzoneType, relationType)) {
            return violations;
        }
        if (relationId == null) {
            violations.add(new Violation(ViolationType.RELATION_REQUIRED,
                    "relationId", "会员推广位必须填写会员关系 ID"));
        }
        if (!PLATFORM_TAOBAO.equalsIgnoreCase(platformCode)) {
            return violations;
        }
        if (!StringUtils.hasText(adzoneId)
                || !TAOBAO_PID_PATTERN.matcher(adzoneId.trim()).matches()) {
            violations.add(new Violation(ViolationType.CONFIG_INVALID,
                    "adzoneId", "淘宝会员 PID 必须使用 mm_数字_数字_数字 格式"));
        }
        if (!StringUtils.hasText(externalSpecialId)) {
            violations.add(new Violation(ViolationType.RELATION_REQUIRED,
                    "externalSpecialId", "淘宝会员专属推广位必须填写 specialId"));
        } else if (!TAOBAO_SPECIAL_ID_PATTERN.matcher(externalSpecialId.trim()).matches()) {
            violations.add(new Violation(ViolationType.CONFIG_INVALID,
                    "externalSpecialId", "淘宝会员专属推广位必须填写数字会员运营ID specialId"));
        }
        return violations;
    }

    public static boolean isTaobaoChannel(String platformCode, String adzoneType,
                                           String relationType) {
        return PLATFORM_TAOBAO.equalsIgnoreCase(platformCode)
                && (TYPE_CHANNEL.equalsIgnoreCase(adzoneType)
                || TYPE_CHANNEL.equalsIgnoreCase(relationType));
    }

    public static boolean isMember(String adzoneType, String relationType) {
        return TYPE_MEMBER.equalsIgnoreCase(adzoneType)
                || TYPE_MEMBER.equalsIgnoreCase(relationType);
    }

    public enum ViolationType {
        RELATION_REQUIRED,
        CONFIG_INVALID
    }

    public record Violation(ViolationType type, String field, String message) {
    }

}
