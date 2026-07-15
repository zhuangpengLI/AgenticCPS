package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;

public interface CpsRebateAssetPolicyService {

    CpsRebateAssetPolicyDO getPolicy();

    void savePolicy(CpsRebateAssetPolicyDO policy);

    void assertWritable();
}
