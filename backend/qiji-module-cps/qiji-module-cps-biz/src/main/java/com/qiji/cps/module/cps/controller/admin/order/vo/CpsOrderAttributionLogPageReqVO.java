package com.qiji.cps.module.cps.controller.admin.order.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.qiji.cps.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CPS 订单归因日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsOrderAttributionLogPageReqVO extends PageParam {

    @Schema(description = "订单 ID", example = "1")
    private Long orderId;

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "平台订单号", example = "TB20260713001")
    private String platformOrderId;

    @Schema(description = "候选会员 ID", example = "1001")
    private Long candidateMemberId;

    @Schema(description = "最终归因会员 ID", example = "1001")
    private Long attributedMemberId;

    @Schema(description = "归因来源", example = "specialId")
    private String attributionSource;

    @Schema(description = "动作", example = "AUTO")
    private String action;

    @Schema(description = "归因结果", example = "BOUND")
    private String result;

    @Schema(description = "审核状态", example = "PENDING_REVIEW")
    private String reviewStatus;

    @Schema(description = "创建时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;
}
