package com.qiji.cps.module.cps.dal.mysql.selection;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.service.selection.CpsSelectionConstants;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CpsSelectionThemeItemMapper extends BaseMapperX<CpsSelectionThemeItemDO> {

    default List<CpsSelectionThemeItemDO> selectListByThemeId(Long themeId) {
        return selectList(new LambdaQueryWrapperX<CpsSelectionThemeItemDO>()
                .eq(CpsSelectionThemeItemDO::getThemeId, themeId)
                .orderByDesc(CpsSelectionThemeItemDO::getTopFlag)
                .orderByAsc(CpsSelectionThemeItemDO::getSort)
                .orderByDesc(CpsSelectionThemeItemDO::getRecommendScore)
                .orderByDesc(CpsSelectionThemeItemDO::getId));
    }

    default List<CpsSelectionThemeItemDO> selectEnabledListByThemeId(Long themeId) {
        return selectList(new LambdaQueryWrapperX<CpsSelectionThemeItemDO>()
                .eq(CpsSelectionThemeItemDO::getThemeId, themeId)
                .eq(CpsSelectionThemeItemDO::getStatus, CpsSelectionConstants.ItemStatus.ENABLED)
                .orderByDesc(CpsSelectionThemeItemDO::getTopFlag)
                .orderByAsc(CpsSelectionThemeItemDO::getSort)
                .orderByDesc(CpsSelectionThemeItemDO::getRecommendScore)
                .orderByDesc(CpsSelectionThemeItemDO::getId));
    }

    default CpsSelectionThemeItemDO selectOneByUnique(Long themeId, String platformCode, String vendorCode,
                                                     String goodsId, String goodsSign) {
        return selectOne(new LambdaQueryWrapperX<CpsSelectionThemeItemDO>()
                .eq(CpsSelectionThemeItemDO::getThemeId, themeId)
                .eq(CpsSelectionThemeItemDO::getPlatformCode, platformCode)
                .eqIfPresent(CpsSelectionThemeItemDO::getVendorCode, vendorCode)
                .eq(CpsSelectionThemeItemDO::getGoodsId, goodsId)
                .eqIfPresent(CpsSelectionThemeItemDO::getGoodsSign, goodsSign));
    }
}
