package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理后台 - 商品返利查询 Request VO.
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - 商品返利查询 Request VO")
@Data
public class CpsGoodsRebateQueryReqVO {

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    @Schema(description = "原始商品链接、商品ID或口令", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原始内容不能为空")
    private String originalContent;

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "API供应商编码，不传则使用平台默认供应商", example = "dataoke")
    private String vendorCode;

    @Schema(description = "推广位ID，不传则使用会员推广位或平台默认推广位")
    private String adzoneId;

}
