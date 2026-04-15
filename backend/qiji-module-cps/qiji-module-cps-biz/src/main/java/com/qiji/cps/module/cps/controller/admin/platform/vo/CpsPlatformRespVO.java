package com.qiji.cps.module.cps.controller.admin.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS平台配置 Response VO")
@Data
public class CpsPlatformRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    private String platformCode;

    @Schema(description = "平台名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "淘宝联盟")
    private String platformName;

    @Schema(description = "平台Logo图片URL")
    private String platformLogo;

    @Schema(description = "默认推广位ID")
    private String defaultAdzoneId;

    @Schema(description = "平台服务费率（百分比）")
    private BigDecimal platformServiceRate;

    @Schema(description = "排序权重")
    private Integer sort;

    @Schema(description = "状态（0禁用 1启用）")
    private Integer status;

    @Schema(description = "扩展配置（JSON格式）")
    private String extraConfig;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "当前激活的供应商编码", example = "dataoke")
    private String activeVendorCode;

    @Schema(description = "支持的供应商列表（逗号分隔）", example = "dataoke,haodanku")
    private String supportedVendors;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
