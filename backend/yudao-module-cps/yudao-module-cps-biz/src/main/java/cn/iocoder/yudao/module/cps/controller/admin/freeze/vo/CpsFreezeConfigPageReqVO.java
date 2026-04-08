package cn.iocoder.yudao.module.cps.controller.admin.freeze.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 冻结配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsFreezeConfigPageReqVO extends PageParam {

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "状态（0禁用 1启用）", example = "1")
    private Integer status;

}
