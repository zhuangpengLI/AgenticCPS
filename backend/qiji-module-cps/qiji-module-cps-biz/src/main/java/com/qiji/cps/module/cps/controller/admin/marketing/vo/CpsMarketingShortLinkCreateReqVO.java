package com.qiji.cps.module.cps.controller.admin.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS营销短链创建 Request VO")
@Data
public class CpsMarketingShortLinkCreateReqVO {

    @Schema(description = "目标跳转链接", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标跳转链接不能为空")
    private String targetUrl;

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "平台编码不能为空")
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

    @Schema(description = "会员归因键，仅用于服务端计算摘要，不落库明文")
    private String memberAttributionKey;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}
