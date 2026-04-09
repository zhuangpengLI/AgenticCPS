package cn.iocoder.yudao.module.cps.controller.admin.vendor.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CPS API供应商配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsApiVendorPageReqVO extends PageParam {

    @Schema(description = "供应商编码", example = "dataoke")
    private String vendorCode;

    @Schema(description = "供应商名称", example = "大淘客")
    private String vendorName;

    @Schema(description = "供应商类型", example = "aggregator")
    private String vendorType;

    @Schema(description = "电商平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "状态", example = "1")
    private Integer status;

}
