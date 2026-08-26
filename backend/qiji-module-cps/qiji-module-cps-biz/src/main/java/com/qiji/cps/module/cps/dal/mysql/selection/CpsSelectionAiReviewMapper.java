package com.qiji.cps.module.cps.dal.mysql.selection;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionAiReviewDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CpsSelectionAiReviewMapper extends BaseMapperX<CpsSelectionAiReviewDO> {

    default List<CpsSelectionAiReviewDO> selectListByContextId(String reviewContextId, Long ownerUserId) {
        return selectList(new LambdaQueryWrapperX<CpsSelectionAiReviewDO>()
                .eq(CpsSelectionAiReviewDO::getReviewContextId, reviewContextId)
                .eq(CpsSelectionAiReviewDO::getOwnerUserId, ownerUserId)
                .orderByAsc(CpsSelectionAiReviewDO::getId));
    }

    default CpsSelectionAiReviewDO selectOneByUnique(String reviewContextId, Long ownerUserId, String platformCode,
                                                     String vendorCode, String goodsId, String goodsSign) {
        return selectOne(new LambdaQueryWrapperX<CpsSelectionAiReviewDO>()
                .eq(CpsSelectionAiReviewDO::getReviewContextId, reviewContextId)
                .eq(CpsSelectionAiReviewDO::getOwnerUserId, ownerUserId)
                .eq(CpsSelectionAiReviewDO::getPlatformCode, platformCode)
                .eq(CpsSelectionAiReviewDO::getVendorCode, vendorCode)
                .eq(CpsSelectionAiReviewDO::getGoodsId, goodsId)
                .eq(CpsSelectionAiReviewDO::getGoodsSign, goodsSign));
    }

    default CpsSelectionAiReviewDO selectOneByUniqueForUpdate(String reviewContextId, Long ownerUserId,
                                                              String platformCode, String vendorCode,
                                                              String goodsId, String goodsSign) {
        return selectOne(new LambdaQueryWrapperX<CpsSelectionAiReviewDO>()
                .eq(CpsSelectionAiReviewDO::getReviewContextId, reviewContextId)
                .eq(CpsSelectionAiReviewDO::getOwnerUserId, ownerUserId)
                .eq(CpsSelectionAiReviewDO::getPlatformCode, platformCode)
                .eq(CpsSelectionAiReviewDO::getVendorCode, vendorCode)
                .eq(CpsSelectionAiReviewDO::getGoodsId, goodsId)
                .eq(CpsSelectionAiReviewDO::getGoodsSign, goodsSign)
                .last("FOR UPDATE"));
    }
}
