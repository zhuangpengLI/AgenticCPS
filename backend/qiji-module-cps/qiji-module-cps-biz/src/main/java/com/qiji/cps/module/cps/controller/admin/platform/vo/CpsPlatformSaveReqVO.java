package com.qiji.cps.module.cps.controller.admin.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CPS平台配置创建/修改 Request VO")
@Data
public class CpsPlatformSaveReqVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    @Schema(description = "平台名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "淘宝联盟")
    @NotBlank(message = "平台名称不能为空")
    private String platformName;

    @Schema(description = "平台Logo图片URL", example = "https://xxx.com/logo.png")
    private String platformLogo;

    @Schema(description = "AppKey", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "AppKey不能为空")
    private String appKey;

    @Schema(description = "AppSecret", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "AppSecret不能为空")
    private String appSecret;

    @Schema(description = "API基础URL")
    private String apiBaseUrl;

    @Schema(description = "授权令牌")
    private String authToken;

    @Schema(description = "默认推广位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "默认推广位ID不能为空")
    private String defaultAdzoneId;

    @Schema(description = "平台服务费率（百分比）", example = "0.06")
    private BigDecimal platformServiceRate;

    @Schema(description = "排序权重", example = "0")
    private Integer sort;

    @Schema(description = "状态（0禁用 1启用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "扩展配置（JSON格式）")
    private String extraConfig;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "当前激活的供应商编码", example = "dataoke")
    private String activeVendorCode;

}
