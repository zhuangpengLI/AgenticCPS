package com.qiji.cps.module.cps.dal.mysql.withdraw;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.withdraw.vo.CpsWithdrawPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CPS提现申请 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsWithdrawMapper extends BaseMapperX<CpsWithdrawDO> {

    default PageResult<CpsWithdrawDO> selectPage(CpsWithdrawPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsWithdrawDO>()
                .eqIfPresent(CpsWithdrawDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(CpsWithdrawDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CpsWithdrawDO::getWithdrawType, reqVO.getWithdrawType())
                .betweenIfPresent(CpsWithdrawDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsWithdrawDO::getId));
    }

    default CpsWithdrawDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<CpsWithdrawDO>()
                .eq(CpsWithdrawDO::getIdempotencyKey, idempotencyKey).last("LIMIT 1"));
    }

    default PageResult<CpsWithdrawDO> selectMemberPage(Long memberId, int pageNo, int pageSize) {
        com.qiji.cps.framework.common.pojo.PageParam page = new com.qiji.cps.framework.common.pojo.PageParam();
        page.setPageNo(pageNo);
        page.setPageSize(pageSize);
        return selectPage(page, new LambdaQueryWrapperX<CpsWithdrawDO>()
                .eq(CpsWithdrawDO::getMemberId, memberId).orderByDesc(CpsWithdrawDO::getId));
    }

    default CpsWithdrawDO selectByMemberIdAndId(Long memberId, Long id) {
        return selectOne(new LambdaQueryWrapperX<CpsWithdrawDO>()
                .eq(CpsWithdrawDO::getMemberId, memberId).eq(CpsWithdrawDO::getId, id).last("LIMIT 1"));
    }

    default List<Long> selectDueCompensationIds(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapperX<CpsWithdrawDO>()
                .eq(CpsWithdrawDO::getStatus, "reviewing")
                .and(w -> w.isNull(CpsWithdrawDO::getNextRetryTime).or().le(CpsWithdrawDO::getNextRetryTime, now))
                .orderByAsc(CpsWithdrawDO::getCreateTime).last("LIMIT " + Math.max(1, limit)))
                .stream().map(CpsWithdrawDO::getId).toList();
    }

    default int updateByIdAndStatusVersion(CpsWithdrawDO updateDO, Integer expectedVersion,
                                            List<String> allowedStatuses) {
        int version = expectedVersion == null ? 0 : expectedVersion;
        LambdaUpdateWrapper<CpsWithdrawDO> wrapper = new LambdaUpdateWrapper<CpsWithdrawDO>()
                .eq(CpsWithdrawDO::getId, updateDO.getId())
                .eq(CpsWithdrawDO::getStatusVersion, version)
                .in(CpsWithdrawDO::getStatus, allowedStatuses)
                .set(CpsWithdrawDO::getStatusVersion, version + 1);
        if ("success".equals(updateDO.getStatus()) || "failed".equals(updateDO.getStatus())
                || "rejected".equals(updateDO.getStatus())) {
            wrapper.set(CpsWithdrawDO::getNextRetryTime, null);
        }
        if ("success".equals(updateDO.getStatus())) {
            wrapper.set(CpsWithdrawDO::getTransferError, null);
        }
        return update(updateDO, wrapper);
    }

}
