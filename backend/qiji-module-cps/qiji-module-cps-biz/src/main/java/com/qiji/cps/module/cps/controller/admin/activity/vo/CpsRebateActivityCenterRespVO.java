package com.qiji.cps.module.cps.controller.admin.activity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - CPS活动中心聚合查询 Response VO")
@Data
@Builder
public class CpsRebateActivityCenterRespVO {

    @Schema(description = "平台/场景页签")
    private List<Tab> tabs;

    @Schema(description = "计费类型选项")
    private List<Option> billingTypeOptions;

    @Schema(description = "活动卡片列表")
    private List<Card> cards;

    @Schema(description = "总量")
    private Long total;

    @Schema(description = "页码")
    private Integer pageNo;

    @Schema(description = "每页条数")
    private Integer pageSize;

    @Schema(description = "活动中心平台/场景页签")
    @Data
    @Builder
    public static class Tab {

        @Schema(description = "平台编码")
        private String platformCode;

        @Schema(description = "平台名称")
        private String platformName;

        @Schema(description = "平台 Logo")
        private String platformLogo;

        @Schema(description = "活动数量")
        private Integer activityCount;

    }

    @Schema(description = "活动中心选项")
    @Data
    @Builder
    public static class Option {

        @Schema(description = "选项值")
        private String value;

        @Schema(description = "选项名称")
        private String label;

        @Schema(description = "活动数量")
        private Integer count;

    }

    @Schema(description = "活动中心活动卡片")
    @Data
    @Builder
    public static class Card {

        @Schema(description = "活动 ID")
        private Long id;

        @Schema(description = "活动名称")
        private String activityName;

        @Schema(description = "专题类型")
        private String activityType;

        @Schema(description = "平台编码")
        private String platformCode;

        @Schema(description = "平台名称")
        private String platformName;

        @Schema(description = "平台 Logo")
        private String platformLogo;

        @Schema(description = "活动主图")
        private String mainPic;

        @Schema(description = "短描述")
        private String shortDesc;

        @Schema(description = "返利/奖励文案")
        private String rebateDesc;

        @Schema(description = "计费类型")
        private String billingType;

        @Schema(description = "推广数")
        private Integer promotionCount;

        @Schema(description = "来源类型")
        private String sourceType;

        @Schema(description = "外部活动 ID")
        private String externalActivityId;

        @Schema(description = "标签文案")
        private String tagText;

        @Schema(description = "跳转类型")
        private String jumpType;

        @Schema(description = "跳转地址")
        private String jumpUrl;

        @Schema(description = "搜索关键词")
        private String searchKeyword;

        @Schema(description = "上线时间")
        private LocalDateTime startTime;

        @Schema(description = "下线时间")
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

    }

}
