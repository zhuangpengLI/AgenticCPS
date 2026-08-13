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

    @Schema(description = "是否支持官方活动列表同步")
    private Boolean supportsList;

    @Schema(description = "是否支持官方推广转链")
    private Boolean supportsPromotionLink;

    @Schema(description = "是否支持订单同步")
    private Boolean supportsOrders;

    @Schema(description = "是否支持小程序素材")
    private Boolean supportsMiniProgram;

    @Schema(description = "是否本地生活活动")
    private Boolean supportsLocalLife;

    @Schema(description = "Current member entrance generation status")
    private String linkStatus;

    @Schema(description = "Current member entrance type")
    private String linkType;

    @Schema(description = "Current member entrance message")
    private String linkMessage;

    @Schema(description = "Current member attribution status")
    private String attributionStatus;

    @Schema(description = "Current member attribution message")
    private String attributionMessage;

    @Schema(description = "Current member official promotion URL")
    private String promotionUrl;

    @Schema(description = "Current member platform command")
    private String tpwd;

    @Schema(description = "Current member promotion content")
    private String promotionContent;
}
