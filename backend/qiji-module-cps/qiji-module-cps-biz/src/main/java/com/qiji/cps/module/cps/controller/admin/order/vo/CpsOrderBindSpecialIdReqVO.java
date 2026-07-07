package com.qiji.cps.module.cps.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Schema(description = "管理后台 - CPS订单 special_id 手动绑定会员 Request VO")
@Data
public class CpsOrderBindSpecialIdReqVO {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "会员ID不能为空")
    @Positive(message = "会员ID必须为正数")
    private Long memberId;

}
