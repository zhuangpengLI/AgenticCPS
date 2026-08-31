package com.qiji.cps.module.cps.service.refund;

import com.qiji.cps.module.cps.dal.dataobject.refund.CpsRefundReportImportDO;
import com.qiji.cps.module.cps.dal.dataobject.refund.CpsRefundReportDetailDO;
import java.util.List;

public record CpsRefundReportImportResult(CpsRefundReportImportDO report, boolean duplicate, List<CpsRefundReportDetailDO> details) {
    public CpsRefundReportImportResult(CpsRefundReportImportDO report, boolean duplicate) {
        this(report, duplicate, List.of());
    }
}
