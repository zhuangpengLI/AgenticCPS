package com.qiji.cps.module.cps.dal.mysql.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsPlatformBillDiffPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsPlatformBillDiffDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

@Mapper
public interface CpsPlatformBillDiffMapper extends BaseMapperX<CpsPlatformBillDiffDO> {

    default CpsPlatformBillDiffDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<CpsPlatformBillDiffDO>()
                .eq(CpsPlatformBillDiffDO::getIdempotencyKey, idempotencyKey));
    }

    default PageResult<CpsPlatformBillDiffDO> selectPage(CpsPlatformBillDiffPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsPlatformBillDiffDO>()
                .eqIfPresent(CpsPlatformBillDiffDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsPlatformBillDiffDO::getVendorCode, reqVO.getVendorCode())
                .eqIfPresent(CpsPlatformBillDiffDO::getBillBatchNo, reqVO.getBillBatchNo())
                .eqIfPresent(CpsPlatformBillDiffDO::getPlatformOrderId, reqVO.getPlatformOrderId())
                .eqIfPresent(CpsPlatformBillDiffDO::getDiffType, reqVO.getDiffType())
                .eqIfPresent(CpsPlatformBillDiffDO::getDiffStatus, reqVO.getDiffStatus())
                .betweenIfPresent(CpsPlatformBillDiffDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsPlatformBillDiffDO::getId));
    }

    default List<CpsPlatformBillDiffDO> selectListByTrace(Long orderId, String platformOrderId, String idempotencyKey) {
        if (orderId == null && isBlank(platformOrderId) && isBlank(idempotencyKey)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<CpsPlatformBillDiffDO>()
                .eqIfPresent(CpsPlatformBillDiffDO::getOrderId, orderId)
                .eqIfPresent(CpsPlatformBillDiffDO::getPlatformOrderId, platformOrderId)
                .eqIfPresent(CpsPlatformBillDiffDO::getIdempotencyKey, idempotencyKey)
                .orderByDesc(CpsPlatformBillDiffDO::getId));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
