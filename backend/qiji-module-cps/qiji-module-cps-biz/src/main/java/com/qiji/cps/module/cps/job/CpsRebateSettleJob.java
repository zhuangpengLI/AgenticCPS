package com.qiji.cps.module.cps.job;

import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CPS 返利结算定时任务
 *
 * <p>定期扫描已收货/已结算且返利未入账的订单，自动完成返利计算与入账。</p>
 *
 * <h3>Quartz 注册方式</h3>
 * 在管理后台【基础设施 - 定时任务】手动添加：
 * 处理器名字：cpsRebateSettleJob
 * 处理器参数示例：{"batchSize":100}（或留空使用默认值200）
 * CRON 表达式：0 0 * * * ?（每小时执行一次）
 *
 * <h3>参数说明（JSON 格式）</h3>
 * batchSize：每次扫描的最大订单数，默认 200
 *
 * @author CPS System
 */
@Slf4j
@Component("cpsRebateSettleJob")
public class CpsRebateSettleJob implements JobHandler {

    private static final int DEFAULT_BATCH_SIZE = 200;

    @Resource
    private CpsRebateSettleService rebateSettleService;

    @Override
    public String execute(String param) throws Exception {
        int batchSize = DEFAULT_BATCH_SIZE;

        if (param != null && param.contains("batchSize")) {
            try {
                String bsStr = param.replaceAll(".*\"batchSize\"\\s*:\\s*(\\d+).*", "$1");
                if (!bsStr.equals(param)) {
                    batchSize = Integer.parseInt(bsStr);
                }
            } catch (Exception e) {
                log.warn("[CpsRebateSettleJob] 参数解析失败，使用默认值: param={}", param);
            }
        }

        log.info("[CpsRebateSettleJob] 开始批量返利结算，batchSize={}", batchSize);

        int[] stats = rebateSettleService.batchSettle(batchSize);
        int success = stats[0], skip = stats[1], fail = stats[2];

        String result = String.format("返利结算完成: 成功入账=%d，跳过=%d，失败=%d", success, skip, fail);
        log.info("[CpsRebateSettleJob] {}", result);
        return result;
    }

}
