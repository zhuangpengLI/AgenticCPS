package cn.iocoder.yudao.module.cps.service.statistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.cps.controller.admin.statistics.vo.CpsDashboardRespVO;
import cn.iocoder.yudao.module.cps.controller.admin.statistics.vo.CpsPlatformSummaryVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.statistics.CpsStatisticsDO;
import cn.iocoder.yudao.module.cps.dal.mysql.order.CpsOrderMapper;
import cn.iocoder.yudao.module.cps.dal.mysql.statistics.CpsStatisticsMapper;
import cn.iocoder.yudao.module.cps.enums.CpsPlatformCodeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CPS 统计数据 Service 实现
 *
 * @author CPS System
 */
@Slf4j
@Service
@Validated
public class CpsStatisticsServiceImpl implements CpsStatisticsService {

    @Resource
    private CpsStatisticsMapper statisticsMapper;
    @Resource
    private CpsOrderMapper orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void aggregateDailyStatistics(LocalDate date) {
        Long tenantId = TenantContextHolder.getTenantId();

        // 1. 查各平台明细数据
        List<Map<String, Object>> platformStats = orderMapper.selectDailyStatsByDate(date, tenantId);

        // 2. 各平台分别 upsert，并累加全平台汇总
        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalSettledCommission = BigDecimal.ZERO;
        BigDecimal totalPendingCommission = BigDecimal.ZERO;
        BigDecimal totalRebate = BigDecimal.ZERO;
        int totalOrderCount = 0;
        int totalNewOrderCount = 0;
        int totalActiveMembers = 0;

        for (Map<String, Object> row : platformStats) {
            String platformCode = (String) row.get("platform_code");
            CpsStatisticsDO stat = buildStatDO(row, date, platformCode);
            upsertStatistics(stat);

            // 累加汇总
            totalOrderAmount       = totalOrderAmount.add(nvl(stat.getOrderAmount()));
            totalCommission        = totalCommission.add(nvl(stat.getCommissionAmount()));
            totalSettledCommission = totalSettledCommission.add(nvl(stat.getSettledCommissionAmount()));
            totalPendingCommission = totalPendingCommission.add(nvl(stat.getPendingCommissionAmount()));
            totalRebate            = totalRebate.add(nvl(stat.getRebateAmount()));
            totalOrderCount       += safe(stat.getOrderCount());
            totalNewOrderCount    += safe(stat.getNewOrderCount());
            totalActiveMembers    += safe(stat.getActiveMemberCount());
        }

        // 3. 写全平台汇总行（platform_code='total'）
        BigDecimal totalProfit = totalCommission.subtract(totalRebate);
        CpsStatisticsDO totalStat = CpsStatisticsDO.builder()
                .statDate(date)
                .platformCode("total")
                .orderCount(totalOrderCount)
                .newOrderCount(totalNewOrderCount)
                .orderAmount(totalOrderAmount)
                .commissionAmount(totalCommission)
                .settledCommissionAmount(totalSettledCommission)
                .pendingCommissionAmount(totalPendingCommission)
                .rebateAmount(totalRebate)
                .profitAmount(totalProfit)
                .activeMemberCount(totalActiveMembers)
                .build();
        upsertStatistics(totalStat);

        log.info("[aggregateDailyStatistics] 日期={} 汇总完成，平台数={}", date, platformStats.size());
    }

    @Override
    public CpsDashboardRespVO getDashboard() {
        Long tenantId = TenantContextHolder.getTenantId();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 今日实时聚合
        Map<String, Object> todayData = orderMapper.selectRealtimeDashboard(today, tenantId);
        // 昨日实时聚合（昨日数据也用实时 SQL，保持一致性）
        Map<String, Object> yestData  = orderMapper.selectRealtimeDashboard(yesterday, tenantId);
        // 历史汇总：昨日 total 行（用于待/已结算佣金，定时任务写入）
        CpsStatisticsDO histTotal = statisticsMapper.selectByDateAndPlatform(yesterday, "total");

        return buildDashboardVO(todayData, yestData, histTotal);
    }

    @Override
    public List<CpsStatisticsDO> getTrend(LocalDate startDate, LocalDate endDate, String platformCode) {
        return statisticsMapper.selectTrendList(startDate, endDate, platformCode);
    }

