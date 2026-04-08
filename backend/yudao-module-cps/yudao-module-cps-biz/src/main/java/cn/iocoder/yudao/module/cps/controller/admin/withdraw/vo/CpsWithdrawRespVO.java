package cn.iocoder.yudao.module.cps.controller.admin.withdraw.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - CPS提现申请 Response VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - CPS提现申请 Response VO")
@Data
public class CpsWithdrawRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "提现单号")
    private String withdrawNo;

    @Schema(description = "提现类型（alipay:支付宝 wechat:微信 bank:银行卡）")
    private String withdrawType;

    @Schema(description = "提现账户")
    private String withdrawAccount;

    @Schema(description = "账户名称")
    private String withdrawAccountName;

    @Schema(description = "提现金额")
    private BigDecimal amount;

    @Schema(description = "手续费")
    private BigDecimal feeAmount;

    @Schema(description = "实际到账金额")
    private BigDecimal actualAmount;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "审核人ID")
    private Long auditUserId;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核备注")
    private String reviewNote;

    @Schema(description = "转账单号")
    private String transactionNo;

    @Schema(description = "打款状态")
    private String transferStatus;

    @Schema(description = "转账时间")
    private LocalDateTime transferTime;

    @Schema(description = "转账错误信息")
    private String transferError;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
