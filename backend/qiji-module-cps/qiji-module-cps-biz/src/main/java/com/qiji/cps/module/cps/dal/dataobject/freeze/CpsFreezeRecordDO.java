package com.qiji.cps.module.cps.dal.dataobject.freeze;

import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import com.qiji.cps.module.cps.enums.CpsFreezeStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CPS冻结解冻记录 DO
 *
 * @author CPS System
 */
@TableName("cps_freeze_record")
@KeySequence("cps_freeze_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsFreezeRecordDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 会员ID
     */
    private Long memberId;
    /**
     * 订单ID
     */
    private Long orderId;
    /**
     * 平台订单号
     */
    private String platformOrderId;
    /**
     * 业务类型
     */
    private String businessType;
    /**
     * 业务单号
     */
    private String businessId;
    /**
     * 幂等键
     */
    private String idempotencyKey;
    /**
     * 冻结金额
     */
    private BigDecimal freezeAmount;
    /**
     * 冻结金额（分）。V2 资产操作优先读取该字段。
     */
    private Long amountCent;
    /**
     * 冻结配置快照。
     */
    private Long freezeConfigId;
    private Integer freezeDaysSnapshot;
    /**
     * 同时满足确认收货与平台结算后的资格时间。
     */
    private LocalDateTime eligibleTime;
    /**
     * 手工解冻审计信息。
     */
    private String manualUnfreezeReason;
    private String manualUnfreezeOperatorId;
    /**
     * 计划解冻时间
     */
    private LocalDateTime unfreezeTime;
    /**
     * 实际解冻时间
     */
    private LocalDateTime actualUnfreezeTime;
    /**
     * 状态
     *
     * 枚举 {@link CpsFreezeStatusEnum}
     */
    private String status;

}
