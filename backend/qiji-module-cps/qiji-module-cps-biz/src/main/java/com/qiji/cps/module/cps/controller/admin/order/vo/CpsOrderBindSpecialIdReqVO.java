package com.qiji.cps.module.cps.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "请求幂等键", requiredMode = Schema.RequiredMode.REQUIRED, example = "manual-bind-20260714-0001")
    @NotBlank(message = "请求幂等键不能为空")
    @Size(max = 128, message = "请求幂等键长度不能超过128个字符")
    private String idempotencyKey;

    @Schema(description = "人工复核说明", example = "平台截图与会员申诉单一致")
    @Size(max = 500, message = "人工复核说明长度不能超过500个字符")
    private String auditNote;

}
