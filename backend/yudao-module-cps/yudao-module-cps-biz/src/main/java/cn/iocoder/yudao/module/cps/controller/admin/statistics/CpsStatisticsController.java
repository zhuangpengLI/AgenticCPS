package cn.iocoder.yudao.module.cps.controller.admin.statistics;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.cps.controller.admin.statistics.vo.CpsDashboardRespVO;
import cn.iocoder.yudao.module.cps.controller.admin.statistics.vo.CpsPlatformSummaryVO;
import cn.iocoder.yudao.module.cps.controller.admin.statistics.vo.CpsTrendRespVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.statistics.CpsStatisticsDO;
import cn.iocoder.yudao.module.cps.service.statistics.CpsStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - CPS 数据统计 Controller
 *
 * @author CPS System
 */
@Tag(name = "管理后台 - CPS数据统计")
@RestController
@RequestMapping("/admin-api/cps/statistics")
@Validated
public class CpsStatisticsController {

    @Resource
    private CpsStatisticsService statisticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "运营数据看板")
    @PreAuthorize("@ss.hasPermission('cps:statistics:query')")
    public CommonResult<CpsDashboardRespVO> getDashboard() {
        return success(statisticsService.getDashboard());
    }

    @GetMapping("/trend")
    @Operation(summary = "趋势图表数据")
    @PreAuthorize("@ss.hasPermission('cps:statistics:query')")
    public CommonResult<CpsTrendRespVO> getTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "开始日期") LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "结束日期") LocalDate endDate,
            @RequestParam(defaultValue = "total")
            @Parameter(description = "平台编码，默认 total（全平台）") String platformCode) {
        List<CpsStatisticsDO> list = statisticsService.getTrend(startDate, endDate, platformCode);
        return success(buildTrendVO(list));
    }

    @GetMapping("/platform-summary")
    @Operation(summary = "平台占比统计")
    @PreAuthorize("@ss.hasPermission('cps:statistics:query')")
    public CommonResult<List<CpsPlatformSummaryVO>> getPlatformSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "开始日期") LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "结束日期") LocalDate endDate) {
        return success(statisticsService.getPlatformSummary(startDate, endDate));
    }

    // ======================== 私有工具 ========================

    private CpsTrendRespVO buildTrendVO(List<CpsStatisticsDO> list) {
        CpsTrendRespVO vo = new CpsTrendRespVO();
        List<String> dates = new ArrayList<>(list.size());
        List<Integer> orderCounts = new ArrayList<>(list.size());
        List<BigDecimal> commissions = new ArrayList<>(list.size());
        List<BigDecimal> rebates = new ArrayList<>(list.size());
        List<BigDecimal> profits = new ArrayList<>(list.size());

        for (CpsStatisticsDO stat : list) {
            dates.add(stat.getStatDate().toString());
            orderCounts.add(stat.getOrderCount() != null ? stat.getOrderCount() : 0);
            commissions.add(stat.getCommissionAmount() != null ? stat.getCommissionAmount() : BigDecimal.ZERO);
            rebates.add(stat.getRebateAmount() != null ? stat.getRebateAmount() : BigDecimal.ZERO);
            profits.add(stat.getProfitAmount() != null ? stat.getProfitAmount() : BigDecimal.ZERO);
        }

        vo.setDates(dates);
        vo.setOrderCounts(orderCounts);
        vo.setCommissions(commissions);
        vo.setRebates(rebates);
        vo.setProfits(profits);
        return vo;
    }

}
