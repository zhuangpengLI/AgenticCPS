package com.qiji.cps.module.cps.dal.mysql.cpx;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTrackingLinkDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CpxTrackingLinkMapper extends BaseMapperX<CpxTrackingLinkDO> {

    default CpxTrackingLinkDO selectByTrackingId(String trackingId) {
        return selectOne(new LambdaQueryWrapperX<CpxTrackingLinkDO>()
                .eq(CpxTrackingLinkDO::getTrackingId, trackingId));
    }
}
