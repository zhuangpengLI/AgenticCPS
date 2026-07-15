package com.qiji.cps.module.cps.dal.mysql.rebate;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CpsRebateAssetPolicyMapper extends BaseMapperX<CpsRebateAssetPolicyDO> {

    default CpsRebateAssetPolicyDO selectCurrentTenant() {
        return selectOne(new LambdaQueryWrapperX<CpsRebateAssetPolicyDO>()
                .eq(CpsRebateAssetPolicyDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .last("LIMIT 1"));
    }
}
