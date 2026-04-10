package com.qiji.cps.module.cps.dal.mysql.rebate;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateRecordPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CPS返利记录 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsRebateRecordMapper extends BaseMapperX<CpsRebateRecordDO> {

    default PageResult<CpsRebateRecordDO> selectPage(CpsRebateRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsRebateRecordDO>()
                .eqIfPresent(CpsRebateRecordDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(CpsRebateRecordDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsRebateRecordDO::getRebateType, reqVO.getRebateType())
                .eqIfPresent(CpsRebateRecordDO::getRebateStatus, reqVO.getRebateStatus())
                .betweenIfPresent(CpsRebateRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsRebateRecordDO::getId));
    }

    /**
     * 按订单ID查询返利记录（判断是否已结算过）
     */
    default CpsRebateRecordDO selectByOrderIdAndType(Long orderId, String rebateType) {
        return selectOne(new LambdaQueryWrapperX<CpsRebateRecordDO>()
                .eq(CpsRebateRecordDO::getOrderId, orderId)
                .eq(CpsRebateRecordDO::getRebateType, rebateType)
                .last("LIMIT 1"));
    }

}
