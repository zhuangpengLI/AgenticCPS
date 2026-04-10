package com.qiji.cps.module.cps.controller.admin.risk.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - CPS风控规则分页查询 Request VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - CPS风控规则分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsRiskRulePageReqVO extends PageParam {

    @Schema(description = "规则类型（rate_limit:频率限制 blacklist:黑名单）", example = "rate_limit")
    private String ruleType;

    @Schema(description = "状态（0禁用 1启用）", example = "1")
    private Integer status;

}
