package com.qiji.cps.module.cps.dal.mysql.goods;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsPriceSnapshotPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsPriceSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CpsGoodsPriceSnapshotMapper extends BaseMapperX<CpsGoodsPriceSnapshotDO> {

    default PageResult<CpsGoodsPriceSnapshotDO> selectPage(CpsGoodsPriceSnapshotPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsGoodsPriceSnapshotDO>()
                .eqIfPresent(CpsGoodsPriceSnapshotDO::getMasterId, reqVO.getMasterId())
                .eqIfPresent(CpsGoodsPriceSnapshotDO::getSourceMappingId, reqVO.getSourceMappingId())
                .eqIfPresent(CpsGoodsPriceSnapshotDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsGoodsPriceSnapshotDO::getVendorCode, reqVO.getVendorCode())
                .betweenIfPresent(CpsGoodsPriceSnapshotDO::getSnapshotTime, reqVO.getSnapshotTime())
                .orderByDesc(CpsGoodsPriceSnapshotDO::getSnapshotTime)
                .orderByDesc(CpsGoodsPriceSnapshotDO::getId));
    }
}
