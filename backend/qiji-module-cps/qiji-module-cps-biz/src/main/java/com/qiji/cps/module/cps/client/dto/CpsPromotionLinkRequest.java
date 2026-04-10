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
     * 商品原始URL（京东转链必填的materialId）
     */
    private String itemLink;

}
