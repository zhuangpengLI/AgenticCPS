package com.qiji.cps.module.cps.dal.mysql.selection;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemePageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.service.selection.CpsSelectionConstants;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CpsSelectionThemeMapper extends BaseMapperX<CpsSelectionThemeDO> {

    default PageResult<CpsSelectionThemeDO> selectPage(CpsSelectionThemePageReqVO reqVO) {
        return selectPage(reqVO, buildPageFilter(reqVO, null)
                .orderByAsc(CpsSelectionThemeDO::getSort)
                .orderByDesc(CpsSelectionThemeDO::getId));
    }

    default Long countByStatus(CpsSelectionThemePageReqVO reqVO, String status) {
        if (StringUtils.hasText(status) && StringUtils.hasText(reqVO.getStatus())
                && !status.equals(reqVO.getStatus())) {
            return 0L;
        }
        return selectCount(buildPageFilter(reqVO, status));
    }

    private LambdaQueryWrapperX<CpsSelectionThemeDO> buildPageFilter(CpsSelectionThemePageReqVO reqVO, String status) {
        return new LambdaQueryWrapperX<CpsSelectionThemeDO>()
                .likeIfPresent(CpsSelectionThemeDO::getThemeName, reqVO.getThemeName())
                .eqIfPresent(CpsSelectionThemeDO::getThemeCode, reqVO.getThemeCode())
                .eqIfPresent(CpsSelectionThemeDO::getThemeType, reqVO.getThemeType())
                .eqIfPresent(CpsSelectionThemeDO::getPromotionEvent, reqVO.getPromotionEvent())
                .eqIfPresent(CpsSelectionThemeDO::getStatus, StringUtils.hasText(status) ? status : reqVO.getStatus())
                .eqIfPresent(CpsSelectionThemeDO::getGoodsSquareVisible, reqVO.getGoodsSquareVisible())
                .likeIfPresent(CpsSelectionThemeDO::getPlatformCodes, reqVO.getPlatformCode())
                .eqIfPresent(CpsSelectionThemeDO::getVendorCode, reqVO.getVendorCode());
    }

    default CpsSelectionThemeDO selectByThemeCode(String themeCode) {
        return selectOne(CpsSelectionThemeDO::getThemeCode, themeCode);
    }

    default CpsSelectionThemeDO selectPublishedGoodsSquareByThemeCode(String themeCode) {
        LocalDateTime now = LocalDateTime.now();
        return selectOne(new LambdaQueryWrapperX<CpsSelectionThemeDO>()
                .eq(CpsSelectionThemeDO::getThemeCode, themeCode)
                .eq(CpsSelectionThemeDO::getStatus, CpsSelectionConstants.ThemeStatus.PUBLISHED)
                .eq(CpsSelectionThemeDO::getGoodsSquareVisible, 1)
                .and(w -> w.isNull(CpsSelectionThemeDO::getStartTime).or().le(CpsSelectionThemeDO::getStartTime, now))
                .and(w -> w.isNull(CpsSelectionThemeDO::getEndTime).or().ge(CpsSelectionThemeDO::getEndTime, now)));
    }

    default List<CpsSelectionThemeDO> selectPublishedList(String keyword, String promotionEvent) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapperX<CpsSelectionThemeDO> wrapper = new LambdaQueryWrapperX<CpsSelectionThemeDO>();
        wrapper.eq(CpsSelectionThemeDO::getStatus, CpsSelectionConstants.ThemeStatus.PUBLISHED);
        wrapper.eqIfPresent(CpsSelectionThemeDO::getPromotionEvent, promotionEvent);
        wrapper.and(w -> w.isNull(CpsSelectionThemeDO::getStartTime).or().le(CpsSelectionThemeDO::getStartTime, now));
        wrapper.and(w -> w.isNull(CpsSelectionThemeDO::getEndTime).or().ge(CpsSelectionThemeDO::getEndTime, now));
        wrapper.orderByAsc(CpsSelectionThemeDO::getSort);
        wrapper.orderByDesc(CpsSelectionThemeDO::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CpsSelectionThemeDO::getThemeName, keyword)
                    .or().like(CpsSelectionThemeDO::getThemeCode, keyword)
                    .or().like(CpsSelectionThemeDO::getTags, keyword));
        }
        return selectList(wrapper);
    }
}
