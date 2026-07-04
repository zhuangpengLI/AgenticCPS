package com.qiji.cps.module.cps.controller.admin.activity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - CPS活动推广内容生成 Response VO")
@Data
@Builder
public class CpsRebateActivityPromotionRespVO {

    @Schema(description = "生成状态 SUCCESS/FAILED")
    private String linkStatus;

    @Schema(description = "生成提示")
    private String linkMessage;

    @Schema(description = "活动 ID")
    private Long activityId;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "推广位 ID / PID")
    private String adzoneId;

    @Schema(description = "渠道标识")
    private String channelTag;

    @Schema(description = "可投放活动链接")
    private String promotionUrl;

    @Schema(description = "淘口令")
    private String tpwd;

    @Schema(description = "长淘口令")
    private String longTpwd;

    @Schema(description = "可复制推广文案")
    private String promotionContent;

}
