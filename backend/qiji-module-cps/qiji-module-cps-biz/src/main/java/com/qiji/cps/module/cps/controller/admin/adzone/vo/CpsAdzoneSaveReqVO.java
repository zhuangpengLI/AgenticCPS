package com.qiji.cps.module.cps.controller.admin.adzone.vo;

import com.qiji.cps.framework.common.validation.InEnum;
import com.qiji.cps.module.cps.enums.CpsAdzoneTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CPS推广位创建/修改 Request VO")
@Data
public class CpsAdzoneSaveReqVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    @Schema(description = "推广位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "推广位ID不能为空")
    private String adzoneId;

    @Schema(description = "推广位名称")
    private String adzoneName;

    @Schema(description = "推广位类型：general(通用)/channel(渠道专属)/member(用户专属)", example = "general")
    @InEnum(value = CpsAdzoneTypeEnum.class, message = "推广位类型不合法，可选值：general/channel/member")
    private String adzoneType;

    @Schema(description = "关联类型（自动同步自 adzoneType）")
    private String relationType;

    @Schema(description = "关联ID（channel/member 类型时必填）")
    private Long relationId;

    @Schema(description = "是否默认", example = "0")
    private Integer isDefault;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

}
