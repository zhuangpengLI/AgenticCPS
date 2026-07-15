package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingShortLinkCreateReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingShortLinkPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingShortLinkDO;
import jakarta.validation.Valid;

public interface CpsMarketingShortLinkService {

    CpsMarketingShortLinkDO createShortLink(@Valid CpsMarketingShortLinkCreateReqVO reqVO);

    PageResult<CpsMarketingShortLinkDO> getShortLinkPage(@Valid CpsMarketingShortLinkPageReqVO reqVO);

    String resolveTargetUrl(String shortCode);
}
