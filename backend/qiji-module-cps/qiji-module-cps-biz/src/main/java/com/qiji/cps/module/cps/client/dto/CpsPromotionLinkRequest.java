package com.qiji.cps.module.cps.client.dto;

import lombok.Data;

/**
 * CPS 推广链接生成请求 DTO（平台无关）
 *
 * @author CPS System
 */
@Data
public class CpsPromotionLinkRequest {

    /**
     * 商品ID（淘宝goodsId / 京东skuId / 拼多多goodsSign / 抖音itemId）
     */
    private String goodsId;

    /**
     * 商品goodsSign（拼多多转链必填）
     */
    private String goodsSign;

    /**
     * 推广位ID（PID，联盟推广位标识）
     */
    private String adzoneId;

    /**
     * 外部用户标识（如用户ID，用于订单归因）
     */
    private String externalId;

    /**
     * 渠道ID（京东subUnionId / 淘宝channelId）
     */
    private String channelId;

    /**
     * 淘宝联盟渠道关系ID（大淘客转链时映射为 channelId）
     */
    private String relationId;

    /**
     * 淘宝联盟会员运营ID（大淘客转链时映射为 specialId）
     */
    private String specialId;

    /**
     * 淘宝订单场景（1-常规订单，2-渠道订单，3-会员运营订单）
     */
    private Integer orderScene;

    /**
     * 商品原始URL（仅在没有平台商品 ID 时作为转链素材兜底）
     */
    private String itemLink;

    /**
     * 优惠券领取链接（好单库京东商品转链的 coupon_url）
     */
    private String couponUrl;

    /**
     * 用户提交的完整原始内容（链接、淘口令及分享文案）。
     *
     * <p>万能转链供应商可直接消费原文，避免先降级为数字商品 ID 后触发平台限制。</p>
     */
    private String originalContent;

    /**
     * 是否向调用方传播供应商拒绝详情。仅用于需要可诊断错误的内部调用链，默认保持兼容的空结果语义。
     */
    private boolean propagateVendorError;

}
