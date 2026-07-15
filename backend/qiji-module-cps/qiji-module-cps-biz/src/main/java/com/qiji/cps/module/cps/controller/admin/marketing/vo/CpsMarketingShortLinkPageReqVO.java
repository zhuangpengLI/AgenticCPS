package com.qiji.cps.module.cps.controller.admin.marketing.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CPS营销短链分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsMarketingShortLinkPageReqVO extends PageParam {

    @Schema(description = "短码")
    private String shortCode;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "营销活动ID")
    private String campaignId;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "状态（0禁用 1启用）")
    private Integer status;
}
