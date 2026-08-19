package com.qiji.cps.module.cps.controller.app.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 APP - CPS 装修活动卡片 Response VO")
@Data
public class AppCpsMarketingActivityCardRespVO {

    @Schema(description = "活动 ID")
    private Long id;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "平台名称")
    private String platformName;

    @Schema(description = "主图")
    private String mainPic;

    @Schema(description = "短描述")
    private String shortDesc;

    @Schema(description = "返利说明")
    private String rebateDesc;

    @Schema(description = "标签")
    private String tagText;

    @Schema(description = "跳转类型")
    private String jumpType;

    @Schema(description = "跳转地址")
    private String jumpUrl;

    @Schema(description = "搜索关键词")
    private String searchKeyword;
}
