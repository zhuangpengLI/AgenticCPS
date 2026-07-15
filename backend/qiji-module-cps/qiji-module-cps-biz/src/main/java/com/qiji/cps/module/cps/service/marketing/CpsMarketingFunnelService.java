package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelRespVO;
import jakarta.validation.Valid;

public interface CpsMarketingFunnelService {

    CpsMarketingFunnelRespVO getFunnelSummary(@Valid CpsMarketingFunnelReqVO reqVO);
}
