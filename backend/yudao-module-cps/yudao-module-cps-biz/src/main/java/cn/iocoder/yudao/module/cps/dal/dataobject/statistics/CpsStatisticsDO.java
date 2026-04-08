package cn.iocoder.yudao.module.cps.dal.dataobject.statistics;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CPS统计数据 DO
 *
 * @author CPS System
 */
@TableName("cps_statistics")
@KeySequence("cps_statistics_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsStatisticsDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 统计日期
     */
    private LocalDate statDate;
    /**
     * 平台编码（total：全平台）
     */
    private String platformCode;
    /**
     * 订单数
     */
    private Integer orderCount;
    /**
     * 订单总金额
     */
    private BigDecimal orderAmount;
    /**
     * 佣金总额
     */
    private BigDecimal commissionAmount;
    /**
     * 返利总额
     */
    private BigDecimal rebateAmount;
    /**
     * 利润总额
     */
    private BigDecimal profitAmount;
    /**
     * 活跃会员数（当日有下单）
     */
    private Integer activeMemberCount;
    /**
     * 新增订单数（不含退款）
     */
    private Integer newOrderCount;
    /**
     * 已结算佣金
     */
    private BigDecimal settledCommissionAmount;
    /**
     * 待结算佣金
     */
    private BigDecimal pendingCommissionAmount;

}
