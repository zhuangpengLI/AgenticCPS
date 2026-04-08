package cn.iocoder.yudao.module.cps.controller.admin.freeze.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 冻结配置 Response VO")
@Data
public class CpsFreezeConfigRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "平台编码（null=全平台）")
    private String platformCode;

    @Schema(description = "解冻天数")
    private Integer unfreezeDays;

    @Schema(description = "状态（0禁用 1启用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
