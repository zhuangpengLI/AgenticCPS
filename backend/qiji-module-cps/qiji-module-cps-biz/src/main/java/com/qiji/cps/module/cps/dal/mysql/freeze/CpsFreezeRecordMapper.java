package com.qiji.cps.module.cps.dal.mysql.freeze;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.freeze.vo.CpsFreezeRecordPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.enums.CpsFreezeStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CPS冻结解冻记录 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsFreezeRecordMapper extends BaseMapperX<CpsFreezeRecordDO> {

    /**
     * 查询已到达解冻时间且状态为 frozen 的记录（批量自动解冻）
     */
    default List<CpsFreezeRecordDO> selectPendingUnfreeze(int limit) {
        return selectList(new LambdaQueryWrapperX<CpsFreezeRecordDO>()
                .eq(CpsFreezeRecordDO::getStatus, CpsFreezeStatusEnum.FROZEN.getStatus())
                .le(CpsFreezeRecordDO::getUnfreezeTime, LocalDateTime.now())
                .last("LIMIT " + limit));
    }

    /**
     * 分页查询冻结记录
     */
    default PageResult<CpsFreezeRecordDO> selectPage(CpsFreezeRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsFreezeRecordDO>()
                .eqIfPresent(CpsFreezeRecordDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(CpsFreezeRecordDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(CpsFreezeRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsFreezeRecordDO::getId));
    }

    default CpsFreezeRecordDO selectByBusinessAndIdempotencyKey(String businessType, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<CpsFreezeRecordDO>()
                .eq(CpsFreezeRecordDO::getBusinessType, businessType)
                .eq(CpsFreezeRecordDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    default CpsFreezeRecordDO selectByBusinessId(@Param("businessType") String businessType,
                                                @Param("businessId") String businessId) {
        return selectOne(new LambdaQueryWrapperX<CpsFreezeRecordDO>()
                .eq(CpsFreezeRecordDO::getBusinessType, businessType)
                .eq(CpsFreezeRecordDO::getBusinessId, businessId)
                .last("LIMIT 1"));
    }

}
