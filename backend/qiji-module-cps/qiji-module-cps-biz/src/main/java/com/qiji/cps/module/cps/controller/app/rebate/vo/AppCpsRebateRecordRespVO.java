package com.qiji.cps.module.cps.controller.app.rebate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户 APP - 返利记录 Response VO
 *
 * @author CPS System
 */
@Schema(description = "用户 APP - 返利记录 Response VO")
@Data
public class AppCpsRebateRecordRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "平台编码（taobao/jd/pdd/douyin）")
    private String platformCode;

    @Schema(description = "平台订单号")
    private String platformOrderId;

    @Schema(description = "商品标题")
    private String itemTitle;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "返利金额")
    private BigDecimal rebateAmount;

    @Schema(description = "返利类型（rebate=正常返利, reverse=退款扣回）")
    private String rebateType;

    @Schema(description = "返利状态（received=已到账, refunded=已扣回）")
    private String rebateStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
