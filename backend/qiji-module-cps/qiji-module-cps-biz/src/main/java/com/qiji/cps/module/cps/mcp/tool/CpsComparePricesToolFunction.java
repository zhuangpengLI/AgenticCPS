package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCP Tool：跨平台商品比价
 *
 * <p>AI Agent 调用此 Tool 在所有已启用平台中搜索同一关键词，
 * 按券后价排序并给出最优购买建议（价格最低 / 返利最高 / 综合最优）。</p>
 *
 * @author CPS System
 */
@Component("cps_compare_prices")
public class CpsComparePricesToolFunction
        implements Function<CpsComparePricesToolFunction.Request, CpsComparePricesToolFunction.Response> {

    @Resource
    private CpsGoodsService goodsService;

    @Data
    @JsonClassDescription("跨平台比价：在淘宝/京东/拼多多/抖音搜索同一关键词，对比价格和返利，推荐最优购买方案")
    public static class Request {

        @JsonProperty(required = true, value = "keyword")
        @JsonPropertyDescription("要比价的商品关键词，例如：iPhone 16、耐克 Air Max 2024")
        private String keyword;

        @JsonProperty(value = "top_n")
        @JsonPropertyDescription("每个平台返回前N条结果参与比价，默认5，最大10")
        private Integer topN;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        /** 参与比价的商品总数 */
        private Integer total;

        /** 价格最低的商品 */
        private PriceItem cheapest;

        /** 返利最高的商品 */
        private PriceItem highestRebate;

        /** 综合最优（价格-返利实付最低）*/
        private PriceItem bestValue;

        /** 完整比价列表（按实付价升序） */
        private List<PriceItem> items;

        /** 错误信息 */
        private String error;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class PriceItem {

            /** 平台商品ID */
            private String goodsId;

            /** 平台编码 */
            private String platformCode;

            /** 商品标题 */
            private String title;

            /** 商品主图 */
            private String mainPic;

            /** 原价（元） */
            private BigDecimal originalPrice;

            /** 券后价（元） */
            private BigDecimal actualPrice;

            /** 预估佣金（元） */
            private BigDecimal commissionAmount;

            /** 扣除返利后实付（元）= 券后价 - 佣金 */
            private BigDecimal netPrice;

            /** 月销量 */
            private Long monthSales;

            /** 商品goodsSign（拼多多转链必填） */
            private String goodsSign;

        }

    }

    @Override
    public Response apply(Request request) {
        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            return new Response(0, null, null, null, null, "关键词不能为空");
        }
        try {
            int topN = request.getTopN() != null ? Math.min(request.getTopN(), 10) : 5;

            CpsGoodsSearchRequest searchRequest = new CpsGoodsSearchRequest();
            searchRequest.setKeyword(request.getKeyword());
            searchRequest.setPageSize(topN);

            List<CpsGoodsItem> allItems = goodsService.searchGoodsAllPlatforms(searchRequest);
            if (allItems == null || allItems.isEmpty()) {
                return new Response(0, null, null, null, new ArrayList<>(), null);
            }

            // 转换为 PriceItem，计算净价
            List<Response.PriceItem> priceItems = allItems.stream().map(item -> {
                Response.PriceItem pi = new Response.PriceItem();
                pi.setGoodsId(item.getGoodsId());
                pi.setPlatformCode(item.getPlatformCode());
                pi.setTitle(item.getTitle());
                pi.setMainPic(item.getMainPic());
                pi.setOriginalPrice(item.getOriginalPrice());
                pi.setActualPrice(item.getActualPrice());
                pi.setCommissionAmount(item.getCommissionAmount());
                pi.setMonthSales(item.getMonthSales());
                pi.setGoodsSign(item.getGoodsSign());
                // 净价 = 券后价 - 预估佣金（返利）
                BigDecimal actual = item.getActualPrice() != null ? item.getActualPrice() : BigDecimal.ZERO;
                BigDecimal commission = item.getCommissionAmount() != null ? item.getCommissionAmount() : BigDecimal.ZERO;
                pi.setNetPrice(actual.subtract(commission));
                return pi;
            }).sorted(Comparator.comparing(Response.PriceItem::getActualPrice,
                    Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            // 最便宜：券后价最低
            Response.PriceItem cheapest = priceItems.stream()
                    .filter(p -> p.getActualPrice() != null)
                    .min(Comparator.comparing(Response.PriceItem::getActualPrice))
                    .orElse(null);

            // 返利最高：佣金最高
            Response.PriceItem highestRebate = priceItems.stream()
                    .filter(p -> p.getCommissionAmount() != null)
                    .max(Comparator.comparing(Response.PriceItem::getCommissionAmount))
                    .orElse(null);

            // 综合最优：净价最低
            Response.PriceItem bestValue = priceItems.stream()
                    .filter(p -> p.getNetPrice() != null)
                    .min(Comparator.comparing(Response.PriceItem::getNetPrice))
                    .orElse(null);

            return new Response(priceItems.size(), cheapest, highestRebate, bestValue, priceItems, null);
        } catch (Exception e) {
            return new Response(0, null, null, null, new ArrayList<>(), "比价失败：" + e.getMessage());
        }
    }

}
