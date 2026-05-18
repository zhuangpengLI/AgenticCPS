package com.qiji.cps.module.cps.dal.mysql.exchange;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CpsRebateTokenExchangeOrderMapper extends BaseMapperX<CpsRebateTokenExchangeOrderDO> {

    default CpsRebateTokenExchangeOrderDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<CpsRebateTokenExchangeOrderDO>()
                .eq(CpsRebateTokenExchangeOrderDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    default CpsRebateTokenExchangeOrderDO selectByExchangeOrderNo(String exchangeOrderNo) {
        return selectOne(new LambdaQueryWrapperX<CpsRebateTokenExchangeOrderDO>()
                .eq(CpsRebateTokenExchangeOrderDO::getExchangeOrderNo, exchangeOrderNo)
                .last("LIMIT 1"));
    }
}
