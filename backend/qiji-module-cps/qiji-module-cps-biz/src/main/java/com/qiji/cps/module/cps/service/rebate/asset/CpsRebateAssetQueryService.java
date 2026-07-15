package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.framework.common.pojo.PageParam;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateAssetLedgerPageReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateDebtPageReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateDebtSummaryRespVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateDebtDO;

public interface CpsRebateAssetQueryService {

    PageResult<CpsRebateDebtDO> getDebtPage(CpsRebateDebtPageReqVO reqVO);

    CpsRebateDebtDO getDebt(Long id);

    CpsRebateDebtSummaryRespVO getDebtSummary(Long memberId);

    PageResult<CpsRebateAssetLedgerDO> getLedgerPage(CpsRebateAssetLedgerPageReqVO reqVO);

    PageResult<CpsRebateAssetLedgerDO> getMemberDebtRepaymentPage(Long memberId, PageParam pageParam);
}
