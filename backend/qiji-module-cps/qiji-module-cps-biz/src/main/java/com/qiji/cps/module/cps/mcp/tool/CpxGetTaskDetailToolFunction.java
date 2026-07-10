package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTaskDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component("cpx_get_task_detail")
public class CpxGetTaskDetailToolFunction implements Function<CpxGetTaskDetailToolFunction.Request, CpxGetTaskDetailToolFunction.Response> {

    @Resource
    private CpxTaskService taskService;
    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("查询 CPX 任务详情")
    public static class Request {
        @JsonProperty(required = true, value = "task_id")
        private Long taskId;
    }

    @Data
    public static class Response {
        private String status;
        private String reason;
        private CpxTaskDO task;
    }

    @Override
    public Response apply(Request request) {
        long startedAt = System.currentTimeMillis();
        Response response = new Response();
        try {
            response.setTask(taskService.getTask(request.getTaskId()));
            response.setStatus(response.getTask() == null ? "FAILED" : "SUCCESS");
            response.setReason(response.getTask() == null ? "任务不存在" : "已返回任务详情");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cpx_get_task_detail", request, response, null, null, startedAt);
            return response;
        } catch (Exception e) {
            response.setStatus("FAILED");
            response.setReason("CPX 任务详情查询失败，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cpx_get_task_detail", request, response, e, null, startedAt);
            return response;
        }
    }
}
