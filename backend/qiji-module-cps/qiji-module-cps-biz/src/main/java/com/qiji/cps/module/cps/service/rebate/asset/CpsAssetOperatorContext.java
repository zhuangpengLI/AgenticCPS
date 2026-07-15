package com.qiji.cps.module.cps.service.rebate.asset;

import org.springframework.util.StringUtils;

/**
 * 资产操作主体与审计上下文。
 */
public record CpsAssetOperatorContext(String operatorType, String operatorId,
                                      String idempotencyKey, String reason) {

    public CpsAssetOperatorContext {
        if (!StringUtils.hasText(operatorType) || !StringUtils.hasText(operatorId)
                || !StringUtils.hasText(idempotencyKey)
                || !StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("资产操作必须包含主体类型、主体ID、幂等键和原因");
        }
    }

    public static CpsAssetOperatorContext system(String idempotencyKey, String reason) {
        return new CpsAssetOperatorContext("SYSTEM", "system", idempotencyKey, reason);
    }

    public static CpsAssetOperatorContext admin(String operatorId, String idempotencyKey, String reason) {
        return new CpsAssetOperatorContext("ADMIN", operatorId, idempotencyKey, reason);
    }

    public static CpsAssetOperatorContext member(Long memberId, String idempotencyKey, String reason) {
        return new CpsAssetOperatorContext("MEMBER", String.valueOf(memberId), idempotencyKey, reason);
    }
}
