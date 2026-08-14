package com.qiji.cps.module.cps.dal.mysql.didi;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.didi.CpsDidiCallbackEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CpsDidiCallbackEventMapper extends BaseMapperX<CpsDidiCallbackEventDO> {
    default CpsDidiCallbackEventDO selectByIdempotencyKey(String key) {
        return selectOne(new LambdaQueryWrapperX<CpsDidiCallbackEventDO>()
                .eq(CpsDidiCallbackEventDO::getIdempotencyKey, key));
    }
}
