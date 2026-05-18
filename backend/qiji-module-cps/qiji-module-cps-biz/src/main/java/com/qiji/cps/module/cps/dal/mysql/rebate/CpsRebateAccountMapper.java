package com.qiji.cps.module.cps.dal.mysql.rebate;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

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

    @Update("UPDATE cps_rebate_account SET available_balance = available_balance - #{amount}, "
            + "frozen_balance = frozen_balance + #{amount}, version = version + 1 "
            + "WHERE member_id = #{memberId} AND status = 1 AND available_balance >= #{amount}")
    int freezeBalance(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);

    @Update("UPDATE cps_rebate_account SET available_balance = available_balance + #{amount}, "
            + "frozen_balance = frozen_balance - #{amount}, version = version + 1 "
            + "WHERE member_id = #{memberId} AND frozen_balance >= #{amount}")
    int unfreezeBalance(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);

    @Update("UPDATE cps_rebate_account SET frozen_balance = frozen_balance - #{amount}, version = version + 1 "
            + "WHERE member_id = #{memberId} AND frozen_balance >= #{amount}")
    int deductFrozenBalance(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);

}
