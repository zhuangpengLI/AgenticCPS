package com.qiji.cps.module.cps.controller.app.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 APP - CPS选品主题 Request VO")
@Data
public class AppCpsMarketingSelectionThemeReqVO {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "促销事件")
    private String promotionEvent;
}
