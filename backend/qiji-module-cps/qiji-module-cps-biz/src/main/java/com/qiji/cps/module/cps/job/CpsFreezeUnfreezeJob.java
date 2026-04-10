package com.qiji.cps.module.cps.job;

import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.module.cps.service.freeze.CpsFreezeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CPS 冻结返利自动解冻定时任务
 *
 * <p>定期扫描已到达解冻时间的冻结记录，自动完成解冻操作，释放对应的返利余额。</p>
 *
 * <h3>Quartz 注册方式</h3>
 * 在管理后台【基础设施 - 定时任务】手动添加：
 * 处理器名字：cpsFreezeUnfreezeJob
 * 处理器参数示例：{"batchSize":500}（或留空使用默认值500）
 * CRON 表达式：0 0 2 * * ?（每日凌晨 2 点执行）
 *
 * <h3>参数说明（JSON 格式）</h3>
 * batchSize：每次处理的最大记录数，默认 500
 *
 * @author CPS System
 */
@Slf4j
@Component("cpsFreezeUnfreezeJob")
public class CpsFreezeUnfreezeJob implements JobHandler {

    private static final int DEFAULT_BATCH_SIZE = 500;

    @Resource
    private CpsFreezeService freezeService;

    @Override
    public String execute(String param) throws Exception {
        int batchSize = parseBatchSize(param);

        log.info("[CpsFreezeUnfreezeJob] 开始自动解冻，batchSize={}", batchSize);

        int count = freezeService.batchUnfreeze(batchSize);

        String result = String.format("自动解冻完成，本次解冻=%d条", count);
        log.info("[CpsFreezeUnfreezeJob] {}", result);
        return result;
    }

    /**
     * 解析 batchSize 参数，解析失败则返回默认值
     */
    private int parseBatchSize(String param) {
        if (param == null || !param.contains("batchSize")) {
            return DEFAULT_BATCH_SIZE;
        }
        try {
            String bsStr = param.replaceAll(".*\"batchSize\"\\s*:\\s*(\\d+).*", "$1");
            if (!bsStr.equals(param)) {
                return Integer.parseInt(bsStr);
            }
        } catch (Exception e) {
            log.warn("[CpsFreezeUnfreezeJob] 参数解析失败，使用默认值: param={}", param);
        }
        return DEFAULT_BATCH_SIZE;
    }

}