    @Override
    public List<CpsPlatformSummaryVO> getPlatformSummary(LocalDate startDate, LocalDate endDate) {
        List<CpsStatisticsDO> list = statisticsMapper.selectPlatformSummary(startDate, endDate);

        // 按平台聚合（同一平台可能有多天数据，需汇总）
        Map<String, List<CpsStatisticsDO>> grouped = list.stream()
                .collect(Collectors.groupingBy(CpsStatisticsDO::getPlatformCode));

        List<CpsPlatformSummaryVO> result = new ArrayList<>();
        grouped.forEach((code, rows) -> {
            CpsPlatformSummaryVO vo = new CpsPlatformSummaryVO();
            vo.setPlatformCode(code);
            CpsPlatformCodeEnum enumVal = CpsPlatformCodeEnum.getByCode(code);
            vo.setPlatformName(enumVal != null ? enumVal.getName() : code);
            vo.setOrderCount(rows.stream().mapToInt(r -> safe(r.getOrderCount())).sum());
            vo.setCommissionAmount(rows.stream().map(r -> nvl(r.getCommissionAmount())).reduce(BigDecimal.ZERO, BigDecimal::add));
            vo.setRebateAmount(rows.stream().map(r -> nvl(r.getRebateAmount())).reduce(BigDecimal.ZERO, BigDecimal::add));
            vo.setProfitAmount(rows.stream().map(r -> nvl(r.getProfitAmount())).reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(vo);
        });
        return result;
    }

    // ======================== 私有方法 ========================

    /**
     * 从 Map 构建 CpsStatisticsDO
     */
    private CpsStatisticsDO buildStatDO(Map<String, Object> row, LocalDate date, String platformCode) {
        return CpsStatisticsDO.builder()
                .statDate(date)
                .platformCode(platformCode)
                .orderCount(toInt(row.get("order_count")))
                .newOrderCount(toInt(row.get("new_order_count")))
                .orderAmount(toBigDecimal(row.get("order_amount")))
                .commissionAmount(toBigDecimal(row.get("commission_amount")))
                .settledCommissionAmount(toBigDecimal(row.get("settled_commission_amount")))
                .pendingCommissionAmount(toBigDecimal(row.get("pending_commission_amount")))
                .rebateAmount(toBigDecimal(row.get("rebate_amount")))
                .profitAmount(toBigDecimal(row.get("commission_amount")).subtract(toBigDecimal(row.get("rebate_amount"))))
                .activeMemberCount(toInt(row.get("active_member_count")))
                .build();
    }

    /**
     * upsert：存在则 updateById，不存在则 insert
     */
    private void upsertStatistics(CpsStatisticsDO stat) {
        CpsStatisticsDO existing = statisticsMapper.selectByDateAndPlatform(
                stat.getStatDate(), stat.getPlatformCode());
        if (existing != null) {
            stat.setId(existing.getId());
            statisticsMapper.updateById(stat);
        } else {
            statisticsMapper.insert(stat);
        }
    }

    /**
     * 构建看板 VO
     */
    private CpsDashboardRespVO buildDashboardVO(Map<String, Object> today,
                                                 Map<String, Object> yest,
                                                 CpsStatisticsDO hist) {
        CpsDashboardRespVO vo = new CpsDashboardRespVO();
        // 今日
        vo.setTodayOrderCount(toInt(today != null ? today.get("order_count") : null));
        vo.setTodayCommission(toBigDecimal(today != null ? today.get("commission_amount") : null));
        vo.setTodayRebate(toBigDecimal(today != null ? today.get("rebate_amount") : null));
        vo.setTodayProfit(toBigDecimal(today != null ? today.get("profit_amount") : null));
        vo.setTodayActiveMembers(toInt(today != null ? today.get("active_member_count") : null));
        // 昨日
        vo.setYesterdayOrderCount(toInt(yest != null ? yest.get("order_count") : null));
        vo.setYesterdayCommission(toBigDecimal(yest != null ? yest.get("commission_amount") : null));
        vo.setYesterdayRebate(toBigDecimal(yest != null ? yest.get("rebate_amount") : null));
        vo.setYesterdayProfit(toBigDecimal(yest != null ? yest.get("profit_amount") : null));
        // 累计待/已结算（读昨日统计表的汇总行）
        vo.setTotalPendingCommission(hist != null ? nvl(hist.getPendingCommissionAmount()) : BigDecimal.ZERO);
        vo.setTotalSettledCommission(hist != null ? nvl(hist.getSettledCommissionAmount()) : BigDecimal.ZERO);
        return vo;
    }

    // ======================== 工具方法 ========================

    private static BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private static int safe(Integer val) {
        return val != null ? val : 0;
    }

    private static int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }

    private static BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

}
