package com.qiji.cps.module.cps.controller.admin.adzone.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 京东远端推广位查询 Request VO")
@Data
public class CpsJdPositionQueryReqVO {

    @Schema(description = "联盟 ID；为空时读取京东供应商配置 unionId")
    private Long unionId;

    @Schema(description = "接口调用 key")
    private String key;

    @Schema(description = "联盟类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "联盟类型不能为空")
    private Integer unionType;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码必须大于 0")
    private Integer pageNo = 1;

    @Schema(description = "每页条数", example = "20")
    @Min(value = 1, message = "每页条数必须大于 0")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 20;
}
