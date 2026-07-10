package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import com.qiji.cps.module.cps.service.selection.CpsSelectionConstants;
import com.qiji.cps.module.cps.service.selection.CpsSelectionThemeService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Component("cps_recommend_from_selection_theme")
public class CpsRecommendFromSelectionThemeToolFunction
        implements BiFunction<CpsRecommendFromSelectionThemeToolFunction.Request, ToolContext,
        CpsRecommendFromSelectionThemeToolFunction.Response> {

    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";

    @Resource
    private CpsSelectionThemeService selectionThemeService;

    @Resource
    private CpsGoodsService goodsService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("按已发布选品主题返回商品推荐；默认只读，仅显式 generate_link 且有可信登录上下文时转链")
    public static class Request {
        @JsonProperty(value = "theme_id")
        private Long themeId;

        @JsonProperty(value = "theme_code")
        private String themeCode;

        @JsonProperty(value = "limit")
        private Integer limit;

        @JsonProperty(value = "generate_link")
        @JsonPropertyDescription("是否生成推广链接。默认 false")
        private Boolean generateLink;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String status;
        private String reason;
        private String source;
        private Theme theme;
        private List<Item> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Theme {
        private Long id;
        private String themeCode;
        private String themeName;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String platformCode;
        private String vendorCode;
        private String goodsId;
        private String goodsSign;
        private String title;
        private String mainPic;
        private BigDecimal actualPrice;
        private BigDecimal couponPrice;
        private BigDecimal commissionRate;
        private BigDecimal commissionAmount;
        private Long monthSales;
        private BigDecimal recommendScore;
        private String recommendReason;
        private String promotionUrl;
    }

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        try {
            if (request == null || request.getThemeId() == null) {
                Response response = new Response("FAILED", "theme_id 不能为空", "selection_theme", null, List.of());
                CpsMcpToolAuditSupport.record(accessLogMapper, "cps_recommend_from_selection_theme", request,
                    response, new IllegalArgumentException("theme_id required"), toolContext, startedAt);
                return response;
            }
            CpsSelectionThemeDO theme = selectionThemeService.getTheme(request.getThemeId());
            if (theme == null || !CpsSelectionConstants.ThemeStatus.PUBLISHED.equals(theme.getStatus())) {
                Response response = new Response("OFFLINE", "主题未发布或已下线", "selection_theme",
                        theme == null ? null : toTheme(theme), List.of());
                CpsMcpToolAuditSupport.record(accessLogMapper, "cps_recommend_from_selection_theme", request,
                    response, null, toolContext, startedAt);
                return response;
            }
            int limit = request.getLimit() == null || request.getLimit() <= 0 ? 10 : Math.min(request.getLimit(), 50);
            Long memberId = resolveMemberId(toolContext);
            boolean generateLink = Boolean.TRUE.equals(request.getGenerateLink()) && memberId != null;
            List<Item> items = selectionThemeService.listEnabledItems(theme.getId()).stream()
                    .limit(limit)
                    .map(item -> toItem(item, generateLink ? memberId : null))
                    .toList();
            Response response = new Response("SUCCESS", "已返回主题商品推荐", "selection_theme", toTheme(theme), items);
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_recommend_from_selection_theme", request, response, null, toolContext, startedAt);
            return response;
        } catch (Exception e) {
            Response response = new Response("FAILED", "主题商品推荐失败，请稍后重试", "selection_theme", null, List.of());
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_recommend_from_selection_theme", request, response, e, toolContext, startedAt);
            return response;
        }
    }

    private Theme toTheme(CpsSelectionThemeDO theme) {
        return new Theme(theme.getId(), theme.getThemeCode(), theme.getThemeName(), theme.getStatus());
    }

    private Item toItem(CpsSelectionThemeItemDO item, Long memberId) {
        String promotionUrl = null;
        if (memberId != null) {
            CpsPromotionLinkResult linkResult = goodsService.generatePromotionLink(item.getPlatformCode(), item.getGoodsId(),
                    item.getGoodsSign(), memberId, null, item.getVendorCode());
            if (linkResult != null) {
                promotionUrl = firstText(linkResult.getShortUrl(), linkResult.getMobileUrl(), linkResult.getLongUrl());
            }
        }
        return new Item(item.getPlatformCode(), item.getVendorCode(), item.getGoodsId(), item.getGoodsSign(),
                item.getTitle(), item.getMainPic(), item.getActualPrice(), item.getCouponPrice(),
                item.getCommissionRate(), item.getCommissionAmount(), item.getMonthSales(), item.getRecommendScore(),
                item.getRecommendReason(), promotionUrl);
    }

    private Long resolveMemberId(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Map<String, Object> context = toolContext.getContext();
        if (context == null) {
            return null;
        }
        Object userId = context.get(TOOL_CONTEXT_LOGIN_USER_ID);
        if (userId instanceof Long longValue) {
            return longValue;
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
