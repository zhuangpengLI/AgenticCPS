package com.qiji.cps.module.cps.job;

import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.module.cps.service.statistics.CpsStatisticsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * CPS 统计数据汇总定时任务
 *
 * <p>每日凌晨聚合前一天订单数据，写入 {@code cps_statistics} 统计表。</p>
 *
 * <h3>Quartz 注册方式</h3>
 * 在管理后台【基础设施 - 定时任务】手动添加：
 * 处理器名字：cpsStatisticsAggregateJob
 * CRON 表达式：0 0 1 * * ?（每日凌晨 1 点执行）
 * 处理器参数示例：{"date":"2026-04-06"}（留空则默认汇总昨日数据）
 *
 * <h3>参数说明（JSON 格式）</h3>
 * date：指定汇总日期（YYYY-MM-DD），留空则默认昨日
 *
 * @author CPS System
 */
@Slf4j
@Component("cpsStatisticsAggregateJob")
public class CpsStatisticsAggregateJob implements JobHandler {

    @Resource
    private CpsStatisticsService statisticsService;

    @Override
    public String execute(String param) throws Exception {
        LocalDate date = parseDate(param);
        log.info("[CpsStatisticsAggregateJob] 开始汇总 {} 统计数据", date);
        statisticsService.aggregateDailyStatistics(date);
        String result = "汇总完成，日期=" + date;
        log.info("[CpsStatisticsAggregateJob] {}", result);
        return result;
    }

    /**
     * 解析参数中的日期，格式：{"date":"2026-04-06"}
     * 解析失败或参数为空时返回昨日日期
     */
    private LocalDate parseDate(String param) {
        if (param != null && param.contains("date")) {
            try {
                // 简单提取 "date":"YYYY-MM-DD" 值，避免引入额外 JSON 依赖
                String dateStr = param.replaceAll(".*\"date\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                if (!dateStr.equals(param)) {
                    return LocalDate.parse(dateStr.trim());
                }
            } catch (Exception e) {
                log.warn("[CpsStatisticsAggregateJob] 日期参数解析失败，使用昨日默认值: param={}", param);
            }
        }
        return LocalDate.now().minusDays(1);
    }

}
