package com.qiji.cps.module.cps.client.haina.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class HainaDecisionRequest {

    private String need;

    private String scenario;

    private BigDecimal budgetMin;

    private BigDecimal budgetMax;

    private List<String> preferredPlatforms;

    private Integer maxResults;
}
