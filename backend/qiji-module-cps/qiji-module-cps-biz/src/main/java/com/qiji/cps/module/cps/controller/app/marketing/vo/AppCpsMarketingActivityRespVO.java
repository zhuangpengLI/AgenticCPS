package com.qiji.cps.module.cps.controller.app.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 APP - CPS营销活动中心 Response VO")
@Data
public class AppCpsMarketingActivityRespVO {

    @Schema(description = "活动ID")
    private Long id;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "活动类型")
    private String activityType;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "主图")
    private String mainPic;

    @Schema(description = "短描述")
    private String shortDesc;

    @Schema(description = "返利说明")
    private String rebateDesc;

    @Schema(description = "结算类型")
    private String billingType;

    @Schema(description = "推广数量")
    private Integer promotionCount;

    @Schema(description = "标签")
    private String tagText;

    @Schema(description = "跳转类型")
    private String jumpType;

    @Schema(description = "跳转地址")
    private String jumpUrl;

    @Schema(description = "搜索关键词")
    private String searchKeyword;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
