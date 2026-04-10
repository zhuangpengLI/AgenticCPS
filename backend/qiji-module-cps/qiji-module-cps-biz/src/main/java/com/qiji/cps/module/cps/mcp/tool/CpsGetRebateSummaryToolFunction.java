package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * MCP Tool：返利账户汇总
 *
 * <p>AI Agent 调用此 Tool 查询当前登录会员的返利账户余额、待结算金额、
 * 累计返利总额及最近返利记录，无需传入任何参数。</p>
 *
 * @author CPS System
 */
@Component("cps_get_rebate_summary")
public class CpsGetRebateSummaryToolFunction
        implements BiFunction<CpsGetRebateSummaryToolFunction.Request, ToolContext, CpsGetRebateSummaryToolFunction.Response> {

    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";

    @Resource
    private CpsRebateSettleService rebateSettleService;

    @Resource
    private CpsRebateRecordMapper rebateRecordMapper;

    @Data
    @JsonClassDescription("查询当前登录会员的返利账户汇总信息：可用余额、冻结余额、累计返利总额、已提现金额，以及最近5条返利记录")
    public static class Request {

        @JsonProperty(value = "recent_count")
        @JsonPropertyDescription("查询最近N条返利记录，默认5，最大20")
        private Integer recentCount;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        /** 可用余额（元） */
        private BigDecimal availableBalance;

        /** 冻结余额（元） */
        private BigDecimal frozenBalance;

        /** 累计返利总额（元） */
        private BigDecimal totalRebate;

        /** 已提现金额（元） */
        private BigDecimal withdrawnAmount;

        /** 账户状态（normal=正常, frozen=冻结） */
        private String accountStatus;

        /** 最近返利记录 */
        private List<RecentRebateVO> recentRecords;

        /** 错误信息 */
        private String error;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class RecentRebateVO {

            /** 商品标题 */
            private String itemTitle;

            /** 平台编码 */
            private String platformCode;

            /** 返利金额（元） */
            private BigDecimal rebateAmount;

            /** 返利类型（rebate=返利入账, reverse=退款扣回） */
            private String rebateType;

            /** 返利状态 */
            private String rebateStatus;

            /** 创建时间 */
            private java.time.LocalDateTime createTime;

        }

    }

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        Long memberId = extractMemberId(toolContext);
        if (memberId == null) {
            return new Response(null, null, null, null, null, Collections.emptyList(), "未登录或无法获取用户信息，请先登录");
        }
        try {
            // 获取或初始化返利账户
            CpsRebateAccountDO account = rebateSettleService.getOrInitAccount(memberId);

            String accountStatus = account.getStatus() != null && account.getStatus() == 1 ? "normal" : "frozen";

            // 查询最近返利记录
            int recentCount = request.getRecentCount() != null ? Math.min(request.getRecentCount(), 20) : 5;
            com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateRecordPageReqVO reqVO =
                    new com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateRecordPageReqVO();
            reqVO.setMemberId(memberId);
            reqVO.setPageNo(1);
            reqVO.setPageSize(recentCount);

            List<CpsRebateRecordDO> records = rebateRecordMapper.selectPage(reqVO).getList();
            List<Response.RecentRebateVO> recentRecords = records.stream().map(r -> {
                Response.RecentRebateVO vo = new Response.RecentRebateVO();
                vo.setItemTitle(r.getItemTitle());
                vo.setPlatformCode(r.getPlatformCode());
                vo.setRebateAmount(r.getRebateAmount());
                vo.setRebateType(r.getRebateType());
                vo.setRebateStatus(r.getRebateStatus());
                vo.setCreateTime(r.getCreateTime());
                return vo;
            }).collect(Collectors.toList());

            return new Response(
                    account.getAvailableBalance(),
                    account.getFrozenBalance(),
                    account.getTotalRebate(),
                    account.getWithdrawnAmount(),
                    accountStatus,
                    recentRecords,
                    null);
        } catch (Exception e) {
            return new Response(null, null, null, null, null, Collections.emptyList(), "查询返利汇总失败：" + e.getMessage());
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
