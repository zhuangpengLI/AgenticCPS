package com.qiji.cps.module.cps.client;

import com.qiji.cps.module.cps.client.dto.CpsCouponInfo;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;

import java.util.EnumSet;
import java.util.Set;

public interface CpsCouponInfoVendorClient extends CpsApiVendorClient {

    CpsCouponInfo queryCouponInfo(String content, CpsVendorConfig config);

    @Override
    default Set<CpsVendorCapability> getCapabilities() {
        EnumSet<CpsVendorCapability> capabilities = EnumSet.copyOf(CpsApiVendorClient.super.getCapabilities());
        capabilities.add(CpsVendorCapability.COUPON_QUERY);
        return capabilities;
    }

}
