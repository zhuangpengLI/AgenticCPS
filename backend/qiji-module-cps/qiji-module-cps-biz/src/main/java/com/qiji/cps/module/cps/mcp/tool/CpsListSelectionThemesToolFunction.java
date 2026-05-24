package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.selection.CpsSelectionThemeService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component("cps_list_selection_themes")
public class CpsListSelectionThemesToolFunction
        implements Function<CpsListSelectionThemesToolFunction.Request, CpsListSelectionThemesToolFunction.Response> {

    @Resource
    private CpsSelectionThemeService selectionThemeService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("查询已发布的 CPS 选品主题库，可按关键词或大促标识筛选")
    public static class Request {
        @JsonProperty(value = "keyword")
        @JsonPropertyDescription("主题名称、主题编码或标签关键词")
        private String keyword;

        @JsonProperty(value = "promotion_event")
        @JsonPropertyDescription("大促标识，例如 618、双11、年货节")
        private String promotionEvent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String status;
        private String reason;
        private String source;
        private List<Theme> themes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Theme {
        private Long id;
        private String themeCode;
        private String themeName;
        private String promotionEvent;
        private String description;
        private String tags;
        private String status;
    }

    @Override
    public Response apply(Request request) {
        long startedAt = System.currentTimeMillis();
        try {
            List<CpsSelectionThemeDO> themes = selectionThemeService.listPublishedThemes(
                    request == null ? null : request.getKeyword(),
                    request == null ? null : request.getPromotionEvent());
            Response response = new Response("SUCCESS", "已返回已发布主题库", "selection_theme",
                    themes.stream().map(this::toTheme).toList());
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_list_selection_themes", request, response, null, startedAt);
            return response;
        } catch (Exception e) {
            Response response = new Response("FAILED", "选品主题查询失败，请稍后重试", "selection_theme", List.of());
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_list_selection_themes", request, response, e, startedAt);
            return response;
        }
    }

    private Theme toTheme(CpsSelectionThemeDO theme) {
        return new Theme(theme.getId(), theme.getThemeCode(), theme.getThemeName(), theme.getPromotionEvent(),
                theme.getDescription(), theme.getTags(), theme.getStatus());
    }
}
