package com.qiji.cps.module.cps.controller.admin.couponpool.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CpsCouponPoolUsableReqVO {

    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    private String vendorCode;

    @NotBlank(message = "外部商品ID不能为空")
    private String externalGoodsId;

    private String goodsSign;
}
