package com.qiji.cps.module.cps.controller.admin.freeze.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理后台 - CPS冻结配置 创建/修改 Request VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - CPS冻结配置创建/修改 Request VO")
@Data
public class CpsFreezeConfigSaveReqVO {

    @Schema(description = "主键ID（修改时必填）", example = "1")
    private Long id;

    @Schema(description = "平台编码（NULL表示全平台默认配置）", example = "taobao")
    private String platformCode;

    @Schema(description = "解冻天数（确认收货后N天自动解冻）", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    @NotNull(message = "解冻天数不能为空")
    @Min(value = 1, message = "解冻天数至少为1天")
    private Integer unfreezeDays;

    @Schema(description = "状态（0禁用 1启用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "淘宝平台专属解冻配置")
    private String remark;

}
