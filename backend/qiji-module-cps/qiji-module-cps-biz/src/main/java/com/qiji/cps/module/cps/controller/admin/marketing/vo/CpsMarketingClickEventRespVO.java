package com.qiji.cps.module.cps.controller.admin.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS营销点击 Response VO")
@Data
public class CpsMarketingClickEventRespVO {

    @Schema(description = "点击事件ID")
    private Long id;

    @Schema(description = "点击唯一ID")
    private String clickId;

    @Schema(description = "短码")
    private String shortCode;

    @Schema(description = "短链ID")
    private Long shortLinkId;

    @Schema(description = "营销活动ID")
    private String campaignId;

    @Schema(description = "素材ID")
    private String creativeId;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "归因摘要")
    private String memberAttributionHash;

    @Schema(description = "IP摘要")
    private String ipHash;

    @Schema(description = "UA摘要")
    private String userAgentHash;

    @Schema(description = "设备摘要")
    private String deviceHash;

    @Schema(description = "去重摘要")
    private String dedupeKey;

    @Schema(description = "可信来源")
    private String trustedSource;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "点击时间")
    private LocalDateTime clickTime;
}
