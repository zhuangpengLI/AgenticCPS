package cn.iocoder.yudao.module.cps.dal.dataobject.rebate;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.*;

import java.math.BigDecimal;

/**
 * CPS会员返利账户 DO
 *
 * @author CPS System
 */
@TableName("cps_rebate_account")
@KeySequence("cps_rebate_account_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRebateAccountDO extends TenantBaseDO {

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
     * 累计返利总额
     */
    private BigDecimal totalRebate;
    /**
     * 可用余额
     */
    private BigDecimal availableBalance;
    /**
     * 冻结余额
     */
    private BigDecimal frozenBalance;
    /**
     * 已提现金额
     */
    private BigDecimal withdrawnAmount;
    /**
     * 状态（0冻结 1正常）
     */
    private Integer status;
    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

}
