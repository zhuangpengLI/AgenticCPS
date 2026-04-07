package cn.iocoder.yudao.module.cps.job;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.module.cps.client.CpsPlatformClient;
import cn.iocoder.yudao.module.cps.client.CpsPlatformClientFactory;
import cn.iocoder.yudao.module.cps.client.dto.CpsOrderDTO;
import cn.iocoder.yudao.module.cps.client.dto.CpsOrderQueryRequest;
import cn.iocoder.yudao.module.cps.dal.dataobject.order.CpsOrderSyncLogDO;
import cn.iocoder.yudao.module.cps.dal.dataobject.platform.CpsPlatformDO;
import cn.iocoder.yudao.module.cps.dal.mysql.order.CpsOrderSyncLogMapper;
import cn.iocoder.yudao.module.cps.service.order.CpsOrderService;
import cn.iocoder.yudao.module.cps.service.platform.CpsPlatformService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CPS 订单同步定时任务
 *
 * <p>定时拉取各已启用平台的订单，并进行幂等保存/状态更新。</p>
 *
 * <h3>Quartz 注册方式</h3>
 * 在管理后台【基础设施 - 定时任务】手动添加：
 * 处理器名字：cpsOrderSyncJob
 * 处理器参数示例：{"hours":2,"queryType":4}（或留空使用默认值）
 * CRON 表达式：0 0/30 * * * ?（每 30 分钟执行一次）
 *
 * <h3>参数说明（JSON 格式）</h3>
 * hours：向前追溯的小时数，默认 2
 * queryType：查询时间维度（1=下单时间，2=付款时间，3=结算时间，4=更新时间），默认 4
 * platformCode：指定平台编码，留空则同步所有已启用平台
 *
 * @author CPS System
 */
@Slf4j
@Component("cpsOrderSyncJob")
public class CpsOrderSyncJob implements JobHandler {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private CpsPlatformService platformService;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @Resource
    private CpsOrderService orderService;

    @Resource
    private CpsOrderSyncLogMapper syncLogMapper;

    @Override
    public String execute(String param) throws Exception {
        // 解析参数
        int hours = 2;
        int queryType = 4;
        String targetPlatformCode = null;

        if (StrUtil.isNotBlank(param)) {
            try {
                if (param.contains("hours")) {
                    String hoursStr = param.replaceAll(".*\"hours\"\\s*:\\s*(\\d+).*", "$1");
                    if (!hoursStr.equals(param)) {
                        hours = Integer.parseInt(hoursStr);
                    }
                }
                if (param.contains("queryType")) {
                    String qtStr = param.replaceAll(".*\"queryType\"\\s*:\\s*(\\d+).*", "$1");
                    if (!qtStr.equals(param)) {
                        queryType = Integer.parseInt(qtStr);
                    }
                }
                if (param.contains("platformCode")) {
                    String pcStr = param.replaceAll(".*\"platformCode\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                    if (!pcStr.equals(param)) {
                        targetPlatformCode = pcStr;
                    }
                }
            } catch (Exception e) {
                log.warn("[CpsOrderSyncJob] 参数解析失败，使用默认值: param={}", param);
            }
        }

        // 确定同步平台范围
        List<CpsPlatformDO> platforms;
        if (StrUtil.isNotBlank(targetPlatformCode)) {
            CpsPlatformDO platform = platformService.getPlatformByCode(targetPlatformCode);
            platforms = (platform != null) ? List.of(platform) : List.of();
        } else {
            platforms = platformService.getEnabledPlatformList();
        }

        if (platforms.isEmpty()) {
            log.info("[CpsOrderSyncJob] 没有已启用的平台，跳过本次同步");
            return "没有已启用的平台";
        }

        // 时间窗口
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hours);
        String startTimeStr = startTime.format(DTF);
        String endTimeStr = endTime.format(DTF);

        log.info("[CpsOrderSyncJob] 开始同步，时间窗口: {} ~ {}，queryType={}，平台数={}",
                startTimeStr, endTimeStr, queryType, platforms.size());

