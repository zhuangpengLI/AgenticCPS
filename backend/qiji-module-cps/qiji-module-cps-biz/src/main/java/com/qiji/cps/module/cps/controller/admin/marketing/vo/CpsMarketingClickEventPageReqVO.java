package com.qiji.cps.module.cps.controller.admin.marketing.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS营销点击分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsMarketingClickEventPageReqVO extends PageParam {

    @Schema(description = "短码")
    private String shortCode;

    @Schema(description = "营销活动ID")
    private String campaignId;

    @Schema(description = "素材ID")
    private String creativeId;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "可信来源")
    private String trustedSource;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
