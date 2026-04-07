package cn.iocoder.yudao.module.cps.controller.admin.adzone.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CPS推广位分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsAdzonePageReqVO extends PageParam {

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "推广位名称", example = "默认推广位")
    private String adzoneName;

    @Schema(description = "推广位类型", example = "general")
    private String adzoneType;

    @Schema(description = "状态", example = "1")
    private Integer status;

}
