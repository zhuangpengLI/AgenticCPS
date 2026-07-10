package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTaskDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component("cpx_list_tasks")
public class CpxListTasksToolFunction implements Function<CpxListTasksToolFunction.Request, CpxListTasksToolFunction.Response> {

    @Resource
    private CpxTaskService taskService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("查询 CPX 任务大厅，默认 CPS 成交返利任务优先返回")
    public static class Request {
        @JsonProperty(value = "keyword")
        @JsonPropertyDescription("任务名称、标签或描述关键词")
        private String keyword;
        @JsonProperty(value = "promotion_method")
        @JsonPropertyDescription("推广方式：CPS/CPA/CPL/CPM/CPC/OCPA/OCPC/MIXED")
        private String promotionMethod;
        @JsonProperty(value = "limit")
        @JsonPropertyDescription("返回数量，默认 20，最大 100")
        private Integer limit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String status;
        private String reason;
        private List<Task> tasks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Task {
        private Long id;
        private String taskName;
        private String platformCode;
        private String promotionMethod;
        private String rewardDesc;
        private Integer priority;
    }

    @Override
    public Response apply(Request request) {
        long startedAt = System.currentTimeMillis();
        try {
            List<CpxTaskDO> tasks = taskService.listPublishedTasks(
                    request == null ? null : request.getKeyword(),
                    request == null ? null : request.getPromotionMethod(),
                    request == null ? null : request.getLimit());
            Response response = new Response("SUCCESS", "已返回 CPX 任务，CPS 成交返利优先",
                    tasks.stream().map(this::toTask).toList());
            CpsMcpToolAuditSupport.record(accessLogMapper, "cpx_list_tasks", request, response, null, null, startedAt);
            return response;
        } catch (Exception e) {
            Response response = new Response("FAILED", "CPX 任务查询失败，请稍后重试", List.of());
            CpsMcpToolAuditSupport.record(accessLogMapper, "cpx_list_tasks", request, response, e, null, startedAt);
            return response;
        }
    }

    Task toTask(CpxTaskDO task) {
        return new Task(task.getId(), task.getTaskName(), task.getPlatformCode(), task.getPromotionMethod(),
                task.getRewardDesc(), task.getPriority());
    }
}
