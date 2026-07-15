package com.qiji.cps.module.cps.controller.admin.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - CPS营销漏斗 Response VO")
@Data
@Builder
public class CpsMarketingFunnelRespVO {

    @Schema(description = "曝光数")
    private Long exposureCount;

    @Schema(description = "点击数")
    private Long clickCount;

    @Schema(description = "转链数")
    private Long transferCount;

    @Schema(description = "订单数")
    private Long orderCount;

    @Schema(description = "已结算订单数")
    private Long settledOrderCount;

    @Schema(description = "返利可处理订单数")
    private Long rebateReadyOrderCount;
}
