package com.qiji.cps.module.cps.dal.mysql.adzone;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzonePageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * CPS推广位 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsAdzoneMapper extends BaseMapperX<CpsAdzoneDO> {

    default PageResult<CpsAdzoneDO> selectPage(CpsAdzonePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsAdzoneDO>()
                .eqIfPresent(CpsAdzoneDO::getPlatformCode, reqVO.getPlatformCode())
                .likeIfPresent(CpsAdzoneDO::getAdzoneName, reqVO.getAdzoneName())
                .eqIfPresent(CpsAdzoneDO::getAdzoneType, reqVO.getAdzoneType())
                .eqIfPresent(CpsAdzoneDO::getStatus, reqVO.getStatus())
                .orderByDesc(CpsAdzoneDO::getId));
    }

    default CpsAdzoneDO selectDefaultByPlatformCode(String platformCode) {
        return selectOne(new LambdaQueryWrapperX<CpsAdzoneDO>()
                .eq(CpsAdzoneDO::getPlatformCode, platformCode)
                .eq(CpsAdzoneDO::getIsDefault, 1)
                .eq(CpsAdzoneDO::getStatus, 1));
    }

    default List<CpsAdzoneDO> selectListByPlatformCode(String platformCode) {
        return selectList(new LambdaQueryWrapperX<CpsAdzoneDO>()
                .eq(CpsAdzoneDO::getPlatformCode, platformCode)
                .eq(CpsAdzoneDO::getStatus, 1));
    }

}
