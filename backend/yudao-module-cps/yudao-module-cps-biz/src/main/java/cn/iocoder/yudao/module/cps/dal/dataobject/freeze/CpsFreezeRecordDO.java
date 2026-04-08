package cn.iocoder.yudao.module.cps.dal.dataobject.freeze;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.cps.enums.CpsFreezeStatusEnum;
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
     * 冻结金额
     */
    private BigDecimal freezeAmount;
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
