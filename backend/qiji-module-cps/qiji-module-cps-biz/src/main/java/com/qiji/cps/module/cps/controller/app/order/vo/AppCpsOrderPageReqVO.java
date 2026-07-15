package com.qiji.cps.module.cps.controller.app.order.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "用户 APP - CPS订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppCpsOrderPageReqVO extends PageParam {

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "订单状态", example = "settled")
    private String orderStatus;
}
