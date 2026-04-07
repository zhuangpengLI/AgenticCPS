package cn.iocoder.yudao.module.cps.dal.mysql.platform;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.cps.controller.admin.platform.vo.CpsPlatformPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.platform.CpsPlatformDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * CPS平台配置 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsPlatformMapper extends BaseMapperX<CpsPlatformDO> {

    default PageResult<CpsPlatformDO> selectPage(CpsPlatformPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsPlatformDO>()
                .likeIfPresent(CpsPlatformDO::getPlatformName, reqVO.getPlatformName())
                .eqIfPresent(CpsPlatformDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsPlatformDO::getStatus, reqVO.getStatus())
                .orderByAsc(CpsPlatformDO::getSort));
    }

    default CpsPlatformDO selectByPlatformCode(String platformCode) {
        return selectOne(CpsPlatformDO::getPlatformCode, platformCode);
    }

    default List<CpsPlatformDO> selectListByStatus(Integer status) {
        return selectList(CpsPlatformDO::getStatus, status);
    }

}
