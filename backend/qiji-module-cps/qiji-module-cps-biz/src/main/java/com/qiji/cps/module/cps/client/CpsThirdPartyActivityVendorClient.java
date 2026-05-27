package com.qiji.cps.module.cps.client;

import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;

/**
 * 支持活动拉取的第三方供应商客户端。
 */
public interface CpsThirdPartyActivityVendorClient {

    String getVendorCode();

    String getPlatformCode();

    CpsThirdPartyPage<CpsThirdPartyActivity> fetchActivities(CpsThirdPartyActivityRequest request,
                                                             CpsVendorConfig config);

}
