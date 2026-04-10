package com.qiji.cps.module.cps.service.statistics;

import com.qiji.cps.module.cps.controller.admin.statistics.vo.CpsDashboardRespVO;
import com.qiji.cps.module.cps.controller.admin.statistics.vo.CpsPlatformSummaryVO;
import com.qiji.cps.module.cps.dal.dataobject.statistics.CpsStatisticsDO;

import java.time.LocalDate;
import java.util.List;

/**
 * CPS 统计数据 Service 接口
 *
 * @author CPS System
 */
public interface CpsStatisticsService {

    /**
     * 汇总指定日期的统计数据（供 Job 调用）
     *
     * @param date 统计日期
     */
    void aggregateDailyStatistics(LocalDate date);

    /**
     * 运营看板：实时今日数据 + 历史昨日数据（用于环比）
     */
    CpsDashboardRespVO getDashboard();

    /**
     * 趋势数据：按日返回指定时间段的统计指标
     *
     * @param startDate    开始日期（含）
     * @param endDate      结束日期（含）
     * @param platformCode 平台编码，"total" 代表全平台
     */
    List<CpsStatisticsDO> getTrend(LocalDate startDate, LocalDate endDate, String platformCode);

    /**
     * 各平台占比汇总（饼图数据）
     *
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     */
    List<CpsPlatformSummaryVO> getPlatformSummary(LocalDate startDate, LocalDate endDate);

}
