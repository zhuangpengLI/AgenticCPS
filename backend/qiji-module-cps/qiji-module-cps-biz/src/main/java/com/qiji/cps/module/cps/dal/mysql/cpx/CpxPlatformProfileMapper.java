package com.qiji.cps.module.cps.dal.mysql.cpx;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxPlatformProfileDO;
import com.qiji.cps.module.cps.service.cpx.CpxTaskConstants;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CpxPlatformProfileMapper extends BaseMapperX<CpxPlatformProfileDO> {

    default List<CpxPlatformProfileDO> selectAdminList() {
        return selectList(new LambdaQueryWrapperX<CpxPlatformProfileDO>()
                .orderByDesc(CpxPlatformProfileDO::getId));
    }

    default List<CpxPlatformProfileDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<CpxPlatformProfileDO>()
                .eq(CpxPlatformProfileDO::getStatus, CpxTaskConstants.STATUS_ONLINE)
                .orderByDesc(CpxPlatformProfileDO::getId));
    }
}
