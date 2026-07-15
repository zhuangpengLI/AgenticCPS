package com.qiji.cps.module.cps.controller.admin.rebate.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.qiji.cps.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
@EqualsAndHashCode(callSuper = true)
public class CpsRebateAssetLedgerPageReqVO extends PageParam {
    private Long memberId;
    private String businessType;
    private String businessId;
    private Long orderId;
    private String idempotencyKey;
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;
}
