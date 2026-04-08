package cn.iocoder.yudao.module.cps.controller.admin.rebate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 返利记录 Response VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - 返利记录 Response VO")
@Data
public class CpsRebateRecordRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "平台订单号")
    private String platformOrderId;

    @Schema(description = "商品ID")
    private String itemId;

    @Schema(description = "商品标题")
    private String itemTitle;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "可分配佣金")
    private BigDecimal commissionAmount;

    @Schema(description = "返利比例（百分比）")
    private BigDecimal rebateRate;

    @Schema(description = "返利金额")
    private BigDecimal rebateAmount;

    @Schema(description = "返利类型（rebate=正常返利, reverse=退款扣回）")
    private String rebateType;

    @Schema(description = "返利状态（received=已到账, refunded=已扣回）")
    private String rebateStatus;

    @Schema(description = "前序返利ID（扣回时关联原始记录）")
    private Long precedingRebateId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
