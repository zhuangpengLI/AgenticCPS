package com.qiji.cps.module.cps.dal.mysql.cpx;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CpxEventMapper extends BaseMapperX<CpxEventDO> {

    default CpxEventDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<CpxEventDO>()
                .eq(CpxEventDO::getIdempotencyKey, idempotencyKey));
    }
}
