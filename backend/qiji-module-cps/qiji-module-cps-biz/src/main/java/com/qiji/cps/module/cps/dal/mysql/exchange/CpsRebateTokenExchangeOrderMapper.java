package com.qiji.cps.module.cps.dal.mysql.exchange;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CpsRebateTokenExchangeOrderMapper extends BaseMapperX<CpsRebateTokenExchangeOrderDO> {

    default CpsRebateTokenExchangeOrderDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<CpsRebateTokenExchangeOrderDO>()
                .eq(CpsRebateTokenExchangeOrderDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    default CpsRebateTokenExchangeOrderDO selectByExchangeOrderNo(String exchangeOrderNo) {
        return selectOne(new LambdaQueryWrapperX<CpsRebateTokenExchangeOrderDO>()
                .eq(CpsRebateTokenExchangeOrderDO::getExchangeOrderNo, exchangeOrderNo)
                .last("LIMIT 1"));
    }

    default CpsRebateTokenExchangeOrderDO selectByMemberIdAndExchangeOrderNo(Long memberId, String exchangeOrderNo) {
        return selectOne(new LambdaQueryWrapperX<CpsRebateTokenExchangeOrderDO>()
                .eq(CpsRebateTokenExchangeOrderDO::getMemberId, memberId)
                .eq(CpsRebateTokenExchangeOrderDO::getExchangeOrderNo, exchangeOrderNo)
                .last("LIMIT 1"));
    }

    default List<Long> selectDueCompensationIds(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapperX<CpsRebateTokenExchangeOrderDO>()
                .in(CpsRebateTokenExchangeOrderDO::getStatus, "PROCESSING", "CREDITED", "ROLLBACK_REQUIRED")
                .and(wrapper -> wrapper.isNull(CpsRebateTokenExchangeOrderDO::getNextRetryTime)
                        .or().le(CpsRebateTokenExchangeOrderDO::getNextRetryTime, now))
                .orderByAsc(CpsRebateTokenExchangeOrderDO::getCreateTime)
                .last("LIMIT " + Math.max(1, limit)))
                .stream().map(CpsRebateTokenExchangeOrderDO::getId).toList();
    }

    /**
     * CAS 更新兑换状态：必须同时匹配状态版本和允许的源状态，避免陈旧任务覆盖新状态或终态降级。
     */
    default int updateByIdAndStatusVersion(CpsRebateTokenExchangeOrderDO updateDO,
                                           Integer expectedStatusVersion,
                                           List<String> allowedSourceStatuses) {
        int currentVersion = expectedStatusVersion == null ? 0 : expectedStatusVersion;
        if (allowedSourceStatuses == null || allowedSourceStatuses.isEmpty()) {
            throw new IllegalArgumentException("allowed source statuses must not be empty");
        }
        LambdaUpdateWrapper<CpsRebateTokenExchangeOrderDO> wrapper =
                new LambdaUpdateWrapper<CpsRebateTokenExchangeOrderDO>()
                        .eq(CpsRebateTokenExchangeOrderDO::getId, updateDO.getId())
                        .eq(CpsRebateTokenExchangeOrderDO::getStatusVersion, currentVersion)
                        .in(CpsRebateTokenExchangeOrderDO::getStatus, allowedSourceStatuses)
                        .set(CpsRebateTokenExchangeOrderDO::getStatusVersion, currentVersion + 1);
        return update(updateDO, wrapper);
    }
}
