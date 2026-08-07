package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCP Tool：多平台商品搜索
 *
 * <p>AI Agent 调用此 Tool 在淘宝/京东/拼多多/抖音搜索商品，返回结构化商品列表。</p>
 *
 * @author CPS System
 */
@Component("cps_search_goods")
public class CpsSearchGoodsToolFunction
        implements Function<CpsSearchGoodsToolFunction.Request, CpsSearchGoodsToolFunction.Response> {

    @Resource
    private CpsGoodsService goodsService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("在联盟平台（淘宝/京东/拼多多/抖音）搜索商品，返回商品列表及价格信息")
    public static class Request {

        @JsonProperty(required = true, value = "keyword")
        @JsonPropertyDescription("商品搜索关键词，例如：iPhone 16 手机壳、耐克运动鞋")
        private String keyword;

        @JsonProperty(value = "platform_code")
        @JsonPropertyDescription("指定搜索平台编码：taobao=淘宝、jd=京东、pdd=拼多多、douyin=抖音。不传则搜索全平台并聚合结果")
        private String platformCode;

        @JsonProperty(value = "page_size")
        @JsonPropertyDescription("最终返回数量，默认10，最大20；全平台搜索会优先覆盖有结果的平台")
        private Integer pageSize;

        @JsonProperty(value = "price_min")
        @JsonPropertyDescription("最低价格（元），可选筛选条件")
        private BigDecimal priceMin;

        @JsonProperty(value = "price_max")
        @JsonPropertyDescription("最高价格（元），可选筛选条件")
        private BigDecimal priceMax;

        @JsonProperty(value = "vendor_code")
        @JsonPropertyDescription("API供应商编码；指定单平台搜索时可用，不填则使用平台默认供应商")
        private String vendorCode;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        public Response(Integer total, List<GoodsVO> goods, String error) {
            this(total, goods == null ? 0 : goods.size(), Collections.emptyMap(), goods, null, error);
        }

        /** 搜索结果总数 */
        private Integer total;

        /** 本次实际返回数量 */
        private Integer returned;

        /** 各平台候选数量 */
        private Map<String, Integer> platformCounts;

        /** 商品列表 */
        private List<GoodsVO> goods;

        /** 结果选择口径 */
        private String selectionNote;

        /** 错误信息（成功时为null） */
        private String error;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class GoodsVO {

            /** 平台商品ID */
            private String goodsId;

            /** 平台编码 */
            private String platformCode;

            /** 平台中文名称 */
            private String platformName;

            /** 商品标题 */
            private String title;

            /** 商品主图URL */
            private String mainPic;

            /** 原价（元） */
            private BigDecimal originalPrice;

            /** 券后实付价（元） */
            private BigDecimal actualPrice;

            /** 优惠券金额（元） */
            private BigDecimal couponPrice;

            /** 佣金比例（%） */
            private BigDecimal commissionRate;

            /** 预估佣金（元） */
            private BigDecimal commissionAmount;

            /** 月销量 */
            private Long monthSales;

            /** 店铺名称 */
            private String shopName;

            /** 商品goodsSign（拼多多转链必填） */
            private String goodsSign;

            /** 搜索结果所属 API 供应商，转链时应原样传入 vendor_code */
            private String vendorCode;

            /** 商品原始链接，供应商需要原始素材时可用于转链 */
            private String itemLink;

        }

    }

    @Override
    public Response apply(Request request) {
        long startedAt = System.currentTimeMillis();
        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            Response response = new Response(0, 0, Collections.emptyMap(), Collections.emptyList(), null,
                    "关键词不能为空");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_search_goods", request, response,
                    new IllegalArgumentException("keyword required"), null, startedAt);
            return response;
        }
        try {
            CpsGoodsSearchRequest searchRequest = new CpsGoodsSearchRequest();
            searchRequest.setKeyword(request.getKeyword());
            searchRequest.setPageSize(normalizePageSize(request.getPageSize()));

            List<CpsGoodsItem> items;
            if (request.getPlatformCode() != null && !request.getPlatformCode().isBlank()) {
                items = goodsService.searchGoods(request.getPlatformCode(), searchRequest, request.getVendorCode()).getList();
            } else {
                items = goodsService.searchGoodsAllPlatforms(searchRequest);
            }

            // 价格范围过滤
            if (items != null && (request.getPriceMin() != null || request.getPriceMax() != null)) {
                items = items.stream()
                        .filter(item -> {
                            if (item.getActualPrice() == null) return true;
                            if (request.getPriceMin() != null
                                    && item.getActualPrice().compareTo(request.getPriceMin()) < 0) return false;
                            if (request.getPriceMax() != null
                                    && item.getActualPrice().compareTo(request.getPriceMax()) > 0) return false;
                            return true;
                        })
                        .collect(Collectors.toList());
            }

            List<CpsGoodsItem> candidates = items == null ? new ArrayList<>() : new ArrayList<>(items);
            candidates.sort(actualPriceComparator());
            Map<String, Integer> platformCounts = countByPlatform(candidates);
            int limit = normalizePageSize(request.getPageSize());
            boolean allPlatforms = request.getPlatformCode() == null || request.getPlatformCode().isBlank();
            List<CpsGoodsItem> selectedItems = allPlatforms
                    ? selectPlatformBalanced(candidates, limit)
                    : candidates.stream().limit(limit).toList();
            String selectionNote = allPlatforms && platformCounts.size() > 1
                    ? "全平台均衡展示：已优先覆盖有结果的平台，再按券后价补足并升序排列"
                    : "按券后价从低到高排列";

            List<Response.GoodsVO> voList = selectedItems.stream().map(item -> {
                        Response.GoodsVO vo = new Response.GoodsVO();
                        vo.setGoodsId(item.getGoodsId());
                        vo.setPlatformCode(item.getPlatformCode());
                        vo.setPlatformName(platformName(item.getPlatformCode()));
                        vo.setTitle(item.getTitle());
                        vo.setMainPic(item.getMainPic());
                        vo.setOriginalPrice(item.getOriginalPrice());
                        vo.setActualPrice(item.getActualPrice());
                        vo.setCouponPrice(item.getCouponPrice());
                        vo.setCommissionRate(item.getCommissionRate());
                        vo.setCommissionAmount(item.getCommissionAmount());
                        vo.setMonthSales(item.getMonthSales());
                        vo.setShopName(item.getShopName());
                        vo.setGoodsSign(item.getGoodsSign());
                        vo.setVendorCode(item.getVendorCode());
                        vo.setItemLink(item.getItemLink());
                        return vo;
                    }).collect(Collectors.toList());

            Response response = new Response(candidates.size(), voList.size(), platformCounts, voList, selectionNote, null);
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_search_goods", request, response, null, null, startedAt);
            return response;
        } catch (Exception e) {
            Response response = new Response(0, 0, Collections.emptyMap(), Collections.emptyList(), null,
                    "搜索失败，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_search_goods", request, response, e, null, startedAt);
            return response;
        }
    }

    private Integer normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 10;
        }
        return Math.max(1, Math.min(pageSize, 20));
    }

    private Map<String, Integer> countByPlatform(List<CpsGoodsItem> items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        items.stream()
                .map(CpsGoodsItem::getPlatformCode)
                .map(this::normalizePlatformCode)
                .sorted()
                .forEach(platformCode -> counts.merge(platformCode, 1, Integer::sum));
        return counts;
    }

    private List<CpsGoodsItem> selectPlatformBalanced(List<CpsGoodsItem> candidates, int limit) {
        if (candidates.size() <= limit) {
            return candidates;
        }
        Map<String, CpsGoodsItem> cheapestByPlatform = new LinkedHashMap<>();
        for (CpsGoodsItem item : candidates) {
            cheapestByPlatform.putIfAbsent(normalizePlatformCode(item.getPlatformCode()), item);
        }

        List<CpsGoodsItem> selected = new ArrayList<>();
        cheapestByPlatform.values().stream()
                .sorted(actualPriceComparator())
                .limit(limit)
                .forEach(selected::add);
        for (CpsGoodsItem item : candidates) {
            if (selected.size() >= limit) {
                break;
            }
            if (!selected.contains(item)) {
                selected.add(item);
            }
        }
        selected.sort(actualPriceComparator());
        return selected;
    }

    private Comparator<CpsGoodsItem> actualPriceComparator() {
        return Comparator.comparing(CpsGoodsItem::getActualPrice,
                Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private String normalizePlatformCode(String platformCode) {
        return platformCode == null ? "unknown" : platformCode.trim().toLowerCase(Locale.ROOT);
    }

    private String platformName(String platformCode) {
        return switch (normalizePlatformCode(platformCode)) {
            case "taobao" -> "淘宝";
            case "jd", "jingdong" -> "京东";
            case "pdd", "pinduoduo" -> "拼多多";
            case "douyin" -> "抖音";
            default -> platformCode;
        };
    }

}
