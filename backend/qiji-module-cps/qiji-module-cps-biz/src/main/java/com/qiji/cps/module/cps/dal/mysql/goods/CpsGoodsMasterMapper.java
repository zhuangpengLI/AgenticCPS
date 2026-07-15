package com.qiji.cps.module.cps.dal.mysql.goods;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsMasterPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsMasterDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CpsGoodsMasterMapper extends BaseMapperX<CpsGoodsMasterDO> {

    default PageResult<CpsGoodsMasterDO> selectPage(CpsGoodsMasterPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsGoodsMasterDO>()
                .likeIfPresent(CpsGoodsMasterDO::getStandardTitle, reqVO.getKeyword())
                .eqIfPresent(CpsGoodsMasterDO::getBrandName, reqVO.getBrandName())
                .eqIfPresent(CpsGoodsMasterDO::getCategoryName, reqVO.getCategoryName())
                .eqIfPresent(CpsGoodsMasterDO::getStatus, reqVO.getStatus())
                .orderByDesc(CpsGoodsMasterDO::getId));
    }
}
