package com.qiji.cps.module.cps.controller.admin.selection.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CPS选品主题分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsSelectionThemePageReqVO extends PageParam {

    @Schema(description = "主题编码")
    private String themeCode;

    @Schema(description = "主题名称")
    private String themeName;

    @Schema(description = "主题类型")
    private String themeType;

    @Schema(description = "大促标识")
    private String promotionEvent;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "是否展示到返利商品广场：0否 1是")
    private Integer goodsSquareVisible;
}
