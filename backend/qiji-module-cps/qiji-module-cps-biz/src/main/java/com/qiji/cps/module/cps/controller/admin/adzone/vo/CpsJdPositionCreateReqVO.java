package com.qiji.cps.module.cps.controller.admin.adzone.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 京东远端推广位创建 Request VO")
@Data
public class CpsJdPositionCreateReqVO {

    @Schema(description = "联盟 ID；为空时读取京东供应商配置 unionId")
    private Long unionId;

    @Schema(description = "接口调用 key")
    private String key;

    @Schema(description = "联盟类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "联盟类型不能为空")
    private Integer unionType;

    @Schema(description = "推广位类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "推广位类型不能为空")
    private Integer type;

    @Schema(description = "媒体站点 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "媒体站点 ID 不能为空")
    private Long siteId;

    @Schema(description = "推广位名称列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "推广位名称列表不能为空")
    private List<String> names;
}
