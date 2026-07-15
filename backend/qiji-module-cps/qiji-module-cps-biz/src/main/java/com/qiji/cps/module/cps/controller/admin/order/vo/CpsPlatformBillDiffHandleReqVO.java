package com.qiji.cps.module.cps.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CPS 平台账单差异处理 Request VO")
@Data
public class CpsPlatformBillDiffHandleReqVO {

    @NotNull
    private Long id;
    @NotNull
    private Long operatorId;
    private String conclusion;
    private String auditNote;
}
