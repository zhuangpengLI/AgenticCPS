package com.qiji.cps.module.cps.service.refund;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import com.qiji.cps.module.cps.dal.dataobject.refund.CpsRefundReportImportDO;
import com.qiji.cps.module.cps.dal.dataobject.refund.CpsRefundReportDetailDO;
import java.util.List;

public interface CpsRefundReportImportService {
    CpsRefundReportImportResult importReport(MultipartFile file, String platformCode,
                                             String vendorCode, String reportPeriod) throws IOException;
    CpsRefundReportImportDO getReport(Long id);
    List<CpsRefundReportDetailDO> getDetails(Long id);
}
