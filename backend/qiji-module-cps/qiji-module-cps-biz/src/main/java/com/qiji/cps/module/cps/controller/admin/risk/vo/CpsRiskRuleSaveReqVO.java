package com.qiji.cps.module.cps.controller.admin.risk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理后台 - CPS风控规则 创建/修改 Request VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - CPS风控规则创建/修改 Request VO")
@Data
public class CpsRiskRuleSaveReqVO {

    @Schema(description = "主键ID（修改时必填）", example = "1")
    private Long id;

    @Schema(description = "规则类型（rate_limit:频率限制 blacklist:黑名单）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "rate_limit")
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    @Schema(description = "目标类型（member:会员 ip:IP）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "member")
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    @Schema(description = "目标值（blacklist类型：会员ID或IP地址；rate_limit类型留空表示全量限制）",
            example = "12345")
    private String targetValue;

    @Schema(description = "限制次数（rate_limit类型：每日最大转链次数）", example = "100")
    @Min(value = 1, message = "限制次数至少为1次")
    private Integer limitCount;

    @Schema(description = "状态（0禁用 1启用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "每日转链次数默认上限")
    private String remark;

}
