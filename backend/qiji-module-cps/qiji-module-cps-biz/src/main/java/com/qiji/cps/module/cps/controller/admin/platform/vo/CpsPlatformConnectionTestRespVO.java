package com.qiji.cps.module.cps.controller.admin.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - CPS 平台连接测试 Response VO")
@Data
@Builder
public class CpsPlatformConnectionTestRespVO {

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "是否存在平台适配器")
    private Boolean supported;

    @Schema(description = "连接测试是否成功")
    private Boolean success;

    @Schema(description = "失败原因")
    private String failureReason;

}
