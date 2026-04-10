package com.qiji.cps.module.cps.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * CPS 运营数据看板 Response VO
 *
 * @author CPS System
 */
@Schema(description = "CPS运营数据看板 Response VO")
@Data
public class CpsDashboardRespVO {

    @Schema(description = "今日订单数")
    private Integer todayOrderCount;

    @Schema(description = "今日佣金（元）")
    private BigDecimal todayCommission;

    @Schema(description = "今日返利（元）")
    private BigDecimal todayRebate;

    @Schema(description = "今日利润（元）")
    private BigDecimal todayProfit;

    @Schema(description = "今日活跃会员数")
    private Integer todayActiveMembers;

    @Schema(description = "昨日订单数")
    private Integer yesterdayOrderCount;

    @Schema(description = "昨日佣金（元）")
    private BigDecimal yesterdayCommission;

    @Schema(description = "昨日返利（元）")
    private BigDecimal yesterdayRebate;

    @Schema(description = "昨日利润（元）")
    private BigDecimal yesterdayProfit;

    @Schema(description = "累计待结算佣金（元）")
    private BigDecimal totalPendingCommission;

    @Schema(description = "累计已结算佣金（元）")
    private BigDecimal totalSettledCommission;

}
