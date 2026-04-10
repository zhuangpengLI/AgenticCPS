package com.qiji.cps.module.cps.dal.mysql.withdraw;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.withdraw.vo.CpsWithdrawPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
import org.apache.ibatis.annotations.Mapper;

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

}
