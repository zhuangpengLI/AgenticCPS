package cn.iocoder.yudao.module.cps.dal.mysql.withdraw;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.cps.controller.admin.withdraw.vo.CpsWithdrawPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
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
