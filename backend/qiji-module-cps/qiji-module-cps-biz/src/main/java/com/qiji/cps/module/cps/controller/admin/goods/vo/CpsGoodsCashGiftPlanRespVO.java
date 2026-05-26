package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - CPS工具箱淘礼金计划 Response VO")
@Data
@Builder
public class CpsGoodsCashGiftPlanRespVO {

    @Schema(description = "计划状态：PLAN_ONLY/READY/RISK")
    private String planStatus;

    @Schema(description = "状态说明")
    private String message;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "计划名称")
    private String campaignName;

    @Schema(description = "总预算（元）")
    private BigDecimal budgetAmount;

    @Schema(description = "单个淘礼金金额（元）")
    private BigDecimal giftAmount;

    @Schema(description = "发放份数")
    private Integer totalQuantity;

    @Schema(description = "预算缺口（元）")
    private BigDecimal budgetGap;

    @Schema(description = "预算是否足够")
    private Boolean budgetEnough;

    @Schema(description = "推广文案")
    private String promotionContent;

    @Schema(description = "上线检查项")
    private List<String> checklist;

    @Schema(description = "风险提示")
    private List<String> warnings;

    @Schema(description = "可选模板")
    private List<Template> templates;

    @Data
    @Builder
    public static class Template {

        @Schema(description = "模板编码")
        private String code;

        @Schema(description = "模板名称")
        private String name;

        @Schema(description = "适用场景")
        private String scene;

        @Schema(description = "推荐配置")
        private String suggestion;

    }

}
