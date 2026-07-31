package com.qiji.cps.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * CPS 订单 DTO（平台无关，同步订单时使用）
 *
 * @author CPS System
 */
@Data
@Builder
public class CpsOrderDTO {

    /**
     * 平台订单号（子订单号）
     */
    private String platformOrderId;

    /**
     * 父订单号
     */
    private String parentOrderId;

    /**
     * 平台编码
     */
    private String platformCode;

    /**
     * 订单来源供应商编码，用于校验供应商专属归因令牌。
     */
    private String vendorCode;

    /**
     * 商品ID
     */
    private String itemId;

    /**
     * 商品标题
     */
    private String itemTitle;

    /**
     * 商品主图
     */
    private String itemPic;

    /**
     * 商品原价（元）
     */
    private BigDecimal itemPrice;

    /**
     * 实付价（元）
     */
    private BigDecimal finalPrice;

    /**
     * 优惠券金额（元）
     */
    private BigDecimal couponAmount;

    /**
     * 佣金比例（百分比）
     */
    private BigDecimal commissionRate;

    /**
     * 预估佣金金额（元）
     */
    private BigDecimal commissionAmount;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 平台订单状态（平台原始值）
     */
    private Integer platformStatus;

    /**
     * 下单时间（yyyy-MM-dd HH:mm:ss）
     */
    private String orderTime;

    /**
     * 付款时间
     */
    private String payTime;

    /**
     * 收货时间
     */
    private String receiveTime;

    /**
     * 结算时间
     */
    private String settleTime;

    /**
     * 推广位ID
     */
    private String adzoneId;

    /**
     * 外部用户标识（外部ID，用于归因）
     */
    private String externalId;

    /**
     * 淘宝会员运营ID
     */
    private String specialId;

    /**
     * 淘宝渠道关系ID
     */
    private String relationId;

    /**
     * 淘宝订单场景（1-常规订单，2-渠道订单，3-会员运营订单）
     */
    private Integer orderScene;

    /**
     * 是否退款（0-否，1-是）
     */
    private Integer refundTag;

    /**
     * 翻页游标（下一次查询使用）
     */
    private String nextPositionIndex;

    /**
     * 同步批次号，用于状态事件追溯。
     */
    private String syncBatchNo;

    /**
     * 供应商特有扩展字段，保留标准对象未覆盖的信息。
     */
    private Map<String, Object> extraFields;

    /**
     * 供应商原始响应片段，便于排查真实接口差异。
     */
    private String rawPayload;

}
