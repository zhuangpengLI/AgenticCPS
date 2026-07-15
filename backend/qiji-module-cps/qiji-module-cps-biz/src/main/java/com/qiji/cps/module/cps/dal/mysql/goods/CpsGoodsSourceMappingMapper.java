package com.qiji.cps.module.cps.dal.mysql.goods;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsSourceMappingPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsSourceMappingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CpsGoodsSourceMappingMapper extends BaseMapperX<CpsGoodsSourceMappingDO> {

    default CpsGoodsSourceMappingDO selectBySourceKey(String platformCode, String vendorCode,
                                                     String externalGoodsId, String goodsSign) {
        return selectOne(new LambdaQueryWrapperX<CpsGoodsSourceMappingDO>()
                .eq(CpsGoodsSourceMappingDO::getPlatformCode, platformCode)
                .eqIfPresent(CpsGoodsSourceMappingDO::getVendorCode, vendorCode)
                .eq(CpsGoodsSourceMappingDO::getExternalGoodsId, externalGoodsId)
                .eqIfPresent(CpsGoodsSourceMappingDO::getGoodsSign, goodsSign)
                .last("LIMIT 1"));
    }

    default PageResult<CpsGoodsSourceMappingDO> selectPage(CpsGoodsSourceMappingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsGoodsSourceMappingDO>()
                .eqIfPresent(CpsGoodsSourceMappingDO::getMasterId, reqVO.getMasterId())
                .eqIfPresent(CpsGoodsSourceMappingDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsGoodsSourceMappingDO::getVendorCode, reqVO.getVendorCode())
                .eqIfPresent(CpsGoodsSourceMappingDO::getExternalGoodsId, reqVO.getExternalGoodsId())
                .eqIfPresent(CpsGoodsSourceMappingDO::getStatus, reqVO.getStatus())
                .orderByDesc(CpsGoodsSourceMappingDO::getLastSnapshotTime)
                .orderByDesc(CpsGoodsSourceMappingDO::getId));
    }
}
