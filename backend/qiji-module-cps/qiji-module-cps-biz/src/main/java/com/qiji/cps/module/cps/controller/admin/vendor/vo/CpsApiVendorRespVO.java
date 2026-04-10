package com.qiji.cps.module.cps.controller.admin.vendor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS API供应商配置 Response VO")
@Data
public class CpsApiVendorRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "供应商编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "dataoke")
    private String vendorCode;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "大淘客")
    private String vendorName;

    @Schema(description = "供应商类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "aggregator")
    private String vendorType;

    @Schema(description = "电商平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    private String platformCode;

    @Schema(description = "API Key", requiredMode = Schema.RequiredMode.REQUIRED, example = "5vxxx")
    private String appKey;

    @Schema(description = "API基础URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://openapi.dataoke.com/api")
    private String apiBaseUrl;

    @Schema(description = "授权令牌", example = "xxx")
    private String authToken;

    @Schema(description = "默认推广位ID", example = "mm_xxx_xxx_xxx")
    private String defaultAdzoneId;

    @Schema(description = "扩展配置（JSON格式）", example = "{}")
    private String extraConfig;

    @Schema(description = "优先级", example = "0")
    private Integer priority;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "备注", example = "大淘客-淘宝配置")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
