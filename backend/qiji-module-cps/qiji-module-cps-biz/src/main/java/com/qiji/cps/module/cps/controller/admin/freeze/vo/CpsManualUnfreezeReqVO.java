package com.qiji.cps.module.cps.controller.admin.freeze.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "管理后台 - 受控手动解冻请求")
public class CpsManualUnfreezeReqVO {

    @NotNull(message = "冻结记录ID不能为空")
    private Long recordId;

    @NotBlank(message = "解冻原因不能为空")
    private String reason;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
