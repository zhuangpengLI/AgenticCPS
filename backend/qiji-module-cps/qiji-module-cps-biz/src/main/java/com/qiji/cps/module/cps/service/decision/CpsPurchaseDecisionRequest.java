package com.qiji.cps.module.cps.service.decision;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CpsPurchaseDecisionRequest {

    private String need;

    private String scenario;

    private BigDecimal budgetMin;

    private BigDecimal budgetMax;

    private List<String> preferredPlatforms;

    private String decisionMode;

    private Boolean generateLink;
}
