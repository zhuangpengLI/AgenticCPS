package com.qiji.cps.module.cps.controller.app.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 APP - CPS营销活动中心 Request VO")
@Data
public class AppCpsMarketingActivityReqVO {

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "结算类型")
    private String billingType;

    @Schema(description = "关键词")
    private String keyword;
}
