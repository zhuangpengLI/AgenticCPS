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
import java.util.Collections;
import java.util.List;
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
        @JsonPropertyDescription("每页返回数量，默认10，最大20")
        private Integer pageSize;

        @JsonProperty(value = "price_min")
        @JsonPropertyDescription("最低价格（元），可选筛选条件")
        private BigDecimal priceMin;

        @JsonProperty(value = "price_max")
        @JsonPropertyDescription("最高价格（元），可选筛选条件")
        private BigDecimal priceMax;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        /** 搜索结果总数 */
        private Integer total;

        /** 商品列表 */
        private List<GoodsVO> goods;

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

        }

    }

    @Override
    public Response apply(Request request) {
        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            return new Response(0, Collections.emptyList(), "关键词不能为空");
        }
        try {
            CpsGoodsSearchRequest searchRequest = new CpsGoodsSearchRequest();
            searchRequest.setKeyword(request.getKeyword());
            searchRequest.setPageSize(request.getPageSize() != null
                    ? Math.min(request.getPageSize(), 20) : 10);

            List<CpsGoodsItem> items;
            if (request.getPlatformCode() != null && !request.getPlatformCode().isBlank()) {
                items = goodsService.searchGoods(request.getPlatformCode(), searchRequest).getList();
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

            List<Response.GoodsVO> voList = items == null ? Collections.emptyList() :
                    items.stream().map(item -> {
                        Response.GoodsVO vo = new Response.GoodsVO();
                        vo.setGoodsId(item.getGoodsId());
                        vo.setPlatformCode(item.getPlatformCode());
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
                        return vo;
                    }).collect(Collectors.toList());

            return new Response(voList.size(), voList, null);
        } catch (Exception e) {
            return new Response(0, Collections.emptyList(), "搜索失败：" + e.getMessage());
        }
    }

}
