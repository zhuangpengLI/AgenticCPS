package cn.iocoder.yudao.module.cps.dal.mysql.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.order.CpsOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CPS订单 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsOrderMapper extends BaseMapperX<CpsOrderDO> {

    default PageResult<CpsOrderDO> selectPage(CpsOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsOrderDO>()
                .eqIfPresent(CpsOrderDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsOrderDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(CpsOrderDO::getOrderStatus, reqVO.getOrderStatus())
                .likeIfPresent(CpsOrderDO::getItemTitle, reqVO.getItemTitle())
                .likeIfPresent(CpsOrderDO::getPlatformOrderId, reqVO.getPlatformOrderId())
                .betweenIfPresent(CpsOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsOrderDO::getId));
    }

    default CpsOrderDO selectByPlatformOrderId(String platformOrderId) {
        return selectOne(CpsOrderDO::getPlatformOrderId, platformOrderId);
    }

}
