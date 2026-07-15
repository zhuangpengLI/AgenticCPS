package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderStatusEventDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsPlatformBillDiffDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateDebtDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsFundsTraceResult {

    private CpsOrderDO order;
    private List<CpsOrderStatusEventDO> statusEvents;
    private List<CpsRebateRecordDO> rebateRecords;
    private List<CpsFreezeRecordDO> freezeRecords;
    private List<CpsRebateDebtDO> debtRecords;
    private List<CpsRebateAssetLedgerDO> assetLedgers;
    private List<CpsPlatformBillDiffDO> billDiffs;
    private List<String> traceWarnings;
    private boolean traceComplete;
}
