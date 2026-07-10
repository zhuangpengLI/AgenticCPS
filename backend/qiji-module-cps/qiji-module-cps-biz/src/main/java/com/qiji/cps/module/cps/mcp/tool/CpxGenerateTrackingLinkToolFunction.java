package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qiji.cps.module.cps.controller.app.cpx.vo.AppCpxTrackingLinkCreateReqVO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTrackingLinkDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.BiFunction;

@Component("cpx_generate_tracking_link")
public class CpxGenerateTrackingLinkToolFunction
        implements BiFunction<CpxGenerateTrackingLinkToolFunction.Request, ToolContext, CpxGenerateTrackingLinkToolFunction.Response> {

    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";

    @Resource
    private CpxTaskService taskService;
    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("为 CPX 任务生成可信会员归因 tracking link")
    public static class Request {
        @JsonProperty(required = true, value = "task_id")
        private Long taskId;
        @JsonProperty(value = "adzone_id")
        private String adzoneId;
        @JsonProperty(value = "channel_code")
        private String channelCode;
    }

    @Data
    public static class Response {
        private String status;
        private String reason;
        private String trackingId;
        private String trackingUrl;
    }

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        Response response = new Response();
        try {
            AppCpxTrackingLinkCreateReqVO reqVO = new AppCpxTrackingLinkCreateReqVO();
            reqVO.setTaskId(request.getTaskId());
            reqVO.setAdzoneId(request.getAdzoneId());
            reqVO.setChannelCode(request.getChannelCode());
            CpxTrackingLinkDO link = taskService.generateTrackingLink(reqVO, resolveMemberId(toolContext));
            response.setStatus("SUCCESS");
            response.setReason("已生成 CPX tracking link");
            response.setTrackingId(link.getTrackingId());
            response.setTrackingUrl(link.getTrackingUrl());
            CpsMcpToolAuditSupport.record(accessLogMapper, "cpx_generate_tracking_link", request, response, null, toolContext, startedAt);
            return response;
        } catch (Exception e) {
            response.setStatus("FAILED");
            response.setReason("CPX tracking link 生成失败，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cpx_generate_tracking_link", request, response, e, toolContext, startedAt);
            return response;
        }
    }

    private Long resolveMemberId(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Map<String, Object> context = toolContext.getContext();
        Object userId = context.get(TOOL_CONTEXT_LOGIN_USER_ID);
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
