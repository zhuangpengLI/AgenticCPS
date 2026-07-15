package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingClickEventPageReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingClickEventRecordReqVO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingClickEventDO;
import jakarta.validation.Valid;

public interface CpsMarketingClickEventService {

    CpsMarketingClickEventDO recordClick(@Valid CpsMarketingClickEventRecordReqVO reqVO);

    PageResult<CpsMarketingClickEventDO> getClickEventPage(@Valid CpsMarketingClickEventPageReqVO reqVO);
}
