package com.qiji.cps.module.cps.service.recommend;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.controller.openapi.recommend.vo.OpenApiCpsSceneRecommendReqVO;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsSceneRecommendationServiceImplTest {

    @InjectMocks
    private CpsSceneRecommendationServiceImpl service;

    @Mock
    private CpsGoodsService goodsService;

    @Test
    @DisplayName("recommendByScene - 按 AIoT 场景关键词搜索、过滤预算并生成推广链接")
    void recommendByScene_searchesBySceneAndGeneratesPromotionLinks() {
        OpenApiCpsSceneRecommendReqVO request = new OpenApiCpsSceneRecommendReqVO();
        request.setTenantId("company-a");
        request.setUserId(100L);
        request.setSceneCode("CAMERA_NIGHT_BLUR");
        request.setDeviceType("camera");
        request.setProblemDescription("夜间画面模糊，补光不足");
        request.setKeywords(List.of("补光灯", "镜头清洁"));
        request.setBudgetMin(new BigDecimal("50.00"));
        request.setBudgetMax(new BigDecimal("300.00"));
        request.setPlatforms(List.of("jd"));
        request.setSortBy("best_value");

        when(goodsService.searchGoods(eq("jd"), any())).thenReturn(CpsGoodsSearchResult.builder()
                .list(List.of(CpsGoodsItem.builder()
                        .platformCode("jd")
                        .goodsId("goods-1")
                        .title("摄像头红外补光灯")
                        .actualPrice(new BigDecimal("89.00"))
                        .commissionRate(new BigDecimal("8.00"))
                        .commissionAmount(new BigDecimal("7.12"))
                        .goodsSign("sign-1")
                        .build()))
                .total(1L)
                .build());
        when(goodsService.generatePromotionLink(eq("jd"), eq("goods-1"), eq("sign-1"), eq(100L), isNull()))
                .thenReturn(CpsPromotionLinkResult.builder()
                        .shortUrl("https://cps.example/short")
                        .commissionAmount(new BigDecimal("7.12"))
                        .build());

        var response = service.recommendByScene(request);

        assertEquals("ENTERPRISE", response.getRebateOwnerType());
        assertEquals(1, response.getRecommendations().size());
        var item = response.getRecommendations().get(0);
        assertEquals("jd", item.getPlatform());
        assertEquals("goods-1", item.getGoodsId());
        assertEquals("摄像头红外补光灯", item.getTitle());
        assertEquals("https://cps.example/short", item.getPromotionUrl());
        assertEquals(new BigDecimal("7.12"), item.getEstimatedRebate());
        ArgumentCaptor<com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest> captor =
                ArgumentCaptor.forClass(com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest.class);
        verify(goodsService).searchGoods(eq("jd"), captor.capture());
        assertEquals("补光灯 镜头清洁", captor.getValue().getKeyword());
        assertEquals(new BigDecimal("50.00"), captor.getValue().getPriceLowerLimit());
        assertEquals(new BigDecimal("300.00"), captor.getValue().getPriceUpperLimit());
    }
}
