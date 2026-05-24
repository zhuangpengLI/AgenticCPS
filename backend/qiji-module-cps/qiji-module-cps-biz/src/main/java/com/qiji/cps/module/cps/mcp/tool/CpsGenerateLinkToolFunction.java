package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP Tool：生成推广/返利链接（转链）
 *
 * <p>AI Agent 调用此 Tool 为指定商品生成带有返利追踪的推广链接，
 * 通过 ToolContext 获取当前登录会员 ID 完成订单归因。</p>
 *
 * @author CPS System
 */
@Component("cps_generate_link")
public class CpsGenerateLinkToolFunction
        implements BiFunction<CpsGenerateLinkToolFunction.Request, ToolContext, CpsGenerateLinkToolFunction.Response> {

    /** ToolContext key：当前登录用户 ID */
    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";

    @Resource
    private CpsGoodsService goodsService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("为指定商品生成带返利追踪的推广链接（转链），支持淘宝口令、短链、移动链接等格式")
    public static class Request {

        @JsonProperty(required = true, value = "platform_code")
        @JsonPropertyDescription("平台编码：taobao=淘宝、jd=京东、pdd=拼多多、douyin=抖音")
        private String platformCode;

        @JsonProperty(required = true, value = "goods_id")
        @JsonPropertyDescription("平台商品ID，从搜索/比价结果的 goodsId 字段获取")
        private String goodsId;

        @JsonProperty(value = "goods_sign")
        @JsonPropertyDescription("商品goodsSign（拼多多必填，其他平台可不填），从搜索结果的 goodsSign 字段获取")
        private String goodsSign;

        @JsonProperty(value = "member_id")
        @JsonPropertyDescription("会员ID，用于订单归因。不填时从当前登录用户自动获取")
        private Long memberId;

        @JsonProperty(value = "adzone_id")
        @JsonPropertyDescription("推广位ID，不填则使用平台默认推广位")
        private String adzoneId;

        @JsonProperty(value = "vendor_code")
        @JsonPropertyDescription("API供应商编码，不填则使用平台默认供应商")
        private String vendorCode;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        /** 推广短链接（优先） */
        private String shortUrl;

        /** 推广长链接 */
        private String longUrl;

        /** 淘口令（淘宝专用） */
        private String tpwd;

        /** 移动端链接（拼多多专用） */
        private String mobileUrl;

        /** 券后价（元） */
        private BigDecimal actualPrice;

        /** 佣金比例（%） */
        private BigDecimal commissionRate;

        /** 预估佣金（元） */
        private BigDecimal commissionAmount;

        /** 券信息描述 */
        private String couponInfo;

        /** 错误信息 */
        private String error;

    }

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        if (request.getPlatformCode() == null || request.getGoodsId() == null) {
            Response response = new Response(null, null, null, null, null, null, null, null, "platform_code 和 goods_id 不能为空");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_generate_link", request, response,
                    new IllegalArgumentException("platform_code/goods_id required"), startedAt);
            return response;
        }
        try {
            // ToolContext 是可信身份来源；request.memberId 仅用于没有上下文的服务端调用兜底。
            Long memberId = null;
            if (toolContext != null) {
                Map<String, Object> ctx = toolContext.getContext();
                Object userId = ctx.get(TOOL_CONTEXT_LOGIN_USER_ID);
                if (userId instanceof Long) {
                    memberId = (Long) userId;
                } else if (userId instanceof Number) {
                    memberId = ((Number) userId).longValue();
                }
            }
            if (memberId == null) {
                memberId = request.getMemberId();
            }

            CpsPromotionLinkResult result = goodsService.generatePromotionLink(
                    request.getPlatformCode(),
                    request.getGoodsId(),
                    request.getGoodsSign(),
                    memberId,
                    request.getAdzoneId(),
                    request.getVendorCode());

            if (result == null) {
                Response response = new Response(null, null, null, null, null, null, null, null, "转链失败，请检查商品ID是否正确");
                CpsMcpToolAuditSupport.record(accessLogMapper, "cps_generate_link", request, response,
                        new IllegalStateException("empty promotion link result"), startedAt);
                return response;
            }

            Response response = new Response(
                    result.getShortUrl(),
                    result.getLongUrl(),
                    result.getTpwd(),
                    result.getMobileUrl(),
                    result.getActualPrice(),
                    result.getCommissionRate(),
                    result.getCommissionAmount(),
                    result.getCouponInfo(),
                    null);
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_generate_link", request, response, null, startedAt);
            return response;
        } catch (Exception e) {
            Response response = new Response(null, null, null, null, null, null, null, null, "转链失败，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_generate_link", request, response, e, startedAt);
            return response;
        }
    }

}
