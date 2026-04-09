package cn.iocoder.yudao.module.cps.dal.mysql.vendor;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.cps.controller.admin.vendor.vo.CpsApiVendorPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.vendor.CpsApiVendorDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * CPS API 供应商配置 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsApiVendorMapper extends BaseMapperX<CpsApiVendorDO> {

    default PageResult<CpsApiVendorDO> selectPage(CpsApiVendorPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsApiVendorDO>()
                .eqIfPresent(CpsApiVendorDO::getVendorCode, reqVO.getVendorCode())
                .eqIfPresent(CpsApiVendorDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsApiVendorDO::getVendorType, reqVO.getVendorType())
                .eqIfPresent(CpsApiVendorDO::getStatus, reqVO.getStatus())
                .likeIfPresent(CpsApiVendorDO::getVendorName, reqVO.getVendorName())
                .orderByDesc(CpsApiVendorDO::getPriority)
                .orderByAsc(CpsApiVendorDO::getId));
    }

    default CpsApiVendorDO selectByVendorAndPlatform(String vendorCode, String platformCode) {
        return selectOne(new LambdaQueryWrapperX<CpsApiVendorDO>()
                .eq(CpsApiVendorDO::getVendorCode, vendorCode)
                .eq(CpsApiVendorDO::getPlatformCode, platformCode));
    }

    default List<CpsApiVendorDO> selectListByPlatformCode(String platformCode) {
        return selectList(new LambdaQueryWrapperX<CpsApiVendorDO>()
                .eq(CpsApiVendorDO::getPlatformCode, platformCode)
                .eq(CpsApiVendorDO::getStatus, 1)
                .orderByDesc(CpsApiVendorDO::getPriority));
    }

    default List<CpsApiVendorDO> selectListByVendorCode(String vendorCode) {
        return selectList(new LambdaQueryWrapperX<CpsApiVendorDO>()
                .eq(CpsApiVendorDO::getVendorCode, vendorCode)
                .eq(CpsApiVendorDO::getStatus, 1));
    }

    default List<CpsApiVendorDO> selectListByStatus(Integer status) {
        return selectList(CpsApiVendorDO::getStatus, status);
    }

}
