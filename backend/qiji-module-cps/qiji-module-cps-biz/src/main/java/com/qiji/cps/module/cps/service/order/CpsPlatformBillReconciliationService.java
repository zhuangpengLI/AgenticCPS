package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsPlatformBillDiffPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsPlatformBillDiffDO;

import java.util.List;

public interface CpsPlatformBillReconciliationService {

    CpsPlatformBillReconciliationResult importAndReconcile(List<CpsPlatformBillImportRowCommand> rows);

    PageResult<CpsPlatformBillDiffDO> getDiffPage(CpsPlatformBillDiffPageReqVO reqVO);

    void handleDiff(Long id, Long operatorId, String conclusion, String auditNote);

    void requestTargetedRepull(Long id, Long operatorId, String auditNote);
}
