package com.qiji.cps.module.cps.dal.mysql.rebate;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * CPS会员返利账户 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsRebateAccountMapper extends BaseMapperX<CpsRebateAccountDO> {

    default CpsRebateAccountDO selectByMemberId(Long memberId) {
        return selectOne(CpsRebateAccountDO::getMemberId, memberId);
    }

    default CpsRebateAccountDO selectForUpdateByMemberId(Long memberId) {
        return selectOne(new LambdaQueryWrapperX<CpsRebateAccountDO>()
                .eq(CpsRebateAccountDO::getMemberId, memberId)
                .last("FOR UPDATE"));
    }
}
