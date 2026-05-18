package com.qiji.cps.module.cps.dal.dataobject.exchange;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("cps_rebate_token_exchange_order")
@KeySequence("cps_rebate_token_exchange_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRebateTokenExchangeOrderDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String exchangeOrderNo;

    private Long memberId;

    private String sourceSystem;

    private String sourceAsset;

    private BigDecimal sourceAmount;

    private String targetAsset;

    private Long targetTokens;

    private BigDecimal exchangeRate;

    private Long freezeRecordId;

    private String aitokenExchangeOrderId;

    private String status;

    private String failureReason;

    private String idempotencyKey;

    private LocalDateTime completedAt;
}
