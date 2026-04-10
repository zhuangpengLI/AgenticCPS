package com.qiji.cps.module.cps.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * CPS 趋势数据 Response VO（折线图）
 *
 * @author CPS System
 */
@Schema(description = "CPS趋势数据 Response VO")
@Data
public class CpsTrendRespVO {

    @Schema(description = "日期列表（YYYY-MM-DD）")
    private List<String> dates;

    @Schema(description = "订单数序列")
    private List<Integer> orderCounts;

    @Schema(description = "佣金序列（元）")
    private List<BigDecimal> commissions;

    @Schema(description = "返利序列（元）")
    private List<BigDecimal> rebates;

    @Schema(description = "利润序列（元）")
    private List<BigDecimal> profits;

}
