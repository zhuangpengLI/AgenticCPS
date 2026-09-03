package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;

public interface CpsRebateAssetPolicyService {

    CpsRebateAssetPolicyDO getPolicy();

    /**
     * 幂等创建当前租户的默认策略，并补齐全平台冻结兜底规则。
     * 该操作不会启用返利资产，也不会修改已有策略。
     */
    CpsRebateAssetPolicyDO initializePolicy();

    /** 记录管理员确认的 Release B 审批凭证，并绑定最新 READY 预检批次。 */
    CpsRebateAssetPolicyDO confirmMigrationReady(String approvalRef);

    void savePolicy(CpsRebateAssetPolicyDO policy);

    void assertWritable();
}
