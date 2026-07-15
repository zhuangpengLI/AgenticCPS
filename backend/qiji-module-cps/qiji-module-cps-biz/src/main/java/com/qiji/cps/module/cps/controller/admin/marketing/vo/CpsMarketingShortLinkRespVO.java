package com.qiji.cps.module.cps.controller.admin.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS营销短链 Response VO")
@Data
public class CpsMarketingShortLinkRespVO {

    @Schema(description = "短链ID")
    private Long id;

    @Schema(description = "短码")
    private String shortCode;

    @Schema(description = "目标跳转链接")
    private String targetUrl;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "已有转链记录ID")
    private Long transferRecordId;

    @Schema(description = "营销活动ID")
    private String campaignId;

    @Schema(description = "素材ID")
    private String creativeId;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "会员归因摘要")
    private String memberAttributionHash;

    @Schema(description = "请求摘要")
    private String requestHash;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "访问次数")
    private Long accessCount;

    @Schema(description = "最近访问时间")
    private LocalDateTime lastAccessTime;
}
