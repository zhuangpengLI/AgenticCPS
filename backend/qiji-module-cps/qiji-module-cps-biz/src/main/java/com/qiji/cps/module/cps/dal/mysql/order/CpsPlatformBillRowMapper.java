package com.qiji.cps.module.cps.dal.mysql.order;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsPlatformBillRowDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CpsPlatformBillRowMapper extends BaseMapperX<CpsPlatformBillRowDO> {

    default CpsPlatformBillRowDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<CpsPlatformBillRowDO>()
                .eq(CpsPlatformBillRowDO::getIdempotencyKey, idempotencyKey));
    }
}
