package com.qiji.cps.module.cps.controller.admin.adzone.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 京东渠道关系生成 Request VO")
@Data
public class CpsJdChannelRelationReqVO {

    @Schema(description = "邀请码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "渠道备注")
    private String channelNote;
}
