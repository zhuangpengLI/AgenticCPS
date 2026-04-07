package cn.iocoder.yudao.module.cps.controller.admin.rebate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 返利配置 Response VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - 返利配置 Response VO")
@Data
public class CpsRebateConfigRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "会员等级ID（NULL表示无等级限制）")
    private Long memberLevelId;

    @Schema(description = "平台编码（NULL表示全平台）")
    private String platformCode;

    @Schema(description = "返利比例（百分比）")
    private BigDecimal rebateRate;

    @Schema(description = "单笔最大返利金额（0表示不限）")
    private BigDecimal maxRebateAmount;

    @Schema(description = "单笔最小返利金额（0表示不限）")
    private BigDecimal minRebateAmount;

    @Schema(description = "状态（0禁用 1启用）")
    private Integer status;

    @Schema(description = "优先级（数字越大优先级越高）")
    private Integer priority;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
