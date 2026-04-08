package cn.iocoder.yudao.module.cps.dal.dataobject.order;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.cps.enums.CpsFreezeStatusEnum;
import cn.iocoder.yudao.module.cps.enums.CpsOrderStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CPS订单 DO
 *
 * @author CPS System
 */
@TableName("cps_order")
@KeySequence("cps_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsOrderDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 平台编码
     */
    private String platformCode;
    /**
     * 平台订单号
     */
    private String platformOrderId;
    /**
     * 父订单号
     */
    private String parentOrderId;
    /**
     * 会员ID（归因后）
     */
    private Long memberId;
    /**
     * 会员昵称
     */
    private String memberNickname;
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
     * 商品原价
     */
    private BigDecimal itemPrice;
    /**
     * 券后价
     */
    private BigDecimal finalPrice;
    /**
     * 优惠券金额
     */
    private BigDecimal couponAmount;
    /**
     * 佣金比例（万分比）
     */
    private BigDecimal commissionRate;
    /**
     * 预估佣金金额
     */
    private BigDecimal commissionAmount;
    /**
     * 预估返利金额
     */
    private BigDecimal estimateRebate;
    /**
     * 实际返利金额
     */
    private BigDecimal realRebate;
    /**
     * 推广位ID
     */
    private String adzoneId;
    /**
     * 外部追踪参数
     */
    private String externalInfo;
    /**
     * 订单状态
     *
     * 枚举 {@link CpsOrderStatusEnum}
     */
    private String orderStatus;
    /**
     * 同步时间
     */
    private LocalDateTime syncTime;
    /**
     * 结算时间
     */
    private LocalDateTime settleTime;
    /**
     * 返利入账时间
     */
    private LocalDateTime rebateTime;
    /**
     * 退款时间
     */
    private LocalDateTime refundTime;
    /**
     * 确认收货时间
     */
    private LocalDateTime confirmReceiptTime;
    /**
     * 返利冻结状态
     *
     * 枚举 {@link CpsFreezeStatusEnum}
     */
    private String rebateFreezeStatus;
    /**
     * 计划解冻时间
     */
    private LocalDateTime planUnfreezeTime;
    /**
     * 实际解冻时间
     */
    private LocalDateTime actualUnfreezeTime;
    /**
     * 平台确认时间
     */
    private LocalDateTime platformConfirmTime;
    /**
     * 同步重试次数
     */
    private Integer retryCount;
    /**
     * 最后同步错误信息
     */
    private String lastSyncError;

}
