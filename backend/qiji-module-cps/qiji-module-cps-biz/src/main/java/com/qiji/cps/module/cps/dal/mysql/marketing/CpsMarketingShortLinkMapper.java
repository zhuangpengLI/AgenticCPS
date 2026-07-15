package com.qiji.cps.module.cps.dal.mysql.marketing;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingShortLinkPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingShortLinkDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CpsMarketingShortLinkMapper extends BaseMapperX<CpsMarketingShortLinkDO> {

    default PageResult<CpsMarketingShortLinkDO> selectPage(CpsMarketingShortLinkPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsMarketingShortLinkDO>()
                .eqIfPresent(CpsMarketingShortLinkDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsMarketingShortLinkDO::getVendorCode, reqVO.getVendorCode())
                .eqIfPresent(CpsMarketingShortLinkDO::getCampaignId, reqVO.getCampaignId())
                .eqIfPresent(CpsMarketingShortLinkDO::getChannelCode, reqVO.getChannelCode())
                .eqIfPresent(CpsMarketingShortLinkDO::getStatus, reqVO.getStatus())
                .likeIfPresent(CpsMarketingShortLinkDO::getShortCode, reqVO.getShortCode())
                .orderByDesc(CpsMarketingShortLinkDO::getId));
    }

    default CpsMarketingShortLinkDO selectByRequestHash(String requestHash) {
        return selectOne(CpsMarketingShortLinkDO::getRequestHash, requestHash);
    }

    default CpsMarketingShortLinkDO selectByShortCode(String shortCode) {
        return selectOne(CpsMarketingShortLinkDO::getShortCode, shortCode);
    }

    default List<CpsMarketingShortLinkDO> selectListForFunnel(CpsMarketingFunnelReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<CpsMarketingShortLinkDO>()
                .eqIfPresent(CpsMarketingShortLinkDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsMarketingShortLinkDO::getVendorCode, reqVO.getVendorCode())
                .eqIfPresent(CpsMarketingShortLinkDO::getCampaignId, reqVO.getCampaignId())
                .eqIfPresent(CpsMarketingShortLinkDO::getCreativeId, reqVO.getCreativeId())
                .eqIfPresent(CpsMarketingShortLinkDO::getChannelCode, reqVO.getChannelCode())
                .betweenIfPresent(CpsMarketingShortLinkDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
                .orderByDesc(CpsMarketingShortLinkDO::getId));
    }
}
