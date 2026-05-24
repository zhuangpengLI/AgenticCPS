package com.qiji.cps.module.cps.controller.admin.activity.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CPS返利活动分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsRebateActivityPageReqVO extends PageParam {

    @Schema(description = "活动名称", example = "饿了么外卖红包")
    private String activityName;

    @Schema(description = "专题类型", example = "外卖")
    private String activityType;

    @Schema(description = "平台编码", example = "meituan")
    private String platformCode;

    @Schema(description = "计费类型", example = "CPS")
    private String billingType;

    @Schema(description = "状态", example = "1")
    private Integer status;

}
