package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxArticleDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component("cpx_search_articles")
public class CpxSearchArticlesToolFunction implements Function<CpxSearchArticlesToolFunction.Request, CpxSearchArticlesToolFunction.Response> {

    @Resource
    private CpxTaskService taskService;
    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("搜索 CPX 资讯、教程、平台对接指南和任务攻略")
    public static class Request {
        @JsonProperty(value = "keyword")
        private String keyword;
        @JsonProperty(value = "category")
        private String category;
        @JsonProperty(value = "promotion_method")
        private String promotionMethod;
        @JsonProperty(value = "limit")
        private Integer limit;
    }

    @Data
    public static class Response {
        private String status;
        private String reason;
        private List<CpxArticleDO> articles;
    }

    @Override
    public Response apply(Request request) {
        long startedAt = System.currentTimeMillis();
        Response response = new Response();
        try {
            response.setArticles(taskService.searchArticles(
                    request == null ? null : request.getKeyword(),
                    request == null ? null : request.getCategory(),
                    request == null ? null : request.getPromotionMethod(),
                    request == null ? null : request.getLimit()));
            response.setStatus("SUCCESS");
            response.setReason("已返回 CPX 资讯");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cpx_search_articles", request, response, null, startedAt);
            return response;
        } catch (Exception e) {
            response.setStatus("FAILED");
            response.setReason("CPX 资讯查询失败，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cpx_search_articles", request, response, e, startedAt);
            return response;
        }
    }
}
