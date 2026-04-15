package com.qiji.cps.module.cps.controller.admin.vendor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CPS API供应商配置创建/修改 Request VO")
@Data
public class CpsApiVendorSaveReqVO {

    @Schema(description = "主键ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "供应商编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "dataoke")
    @NotBlank(message = "供应商编码不能为空")
    private String vendorCode;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "大淘客")
    @NotBlank(message = "供应商名称不能为空")
    private String vendorName;

    @Schema(description = "供应商类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "aggregator")
    @NotBlank(message = "供应商类型不能为空")
    private String vendorType;

    @Schema(description = "电商平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    @NotBlank(message = "电商平台编码不能为空")
    private String platformCode;

    @Schema(description = "API Key", requiredMode = Schema.RequiredMode.REQUIRED, example = "5vxxx")
    @NotBlank(message = "API Key不能为空")
    private String appKey;

    @Schema(description = "API Secret（新增时必填；更新时留空表示不修改）", example = "9fxxx")
    private String appSecret;

    @Schema(description = "API基础URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://openapi.dataoke.com/api")
    @NotBlank(message = "API基础URL不能为空")
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
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "大淘客-淘宝配置")
    private String remark;

}
