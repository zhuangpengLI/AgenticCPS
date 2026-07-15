package com.qiji.cps.module.cps.dal.mysql.rebate;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateAssetLedgerPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import org.apache.ibatis.annotations.Mapper;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;

import java.util.Collections;
import java.util.List;

/**
 * 资产流水 Mapper。调用方只能使用 insert 和只读查询，禁止更新或删除历史流水。
 */
@Mapper
public interface CpsRebateAssetLedgerMapper extends BaseMapperX<CpsRebateAssetLedgerDO> {

    default PageResult<CpsRebateAssetLedgerDO> selectPage(CpsRebateAssetLedgerPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsRebateAssetLedgerDO>()
                .eqIfPresent(CpsRebateAssetLedgerDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(CpsRebateAssetLedgerDO::getBusinessType, reqVO.getBusinessType())
                .eqIfPresent(CpsRebateAssetLedgerDO::getBusinessId, reqVO.getBusinessId())
                .eqIfPresent(CpsRebateAssetLedgerDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(CpsRebateAssetLedgerDO::getIdempotencyKey, reqVO.getIdempotencyKey())
                .betweenIfPresent(CpsRebateAssetLedgerDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsRebateAssetLedgerDO::getId));
    }

    default PageResult<CpsRebateAssetLedgerDO> selectDebtRepaymentPage(Long memberId,
                                                                        com.qiji.cps.framework.common.pojo.PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<CpsRebateAssetLedgerDO>()
                .eq(CpsRebateAssetLedgerDO::getMemberId, memberId)
                .lt(CpsRebateAssetLedgerDO::getDebtChangeCent, 0L)
                .orderByDesc(CpsRebateAssetLedgerDO::getId));
    }

    default CpsRebateAssetLedgerDO selectByBusinessAndIdempotencyKey(String businessType, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<CpsRebateAssetLedgerDO>()
                .eq(CpsRebateAssetLedgerDO::getBusinessType, businessType)
                .eq(CpsRebateAssetLedgerDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    default CpsRebateAssetLedgerDO selectOpeningBalanceByAccountId(Long accountId) {
        return selectOne(new LambdaQueryWrapperX<CpsRebateAssetLedgerDO>()
                .eq(CpsRebateAssetLedgerDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(CpsRebateAssetLedgerDO::getBusinessType, "OPENING_BALANCE")
                .eq(CpsRebateAssetLedgerDO::getBusinessId, String.valueOf(accountId))
                .last("LIMIT 1"));
    }

    default List<CpsRebateAssetLedgerDO> selectListByTrace(Long orderId, String platformOrderId,
                                                           String businessId, String idempotencyKey) {
        if (orderId == null && isBlank(platformOrderId) && isBlank(businessId) && isBlank(idempotencyKey)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<CpsRebateAssetLedgerDO>()
                .eqIfPresent(CpsRebateAssetLedgerDO::getOrderId, orderId)
                .eqIfPresent(CpsRebateAssetLedgerDO::getPlatformOrderId, platformOrderId)
                .eqIfPresent(CpsRebateAssetLedgerDO::getBusinessId, businessId)
                .eqIfPresent(CpsRebateAssetLedgerDO::getIdempotencyKey, idempotencyKey)
                .orderByDesc(CpsRebateAssetLedgerDO::getId));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
