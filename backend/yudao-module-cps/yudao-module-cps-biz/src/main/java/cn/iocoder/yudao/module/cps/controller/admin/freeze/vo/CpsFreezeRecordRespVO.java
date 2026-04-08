package cn.iocoder.yudao.module.cps.controller.admin.freeze.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - CPS冻结记录 Response VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - CPS冻结记录 Response VO")
@Data
public class CpsFreezeRecordRespVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "会员ID", example = "1001")
    private Long memberId;

    @Schema(description = "订单ID", example = "10001")
    private Long orderId;

    @Schema(description = "平台订单号", example = "TB_20240101001")
    private String platformOrderId;

    @Schema(description = "冻结金额", example = "50.00")
    private BigDecimal freezeAmount;

    @Schema(description = "计划解冻时间")
    private LocalDateTime unfreezeTime;

    @Schema(description = "实际解冻时间")
    private LocalDateTime actualUnfreezeTime;

    @Schema(description = "状态（pending/frozen/unfreezing/unfreezed）", example = "frozen")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
