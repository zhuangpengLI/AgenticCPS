package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetMigrationCheckArchiveDO;

import java.util.List;

/** 只核对并归档，不修复、合并或删除历史资金数据。 */
public interface CpsRebateAssetMigrationCheckService {
    CpsRebateAssetMigrationCheckReport runCheck(String operatorId);

    List<CpsRebateAssetMigrationCheckArchiveDO> getArchives();
}
