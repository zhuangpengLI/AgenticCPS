package com.qiji.cps.module.cps.controller.openapi.recommend.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OpenApiCpsSceneRecommendReqVO {

    @NotBlank
    private String tenantId;

    @NotNull
    private Long userId;

    @NotBlank
    private String sceneCode;

    private String deviceType;

    private String problemDescription;

    @NotEmpty
    private List<String> keywords;

    private BigDecimal budgetMin;

    private BigDecimal budgetMax;

    private List<String> platforms;

    private String sortBy;

    private String rebateOwnerType;
}
