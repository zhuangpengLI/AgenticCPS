package com.qiji.cps.module.cps.dal.mysql.marketing;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingClickEventPageReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelReqVO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingClickEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CpsMarketingClickEventMapper extends BaseMapperX<CpsMarketingClickEventDO> {

    default PageResult<CpsMarketingClickEventDO> selectPage(CpsMarketingClickEventPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsMarketingClickEventDO>()
                .eqIfPresent(CpsMarketingClickEventDO::getShortCode, reqVO.getShortCode())
                .eqIfPresent(CpsMarketingClickEventDO::getCampaignId, reqVO.getCampaignId())
                .eqIfPresent(CpsMarketingClickEventDO::getCreativeId, reqVO.getCreativeId())
                .eqIfPresent(CpsMarketingClickEventDO::getChannelCode, reqVO.getChannelCode())
                .eqIfPresent(CpsMarketingClickEventDO::getTrustedSource, reqVO.getTrustedSource())
                .eqIfPresent(CpsMarketingClickEventDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(CpsMarketingClickEventDO::getClickTime, reqVO.getStartTime(), reqVO.getEndTime())
                .orderByDesc(CpsMarketingClickEventDO::getId));
    }

    default CpsMarketingClickEventDO selectByDedupeKey(String dedupeKey) {
        return selectOne(CpsMarketingClickEventDO::getDedupeKey, dedupeKey);
    }

    default List<CpsMarketingClickEventDO> selectListForFunnel(CpsMarketingFunnelReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<CpsMarketingClickEventDO>()
                .eqIfPresent(CpsMarketingClickEventDO::getCampaignId, reqVO.getCampaignId())
                .eqIfPresent(CpsMarketingClickEventDO::getCreativeId, reqVO.getCreativeId())
                .eqIfPresent(CpsMarketingClickEventDO::getChannelCode, reqVO.getChannelCode())
                .betweenIfPresent(CpsMarketingClickEventDO::getClickTime, reqVO.getStartTime(), reqVO.getEndTime())
                .orderByDesc(CpsMarketingClickEventDO::getId));
    }
}
