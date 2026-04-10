package com.qiji.cps.module.cps.controller.admin.adzone.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS推广位 Response VO")
@Data
public class CpsAdzoneRespVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "推广位ID")
    private String adzoneId;

    @Schema(description = "推广位名称")
    private String adzoneName;

    @Schema(description = "推广位类型", example = "general")
    private String adzoneType;

    @Schema(description = "关联类型")
    private String relationType;

    @Schema(description = "关联ID")
    private Long relationId;

    @Schema(description = "是否默认")
    private Integer isDefault;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
