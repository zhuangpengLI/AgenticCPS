package com.qiji.cps.module.cps.controller.admin.activity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - CPS活动中心第三方活动同步 Request VO")
@Data
public class CpsRebateActivitySyncReqVO {

    @Schema(description = "供应商编码：dataoke/haodanku/jutuike", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "dataoke")
    @NotBlank(message = "供应商编码不能为空")
    private String vendorCode;

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "搜索关键词", example = "618")
    private String keyword;

    @Schema(description = "每页条数", example = "20")
    private Integer pageSize;

    @Schema(description = "最大同步页数", example = "1")
    private Integer maxPages;
}
