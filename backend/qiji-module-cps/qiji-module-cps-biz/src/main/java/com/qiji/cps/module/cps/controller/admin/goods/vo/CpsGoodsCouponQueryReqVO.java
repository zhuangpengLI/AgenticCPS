package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CPS工具箱优惠券查询 Request VO")
@Data
public class CpsGoodsCouponQueryReqVO {

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    @Schema(description = "关键词、商品ID或链接", requiredMode = Schema.RequiredMode.REQUIRED, example = "零食")
    @NotBlank(message = "查询内容不能为空")
    private String queryText;

    @Schema(description = "供应商编码", example = "haodanku")
    private String vendorCode;

    @Schema(description = "最低优惠券金额")
    private BigDecimal couponAmountMin;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = 1;

    @Schema(description = "每页大小", example = "10")
    @Min(value = 1, message = "每页大小最小值为 1")
    @Max(value = 50, message = "每页大小最大值为 50")
    private Integer pageSize = 10;

}
