package com.qiji.cps.module.cps.controller.openapi.recommend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiCpsSceneRecommendRespVO {

    private String sceneCode;

    private String rebateOwnerType;

    private List<RecommendationVO> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationVO {
        private String platform;
        private String goodsId;
        private String goodsSign;
        private String title;
        private String mainPic;
        private BigDecimal price;
        private BigDecimal commissionRate;
        private BigDecimal estimatedRebate;
        private String reason;
        private String promotionUrl;
    }
}
