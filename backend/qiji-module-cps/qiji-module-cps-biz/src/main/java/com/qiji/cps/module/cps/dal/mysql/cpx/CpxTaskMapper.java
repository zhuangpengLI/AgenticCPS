package com.qiji.cps.module.cps.dal.mysql.cpx;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTaskDO;
import com.qiji.cps.module.cps.service.cpx.CpxTaskConstants;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CpxTaskMapper extends BaseMapperX<CpxTaskDO> {

    default List<CpxTaskDO> selectPublishedList(String keyword, String promotionMethod) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapperX<CpxTaskDO> wrapper = new LambdaQueryWrapperX<CpxTaskDO>();
        wrapper.eq(CpxTaskDO::getStatus, CpxTaskConstants.STATUS_ONLINE)
                .eqIfPresent(CpxTaskDO::getPromotionMethod, promotionMethod)
                .and(w -> w.isNull(CpxTaskDO::getStartTime).or().le(CpxTaskDO::getStartTime, now))
                .and(w -> w.isNull(CpxTaskDO::getEndTime).or().ge(CpxTaskDO::getEndTime, now))
                .orderByAsc(CpxTaskDO::getPriority)
                .orderByDesc(CpxTaskDO::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CpxTaskDO::getTaskName, keyword)
                    .or().like(CpxTaskDO::getTitle, keyword)
                    .or().like(CpxTaskDO::getShortDesc, keyword)
                    .or().like(CpxTaskDO::getTags, keyword));
        }
        return selectList(wrapper);
    }

    default List<CpxTaskDO> selectAdminList(String keyword, String promotionMethod) {
        LambdaQueryWrapperX<CpxTaskDO> wrapper = new LambdaQueryWrapperX<CpxTaskDO>();
        wrapper.eqIfPresent(CpxTaskDO::getPromotionMethod, promotionMethod)
                .orderByAsc(CpxTaskDO::getPriority)
                .orderByDesc(CpxTaskDO::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CpxTaskDO::getTaskName, keyword)
                    .or().like(CpxTaskDO::getTitle, keyword)
                    .or().like(CpxTaskDO::getShortDesc, keyword)
                    .or().like(CpxTaskDO::getTags, keyword));
        }
        return selectList(wrapper);
    }
}
