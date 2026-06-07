package com.qiji.cps.module.cps.service.decision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsPurchaseDecisionResponse {

    private String summary;

    private DecisionItem bestChoice;

    @Builder.Default
    private List<DecisionItem> alternatives = Collections.emptyList();

    @Builder.Default
    private EvidenceVO evidence = EvidenceVO.empty();

    @Builder.Default
    private List<String> risks = Collections.emptyList();

    private Boolean hainaAvailable;

    private String hainaUnavailableReason;

    private String error;

    public static CpsPurchaseDecisionResponse error(String message) {
        return CpsPurchaseDecisionResponse.builder()
                .hainaAvailable(false)
                .alternatives(Collections.emptyList())
                .evidence(EvidenceVO.empty())
                .risks(Collections.emptyList())
                .error(message)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecisionItem {
        private String goodsId;
        private String goodsSign;
        private String platformCode;
        private String vendorCode;
        private String title;
        private String mainPic;
        private BigDecimal actualPrice;
        private BigDecimal estimatedRebate;
        private BigDecimal netPrice;
        private String shopName;
        private Integer decisionScore;
        @Builder.Default
        private List<String> reasons = Collections.emptyList();
        private String promotionUrl;
        private String linkError;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceVO {
        @Builder.Default
        private List<HainaGraphVO> hainaGraph = Collections.emptyList();
        @Builder.Default
        private List<HainaDiscountVO> hainaDiscounts = Collections.emptyList();
        @Builder.Default
        private List<HainaProductVO> hainaProducts = Collections.emptyList();
        @Builder.Default
        private List<CpsCandidateVO> cpsCandidates = Collections.emptyList();

        public static EvidenceVO empty() {
            return EvidenceVO.builder()
                    .hainaGraph(Collections.emptyList())
                    .hainaDiscounts(Collections.emptyList())
                    .hainaProducts(Collections.emptyList())
                    .cpsCandidates(Collections.emptyList())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HainaGraphVO {
        private String entityName;
        private String summary;
        @Builder.Default
        private List<String> pros = Collections.emptyList();
        @Builder.Default
        private List<String> cons = Collections.emptyList();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HainaDiscountVO {
        private String title;
        private String mallName;
        private BigDecimal price;
        private String content;
        private String url;
        private String pubdate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HainaProductVO {
        private String productName;
        private String mallName;
        private String shopName;
        private BigDecimal price;
        private String productUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CpsCandidateVO {
        private String goodsId;
        private String platformCode;
        private String title;
        private BigDecimal actualPrice;
        private BigDecimal estimatedRebate;
    }
}
