package com.qiji.cps.module.cps.client;

import com.qiji.cps.module.cps.client.dto.CpsCouponInfo;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;

public interface CpsCouponInfoVendorClient extends CpsApiVendorClient {

    CpsCouponInfo queryCouponInfo(String content, CpsVendorConfig config);

}
