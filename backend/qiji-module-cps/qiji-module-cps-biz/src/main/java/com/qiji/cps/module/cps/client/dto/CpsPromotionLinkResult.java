package com.qiji.cps.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * CPS 推广链接生成结果 DTO（平台无关）
 *
 * @author CPS System
 */
@Data
@Builder
public class CpsPromotionLinkResult {

    /**
     * 推广短链接（优先返回）
     */
    private String shortUrl;

    /**
     * 推广长链接
     */
    private String longUrl;

    /**
     * 淘口令（淘宝专用）
     */
    private String tpwd;

    /**
     * 移动端链接（拼多多专用）
     */
    private String mobileUrl;

    /**
     * 券后价（元）
     */
    private BigDecimal actualPrice;

    /**
     * 佣金比例（百分比）
     */
    private BigDecimal commissionRate;

    /**
     * 预估佣金（元）
     */
    private BigDecimal commissionAmount;

    /**
     * 优惠券信息描述
     */
    private String couponInfo;

    /**
     * 供应商特有扩展字段，保留标准对象未覆盖的信息。
     */
    private Map<String, Object> extraFields;

    /**
     * 供应商原始响应片段，便于排查真实接口差异。
     */
    private String rawPayload;

}
