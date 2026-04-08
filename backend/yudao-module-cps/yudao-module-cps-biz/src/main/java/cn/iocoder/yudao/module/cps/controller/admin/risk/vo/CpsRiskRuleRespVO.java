package cn.iocoder.yudao.module.cps.controller.admin.risk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台 - CPS风控规则 Response VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - CPS风控规则 Response VO")
@Data
public class CpsRiskRuleRespVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "规则类型（rate_limit:频率限制 blacklist:黑名单）", example = "rate_limit")
    private String ruleType;

    @Schema(description = "目标类型（member:会员 ip:IP）", example = "member")
    private String targetType;

    @Schema(description = "目标值（blacklist类型：会员ID或IP地址）", example = "12345")
    private String targetValue;

    @Schema(description = "限制次数（rate_limit类型）", example = "100")
    private Integer limitCount;

    @Schema(description = "状态（0禁用 1启用）", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "每日转链次数默认上限")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