        // 统计汇总
        int totalNew = 0, totalUpdate = 0, totalSkip = 0, totalFailed = 0;
        List<String> resultLines = new ArrayList<>();

        for (CpsPlatformDO platform : platforms) {
            String platformCode = platform.getPlatformCode();
            CpsOrderSyncLogDO syncLog = CpsOrderSyncLogDO.builder()
                    .platformCode(platformCode)
                    .syncType(1)
                    .queryType(queryType)
                    .queryStartTime(startTime)
                    .queryEndTime(endTime)
                    .syncStartTime(LocalDateTime.now())
                    .totalCount(0)
                    .newCount(0)
                    .updateCount(0)
                    .skipCount(0)
                    .build();

            long t0 = System.currentTimeMillis();
            try {
                CpsPlatformClient client = platformClientFactory.getRequiredClient(platformCode);
                List<CpsOrderDTO> orders = pullAllOrders(client, queryType, startTimeStr, endTimeStr);

                int total = orders.size();
                int[] stats = orderService.batchSaveOrUpdateOrders(orders);
                int newCount = stats[0], updateCount = stats[1], skipCount = stats[2];

                syncLog.setTotalCount(total);
                syncLog.setNewCount(newCount);
                syncLog.setUpdateCount(updateCount);
                syncLog.setSkipCount(skipCount);
                syncLog.setSyncStatus(1);

                totalNew += newCount;
                totalUpdate += updateCount;
                totalSkip += skipCount;
                String line = String.format("[%s] 共%d条，新增%d，更新%d，跳过%d",
                        platformCode, total, newCount, updateCount, skipCount);
                resultLines.add(line);
                log.info("[CpsOrderSyncJob] 平台 {} 同步完成: {}", platformCode, line);

            } catch (Exception e) {
                log.error("[CpsOrderSyncJob] 平台 {} 同步失败", platformCode, e);
                syncLog.setSyncStatus(2);
                syncLog.setErrorMsg(StrUtil.subWithLength(e.getMessage(), 0, 500));
                totalFailed++;
                resultLines.add(String.format("[%s] 同步失败: %s", platformCode, e.getMessage()));
            } finally {
                long cost = System.currentTimeMillis() - t0;
                syncLog.setSyncEndTime(LocalDateTime.now());
                syncLog.setCostMs(cost);
                syncLogMapper.insert(syncLog);
            }
        }

        String summary = String.format("同步完成: 成功%d平台，失败%d平台，新增%d，更新%d，跳过%d%n%s",
                platforms.size() - totalFailed, totalFailed, totalNew, totalUpdate, totalSkip,
                String.join("\n", resultLines));
        log.info("[CpsOrderSyncJob] {}", summary);
        return summary;
    }

    /**
     * 翻页拉取平台全部订单（支持游标翻页，最多拉取 20 页防止死循环）
     */
    private List<CpsOrderDTO> pullAllOrders(CpsPlatformClient client, int queryType,
                                             String startTime, String endTime) {
        List<CpsOrderDTO> allOrders = new ArrayList<>();
        String positionIndex = null;
        int maxPages = 20;

        for (int page = 1; page <= maxPages; page++) {
            CpsOrderQueryRequest req = new CpsOrderQueryRequest();
            req.setQueryType(queryType);
            req.setStartTime(startTime);
            req.setEndTime(endTime);
            req.setPageSize(50);
            req.setPageNo(page);
            if (positionIndex != null) {
                req.setPositionIndex(positionIndex);
            }

            List<CpsOrderDTO> pageOrders = client.queryOrders(req);
            if (pageOrders == null || pageOrders.isEmpty()) {
                break;
            }

            allOrders.addAll(pageOrders);

            // 获取下一页游标（大淘客淘宝接口使用游标翻页）
            String nextIndex = pageOrders.get(pageOrders.size() - 1).getNextPositionIndex();
            if (nextIndex == null || nextIndex.equals(positionIndex)) {
                break;
            }
            positionIndex = nextIndex;

            // 如果返回数量小于 pageSize，说明已经是最后一页
            if (pageOrders.size() < 50) {
                break;
            }
        }

        return allOrders;
    }

}
