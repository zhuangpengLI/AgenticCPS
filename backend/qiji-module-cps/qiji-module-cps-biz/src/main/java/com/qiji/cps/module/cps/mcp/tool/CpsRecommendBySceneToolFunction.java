package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.qiji.cps.module.cps.controller.openapi.recommend.vo.OpenApiCpsSceneRecommendReqVO;
import com.qiji.cps.module.cps.controller.openapi.recommend.vo.OpenApiCpsSceneRecommendRespVO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.recommend.CpsSceneRecommendationService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@Component("cps_recommend_by_scene")
public class CpsRecommendBySceneToolFunction
        implements Function<CpsRecommendBySceneToolFunction.Request, OpenApiCpsSceneRecommendRespVO> {

    @Resource
    private CpsSceneRecommendationService sceneRecommendationService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("根据AIoT设备场景、问题和预算推荐可转链的CPS商品")
    public static class Request {
        @JsonProperty(required = true, value = "tenant_id")
        private String tenantId;
        @JsonProperty(required = true, value = "user_id")
        private Long userId;
        @JsonProperty(required = true, value = "scene_code")
        private String sceneCode;
        @JsonProperty(value = "device_type")
        private String deviceType;
        @JsonProperty(value = "problem_description")
        private String problemDescription;
        @JsonProperty(required = true, value = "keywords")
        private List<String> keywords;
        @JsonProperty(value = "budget_min")
        private BigDecimal budgetMin;
        @JsonProperty(value = "budget_max")
        private BigDecimal budgetMax;
        @JsonProperty(value = "platforms")
        private List<String> platforms;
        @JsonProperty(value = "sort_by")
        @JsonPropertyDescription("best_value/low_price/high_rebate")
        private String sortBy;
        @JsonProperty(value = "rebate_owner_type")
        private String rebateOwnerType;
    }

    @Override
    public OpenApiCpsSceneRecommendRespVO apply(Request request) {
        long startedAt = System.currentTimeMillis();
        try {
            OpenApiCpsSceneRecommendReqVO reqVO = new OpenApiCpsSceneRecommendReqVO();
            reqVO.setTenantId(request.getTenantId());
            reqVO.setUserId(request.getUserId());
            reqVO.setSceneCode(request.getSceneCode());
            reqVO.setDeviceType(request.getDeviceType());
            reqVO.setProblemDescription(request.getProblemDescription());
            reqVO.setKeywords(request.getKeywords());
            reqVO.setBudgetMin(request.getBudgetMin());
            reqVO.setBudgetMax(request.getBudgetMax());
            reqVO.setPlatforms(request.getPlatforms());
            reqVO.setSortBy(request.getSortBy());
            reqVO.setRebateOwnerType(request.getRebateOwnerType());
            OpenApiCpsSceneRecommendRespVO response = sceneRecommendationService.recommendByScene(reqVO);
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_recommend_by_scene", request, response, null, null, startedAt);
            return response;
        } catch (Exception e) {
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_recommend_by_scene", request, null, e, null, startedAt);
            throw e;
        }
    }
}
