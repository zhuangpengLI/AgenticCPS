package com.qiji.cps.module.cps.controller.admin.activity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS返利活动 Response VO")
@Data
public class CpsRebateActivityRespVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "活动名称", example = "饿了么外卖红包")
    private String activityName;

    @Schema(description = "专题类型", example = "外卖")
    private String activityType;

    @Schema(description = "平台编码", example = "eleme")
    private String platformCode;

    @Schema(description = "活动主图")
    private String mainPic;

    @Schema(description = "短描述")
    private String shortDesc;

    @Schema(description = "返利文案")
    private String rebateDesc;

    @Schema(description = "计费类型", example = "CPS")
    private String billingType;

    @Schema(description = "推广数", example = "1405")
    private Integer promotionCount;

    @Schema(description = "来源类型", example = "configured")
    private String sourceType;

    @Schema(description = "外部活动ID")
    private String externalActivityId;

    @Schema(description = "标签文案", example = "热")
    private String tagText;

    @Schema(description = "跳转类型", example = "search")
    private String jumpType;

    @Schema(description = "跳转地址")
    private String jumpUrl;

    @Schema(description = "搜索关键词")
    private String searchKeyword;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "状态（0禁用 1启用）", example = "1")
    private Integer status;

    @Schema(description = "上线时间")
    private LocalDateTime startTime;

    @Schema(description = "下线时间")
    private LocalDateTime endTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
