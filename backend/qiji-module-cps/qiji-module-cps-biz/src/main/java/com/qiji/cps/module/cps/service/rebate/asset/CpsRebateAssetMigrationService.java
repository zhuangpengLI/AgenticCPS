package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAccountMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetLedgerMapper;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CpsRebateAssetMigrationService {

    private final CpsRebateAccountMapper accountMapper;
    private final CpsRebateAssetLedgerMapper ledgerMapper;
    private final CpsMoneyConverter moneyConverter;
    private final CpsRebateAssetPolicyService policyService;

    /**
     * 为当前租户历史账户追加期初流水，不修改账户余额。仅允许在V2首次启用前执行，可安全重复调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public int backfillOpeningBalances(String operatorId) {
        CpsRebateAssetPolicyDO policy = policyService.getPolicy();
        if (Boolean.TRUE.equals(policy.getV2Enabled()) || Boolean.TRUE.equals(policy.getMigrationReady())) {
            throw new IllegalStateException("资产迁移已就绪或V2已启用，禁止补写期初流水");
        }
        long tenantId = TenantContextHolder.getRequiredTenantId();
        int created = 0;
        for (CpsRebateAccountDO account : accountMapper.selectList(new LambdaQueryWrapperX<CpsRebateAccountDO>()
                .eq(CpsRebateAccountDO::getTenantId, tenantId))) {
            if (account.getId() == null || ledgerMapper.selectOpeningBalanceByAccountId(account.getId()) != null) {
                continue;
            }
            long available = moneyConverter.yuanToCent(account.getAvailableBalance());
            long frozen = moneyConverter.yuanToCent(account.getFrozenBalance());
            long debt = moneyConverter.yuanToCent(account.getDebtBalance());
            CpsRebateAssetLedgerDO openingLedger = CpsRebateAssetLedgerDO.builder()
                    .memberId(account.getMemberId()).sourceSystem("CPS_MIGRATION")
                    .businessType("OPENING_BALANCE").businessId(String.valueOf(account.getId()))
                    .idempotencyKey("opening-balance:" + account.getId())
                    .availableChangeCent(available).frozenChangeCent(frozen).debtChangeCent(debt)
                    .availableBeforeCent(0L).availableAfterCent(available)
                    .frozenBeforeCent(0L).frozenAfterCent(frozen)
                    .debtBeforeCent(0L).debtAfterCent(debt)
                    .operatorType("ADMIN").operatorId(operatorId)
                    .reason("阶段一资产V2历史账户期初余额回填")
                    .build();
            openingLedger.setTenantId(tenantId);
            try {
                ledgerMapper.insert(openingLedger);
                created++;
            } catch (DuplicateKeyException duplicate) {
                if (ledgerMapper.selectOpeningBalanceByAccountId(account.getId()) == null) {
                    throw duplicate;
                }
                // 并发回填已由另一事务成功写入，按幂等成功处理。
            }
        }
        return created;
    }
}
