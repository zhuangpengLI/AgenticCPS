package com.qiji.cps.module.cps.controller.admin.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS营销漏斗 Request VO")
@Data
public class CpsMarketingFunnelReqVO {

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "营销活动ID")
    private String campaignId;

    @Schema(description = "素材ID")
    private String creativeId;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
