package com.qiji.cps.module.cps.controller.admin.rebate.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CpsRebateDebtSummaryRespVO {
    private Long memberId;
    private Long debtCount;
    private Long originalDebtCent;
    private Long repaidDebtCent;
    private Long waivedDebtCent;
    private Long outstandingDebtCent;
}
