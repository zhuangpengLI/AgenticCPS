package cn.iocoder.yudao.module.cps.mcp.tool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.order.CpsOrderDO;
import cn.iocoder.yudao.module.cps.dal.mysql.order.CpsOrderMapper;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * MCP Tool：查询会员返利订单
 *
 * <p>AI Agent 调用此 Tool 查询当前登录会员的订单列表及返利状态，
 * 通过 ToolContext 获取当前登录会员 ID，无需在请求中传入。</p>
 *
 * @author CPS System
 */
@Component("cps_query_orders")
public class CpsQueryOrdersToolFunction
        implements BiFunction<CpsQueryOrdersToolFunction.Request, ToolContext, CpsQueryOrdersToolFunction.Response> {

    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";

    @Resource
    private CpsOrderMapper orderMapper;

    @Data
    @JsonClassDescription("查询当前登录会员的CPS联盟返利订单列表，可按平台、状态筛选，分页返回")
    public static class Request {

        @JsonProperty(value = "platform_code")
        @JsonPropertyDescription("筛选平台编码：taobao=淘宝、jd=京东、pdd=拼多多、douyin=抖音。不填则查所有平台")
        private String platformCode;

        @JsonProperty(value = "order_status")
        @JsonPropertyDescription("筛选订单状态：ordered=已下单、paid=已付款、received=已收货、settled=已结算、rebate_received=已到账、refunded=已退款、invalid=已失效。不填则查所有状态")
        private String orderStatus;

        @JsonProperty(value = "page_no")
        @JsonPropertyDescription("页码，从1开始，默认1")
        private Integer pageNo;

        @JsonProperty(value = "page_size")
        @JsonPropertyDescription("每页数量，默认10，最大20")
        private Integer pageSize;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        /** 总记录数 */
        private Long total;

        /** 订单列表 */
        private List<OrderVO> orders;

        /** 错误信息 */
        private String error;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class OrderVO {

            /** 订单ID */
            private Long id;

            /** 平台编码 */
            private String platformCode;

            /** 平台订单号 */
            private String platformOrderId;

            /** 商品标题 */
            private String itemTitle;

            /** 商品主图 */
            private String itemPic;

            /** 券后价（元） */
            private BigDecimal finalPrice;

            /** 预估返利金额（元） */
            private BigDecimal estimateRebate;

            /** 实际返利金额（元） */
            private BigDecimal realRebate;

            /** 订单状态 */
            private String orderStatus;

            /** 返利入账时间 */
            private LocalDateTime rebateTime;

            /** 创建时间 */
            private LocalDateTime createTime;

        }

    }

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        // 从 ToolContext 获取会员 ID
        Long memberId = extractMemberId(toolContext);
        if (memberId == null) {
            return new Response(0L, Collections.emptyList(), "未登录或无法获取用户信息，请先登录");
        }
        try {
            CpsOrderPageReqVO reqVO = new CpsOrderPageReqVO();
            reqVO.setMemberId(memberId);
            reqVO.setPlatformCode(request.getPlatformCode());
            reqVO.setOrderStatus(request.getOrderStatus());
            reqVO.setPageNo(request.getPageNo() != null ? request.getPageNo() : 1);
            reqVO.setPageSize(request.getPageSize() != null ? Math.min(request.getPageSize(), 20) : 10);

            PageResult<CpsOrderDO> pageResult = orderMapper.selectPageByMemberId(reqVO, memberId);

            List<Response.OrderVO> voList = pageResult.getList().stream().map(order -> {
                Response.OrderVO vo = new Response.OrderVO();
                vo.setId(order.getId());
                vo.setPlatformCode(order.getPlatformCode());
                vo.setPlatformOrderId(order.getPlatformOrderId());
                vo.setItemTitle(order.getItemTitle());
                vo.setItemPic(order.getItemPic());
                vo.setFinalPrice(order.getFinalPrice());
                vo.setEstimateRebate(order.getEstimateRebate());
                vo.setRealRebate(order.getRealRebate());
                vo.setOrderStatus(order.getOrderStatus());
                vo.setRebateTime(order.getRebateTime());
                vo.setCreateTime(order.getCreateTime());
                return vo;
            }).collect(Collectors.toList());

            return new Response(pageResult.getTotal(), voList, null);
        } catch (Exception e) {
            return new Response(0L, Collections.emptyList(), "查询订单失败：" + e.getMessage());
        }
    }

    private Long extractMemberId(ToolContext toolContext) {
        if (toolContext == null) return null;
        Map<String, Object> ctx = toolContext.getContext();
        Object userId = ctx.get(TOOL_CONTEXT_LOGIN_USER_ID);
        if (userId instanceof Long) return (Long) userId;
        if (userId instanceof Number) return ((Number) userId).longValue();
        return null;
    }

}
