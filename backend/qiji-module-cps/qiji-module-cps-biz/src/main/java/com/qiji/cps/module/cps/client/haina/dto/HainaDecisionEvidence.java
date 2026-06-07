package com.qiji.cps.module.cps.client.haina.dto;

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
public class HainaDecisionEvidence {

    private Boolean available;

    private String unavailableReason;

    @Builder.Default
    private List<GraphEvidence> graphEvidence = Collections.emptyList();

    @Builder.Default
    private List<DiscountEvidence> discounts = Collections.emptyList();

    @Builder.Default
    private List<ProductEvidence> products = Collections.emptyList();

    public static HainaDecisionEvidence unavailable(String reason) {
        return HainaDecisionEvidence.builder()
                .available(false)
                .unavailableReason(reason)
                .graphEvidence(Collections.emptyList())
                .discounts(Collections.emptyList())
                .products(Collections.emptyList())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphEvidence {
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
    public static class DiscountEvidence {
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
    public static class ProductEvidence {
        private String productName;
        private String mallName;
        private String shopName;
        private BigDecimal price;
        private String productUrl;
    }
}
