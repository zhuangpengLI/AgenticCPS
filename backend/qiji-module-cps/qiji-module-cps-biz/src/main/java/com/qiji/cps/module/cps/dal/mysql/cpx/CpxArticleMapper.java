package com.qiji.cps.module.cps.dal.mysql.cpx;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxArticleDO;
import com.qiji.cps.module.cps.service.cpx.CpxTaskConstants;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

@Mapper
public interface CpxArticleMapper extends BaseMapperX<CpxArticleDO> {

    default List<CpxArticleDO> selectPublishedList(String keyword, String category, String promotionMethod) {
        LambdaQueryWrapperX<CpxArticleDO> wrapper = new LambdaQueryWrapperX<CpxArticleDO>();
        wrapper.eq(CpxArticleDO::getStatus, CpxTaskConstants.STATUS_ONLINE)
                .eqIfPresent(CpxArticleDO::getCategory, category)
                .eqIfPresent(CpxArticleDO::getPromotionMethod, promotionMethod)
                .orderByDesc(CpxArticleDO::getPublishTime)
                .orderByDesc(CpxArticleDO::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CpxArticleDO::getTitle, keyword)
                    .or().like(CpxArticleDO::getSummary, keyword)
                    .or().like(CpxArticleDO::getTags, keyword));
        }
        return selectList(wrapper);
    }

    default List<CpxArticleDO> selectAdminList(String keyword, String category, String promotionMethod) {
        LambdaQueryWrapperX<CpxArticleDO> wrapper = new LambdaQueryWrapperX<CpxArticleDO>();
        wrapper.eqIfPresent(CpxArticleDO::getCategory, category)
                .eqIfPresent(CpxArticleDO::getPromotionMethod, promotionMethod)
                .orderByDesc(CpxArticleDO::getPublishTime)
                .orderByDesc(CpxArticleDO::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CpxArticleDO::getTitle, keyword)
                    .or().like(CpxArticleDO::getSummary, keyword)
                    .or().like(CpxArticleDO::getTags, keyword));
        }
        return selectList(wrapper);
    }
}
