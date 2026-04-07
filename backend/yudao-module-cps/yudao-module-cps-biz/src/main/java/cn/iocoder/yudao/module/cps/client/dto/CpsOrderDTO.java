package cn.iocoder.yudao.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

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
     * 是否退款（0-否，1-是）
     */
    private Integer refundTag;

    /**
     * 翻页游标（下一次查询使用）
     */
    private String nextPositionIndex;

}
