package cn.iocoder.yudao.module.cps.dal.mysql.adzone;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.cps.controller.admin.adzone.vo.CpsAdzonePageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
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
