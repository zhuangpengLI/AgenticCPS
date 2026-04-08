package cn.iocoder.yudao.module.cps.dal.dataobject.withdraw;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.cps.enums.CpsWithdrawStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CPS提现申请 DO
 *
 * @author CPS System
 */
@TableName("cps_withdraw")
@KeySequence("cps_withdraw_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsWithdrawDO extends TenantBaseDO {

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
     * 提现单号
     */
    private String withdrawNo;
    /**
     * 提现类型（alipay:支付宝 wechat:微信 bank:银行卡）
     */
    private String withdrawType;
    /**
     * 提现账户
     */
    private String withdrawAccount;
    /**
     * 账户名称
     */
    private String withdrawAccountName;
    /**
     * 提现金额
     */
    private BigDecimal amount;
    /**
     * 手续费
     */
    private BigDecimal feeAmount;
    /**
     * 实际到账金额
     */
    private BigDecimal actualAmount;
    /**
     * 状态
     *
     * 枚举 {@link CpsWithdrawStatusEnum}
     */
    private String status;
    /**
     * 审核人ID
     */
    private Long auditUserId;
    /**
     * 审核时间
     */
    private LocalDateTime auditTime;
    /**
     * 审核备注
     */
    private String reviewNote;
    /**
     * 转账单号
     */
    private String transactionNo;
    /**
     * 打款状态（WAITING:待打款 PROCESSING:打款中 SUCCESS:成功 FAILED:失败）
     */
    private String transferStatus;
    /**
     * 转账时间
     */
    private LocalDateTime transferTime;
    /**
     * 转账错误信息
     */
    private String transferError;

}
