package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
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
        if (request.getPlatformCode() == null || request.getGoodsId() == null) {
            return new Response(null, null, null, null, null, null, null, null, "platform_code 和 goods_id 不能为空");
        }
        try {
            // 从 ToolContext 获取会员 ID（若 request 未传）
            Long memberId = request.getMemberId();
            if (memberId == null && toolContext != null) {
                Map<String, Object> ctx = toolContext.getContext();
                Object userId = ctx.get(TOOL_CONTEXT_LOGIN_USER_ID);
                if (userId instanceof Long) {
                    memberId = (Long) userId;
                } else if (userId instanceof Number) {
                    memberId = ((Number) userId).longValue();
                }
            }

            CpsPromotionLinkResult result = goodsService.generatePromotionLink(
                    request.getPlatformCode(),
                    request.getGoodsId(),
                    request.getGoodsSign(),
                    memberId,
                    request.getAdzoneId());

            if (result == null) {
                return new Response(null, null, null, null, null, null, null, null, "转链失败，请检查商品ID是否正确");
            }

            return new Response(
                    result.getShortUrl(),
                    result.getLongUrl(),
                    result.getTpwd(),
                    result.getMobileUrl(),
                    result.getActualPrice(),
                    result.getCommissionRate(),
                    result.getCommissionAmount(),
                    result.getCouponInfo(),
                    null);
        } catch (Exception e) {
            return new Response(null, null, null, null, null, null, null, null, "转链失败：" + e.getMessage());
        }
    }

}
