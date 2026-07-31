package com.qiji.cps.module.cps.controller.admin.activity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CPS活动推广内容生成 Request VO")
@Data
public class CpsRebateActivityPromotionReqVO {

    @Schema(description = "活动 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "活动 ID 不能为空")
    private Long activityId;

    @Schema(description = "推广位 ID / PID")
    private String adzoneId;

    @Schema(description = "渠道标识，用于文案标记")
    private String channelTag;

}
