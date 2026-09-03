package com.qiji.cps.module.cps.controller.admin.rebate.vo;

import com.qiji.cps.framework.common.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理后台 - 返利配置新增/修改 Request VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - 返利配置新增/修改 Request VO")
@Data
public class CpsRebateConfigSaveReqVO {

    @Schema(description = "主键ID（修改时必填）")
    private Long id;

    @Schema(description = "会员ID（NULL表示非个人规则）")
    private Long memberId;

    @Schema(description = "会员等级ID（NULL表示无等级限制）")
    private Long memberLevelId;

    @Schema(description = "平台编码（NULL表示全平台）")
    private String platformCode;

    @Schema(description = "返利比例（百分比）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "返利比例不能为空")
    @DecimalMin(value = "0", message = "返利比例不能小于0")
    @DecimalMax(value = "100", message = "返利比例不能超过100")
    private BigDecimal rebateRate;

    @Schema(description = "单笔最大返利金额（0表示不限）")
    private BigDecimal maxRebateAmount;

    @Schema(description = "单笔最小返利金额（0表示不限）")
    private BigDecimal minRebateAmount;

    @Schema(description = "冻结阈值（元，实际返利大于该金额才冻结；为空或0表示不冻结）")
    @DecimalMin(value = "0", message = "冻结阈值不能小于0")
    private BigDecimal freezeThresholdAmount;

    @Schema(description = "冻结天数（配置冻结阈值时必填）")
    @Min(value = 1, message = "冻结天数必须大于0")
    @Max(value = 365, message = "冻结天数不能超过365天")
    private Integer freezeDays;

    @Schema(description = "状态（0禁用 1启用）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "优先级（数字越大优先级越高，默认0）")
    private Integer priority = 0;

}
