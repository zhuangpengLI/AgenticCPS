package com.qiji.cps.module.cps.controller.openapi.rebate.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OpenApiCpsRebateUnfreezeReqVO {
    @NotBlank(message = "冻结ID不能为空")
    private String freezeId;

    private String reason;
}
