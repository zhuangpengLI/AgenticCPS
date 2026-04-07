package cn.iocoder.yudao.module.cps.controller.admin.platform.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CPS平台配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsPlatformPageReqVO extends PageParam {

    @Schema(description = "平台名称", example = "淘宝联盟")
    private String platformName;

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "状态", example = "1")
    private Integer status;

}
