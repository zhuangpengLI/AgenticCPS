package com.qiji.cps.module.cps.controller.admin.rebate.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.qiji.cps.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CPS 返利欠款分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CpsRebateDebtPageReqVO extends PageParam {
    private Long memberId;
    private Long orderId;
    private String status;
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;
}
