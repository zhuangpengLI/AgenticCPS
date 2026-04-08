package cn.iocoder.yudao.module.cps.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * CPS 平台汇总 Response VO（饼图）
 *
 * @author CPS System
 */
@Schema(description = "CPS平台汇总 Response VO")
@Data
public class CpsPlatformSummaryVO {

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "平台名称")
    private String platformName;

    @Schema(description = "订单数")
    private Integer orderCount;

    @Schema(description = "佣金（元）")
    private BigDecimal commissionAmount;

    @Schema(description = "返利（元）")
    private BigDecimal rebateAmount;

    @Schema(description = "利润（元）")
    private BigDecimal profitAmount;

}
