package com.qiji.cps.module.cps.controller.admin.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS营销点击记录 Request VO")
@Data
public class CpsMarketingClickEventRecordReqVO {

    @Schema(description = "短码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "短码不能为空")
    private String shortCode;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "设备指纹")
    private String deviceFingerprint;

    @Schema(description = "归因键，仅用于摘要计算")
    private String memberAttributionKey;

    @Schema(description = "可信来源")
    private String trustedSource;

    @Schema(description = "点击时间")
    private LocalDateTime clickTime;
}
