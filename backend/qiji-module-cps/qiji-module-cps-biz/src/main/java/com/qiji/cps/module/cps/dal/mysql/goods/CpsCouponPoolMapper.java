package com.qiji.cps.module.cps.dal.mysql.goods;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsCouponPoolDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CpsCouponPoolMapper extends BaseMapperX<CpsCouponPoolDO> {

    default PageResult<CpsCouponPoolDO> selectPage(CpsCouponPoolPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsCouponPoolDO>()
                .eqIfPresent(CpsCouponPoolDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsCouponPoolDO::getVendorCode, reqVO.getVendorCode())
                .eqIfPresent(CpsCouponPoolDO::getExternalGoodsId, reqVO.getExternalGoodsId())
                .eqIfPresent(CpsCouponPoolDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CpsCouponPoolDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(CpsCouponPoolDO::getActivityId, reqVO.getActivityId())
                .eqIfPresent(CpsCouponPoolDO::getThemeId, reqVO.getThemeId())
                .orderByDesc(CpsCouponPoolDO::getLastSyncTime)
                .orderByDesc(CpsCouponPoolDO::getId));
    }

    default List<CpsCouponPoolDO> selectListByGoods(String platformCode, String vendorCode,
                                                   String externalGoodsId, String goodsSign) {
        return selectList(new LambdaQueryWrapperX<CpsCouponPoolDO>()
                .eq(CpsCouponPoolDO::getPlatformCode, platformCode)
                .eqIfPresent(CpsCouponPoolDO::getVendorCode, vendorCode)
                .eq(CpsCouponPoolDO::getExternalGoodsId, externalGoodsId)
                .eqIfPresent(CpsCouponPoolDO::getGoodsSign, goodsSign)
                .orderByDesc(CpsCouponPoolDO::getCouponAmount)
                .orderByDesc(CpsCouponPoolDO::getId));
    }
}
