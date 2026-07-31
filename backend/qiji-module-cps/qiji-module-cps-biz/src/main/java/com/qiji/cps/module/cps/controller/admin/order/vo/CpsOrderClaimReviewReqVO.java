package com.qiji.cps.module.cps.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - CPS 订单申领审核 Request VO")
@Data
public class CpsOrderClaimReviewReqVO {

    @NotNull(message = "申领记录ID不能为空")
    private Long claimId;

    @NotNull(message = "审核结论不能为空")
    private Boolean approved;

    @NotBlank(message = "审核说明不能为空")
    @Size(max = 500, message = "审核说明长度不能超过500个字符")
    private String auditNote;
}
