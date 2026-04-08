package cn.iocoder.yudao.module.cps.controller.app.rebate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户 APP - 返利账户 Response VO
 *
 * @author CPS System
 */
@Schema(description = "用户 APP - 返利账户 Response VO")
@Data
public class AppCpsRebateAccountRespVO {

    @Schema(description = "账户ID")
    private Long id;

    @Schema(description = "累计返利总额")
    private BigDecimal totalRebate;

    @Schema(description = "可用余额")
    private BigDecimal availableBalance;

    @Schema(description = "冻结余额")
    private BigDecimal frozenBalance;

    @Schema(description = "已提现金额")
    private BigDecimal withdrawnAmount;

    @Schema(description = "账户状态（0冻结 1正常）")
    private Integer status;

}
