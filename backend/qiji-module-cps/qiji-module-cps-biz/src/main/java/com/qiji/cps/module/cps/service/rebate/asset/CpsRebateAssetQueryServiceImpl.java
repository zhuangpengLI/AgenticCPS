package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.framework.common.pojo.PageParam;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateAssetLedgerPageReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateDebtPageReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateDebtSummaryRespVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateDebtDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetLedgerMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateDebtMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CpsRebateAssetQueryServiceImpl implements CpsRebateAssetQueryService {

    @Resource
    private CpsRebateDebtMapper debtMapper;
    @Resource
    private CpsRebateAssetLedgerMapper ledgerMapper;

    @Override
    public PageResult<CpsRebateDebtDO> getDebtPage(CpsRebateDebtPageReqVO reqVO) {
        return debtMapper.selectPage(reqVO);
    }

    @Override
    public CpsRebateDebtDO getDebt(Long id) {
        return debtMapper.selectById(id);
    }

    @Override
    public CpsRebateDebtSummaryRespVO getDebtSummary(Long memberId) {
        List<CpsRebateDebtDO> debts = debtMapper.selectListByMemberId(memberId);
        return CpsRebateDebtSummaryRespVO.builder()
                .memberId(memberId)
                .debtCount((long) debts.size())
                .originalDebtCent(sum(debts, CpsRebateDebtDO::getOriginalDebtCent))
                .repaidDebtCent(sum(debts, CpsRebateDebtDO::getRepaidDebtCent))
                .waivedDebtCent(sum(debts, CpsRebateDebtDO::getWaivedDebtCent))
                .outstandingDebtCent(sum(debts, CpsRebateDebtDO::getOutstandingDebtCent))
                .build();
    }

    @Override
    public PageResult<CpsRebateAssetLedgerDO> getLedgerPage(CpsRebateAssetLedgerPageReqVO reqVO) {
        return ledgerMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<CpsRebateAssetLedgerDO> getMemberDebtRepaymentPage(Long memberId, PageParam pageParam) {
        return ledgerMapper.selectDebtRepaymentPage(memberId, pageParam);
    }

    private long sum(List<CpsRebateDebtDO> debts,
                     java.util.function.Function<CpsRebateDebtDO, Long> getter) {
        return debts.stream().map(getter).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
    }
}
