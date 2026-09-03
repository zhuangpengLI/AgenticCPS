package com.qiji.cps.module.cps.controller.admin.rebate.vo;

import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetMigrationCheckReport;
import lombok.Builder;
import lombok.Value;

/** 一键准备返利资产的结果。 */
@Value
@Builder
public class CpsRebateAssetBootstrapRespVO {

    CpsRebateAssetPolicyDO policy;
    int openingBalanceCount;
    CpsRebateAssetMigrationCheckReport migrationReport;
    boolean enabled;
    String nextStep;
}
