package cn.iocoder.yudao.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

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

}
