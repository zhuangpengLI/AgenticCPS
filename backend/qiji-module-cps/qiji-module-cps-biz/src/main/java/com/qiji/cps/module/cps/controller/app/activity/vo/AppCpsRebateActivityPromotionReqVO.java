package com.qiji.cps.module.cps.controller.app.activity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "用户 APP - CPS 活动推广链接 Request VO")
@Data
public class AppCpsRebateActivityPromotionReqVO {

    @NotNull(message = "活动 ID 不能为空")
    private Long activityId;

    @Size(max = 64, message = "渠道标识长度不能超过64个字符")
    private String channelTag;
}
