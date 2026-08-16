package com.qiji.cps.module.cps.controller.admin.adzone.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 京东 PID 获取 Request VO")
@Data
public class CpsJdPidReqVO {

    @Schema(description = "联盟 ID；为空时读取京东供应商配置 unionId")
    private Long unionId;

    @Schema(description = "子联盟 ID")
    private Long childUnionId;

    @Schema(description = "推广类型")
    private Integer promotionType;

    @Schema(description = "推广位名称")
    private String positionName;

    @Schema(description = "媒体名称")
    private String mediaName;
}
