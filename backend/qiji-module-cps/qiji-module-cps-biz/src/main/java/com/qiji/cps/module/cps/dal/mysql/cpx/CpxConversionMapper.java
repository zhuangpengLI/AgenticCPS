package com.qiji.cps.module.cps.dal.mysql.cpx;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxConversionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CpxConversionMapper extends BaseMapperX<CpxConversionDO> {

    default List<CpxConversionDO> selectListByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapperX<CpxConversionDO>()
                .eq(CpxConversionDO::getMemberId, memberId)
                .orderByDesc(CpxConversionDO::getId));
    }
}
