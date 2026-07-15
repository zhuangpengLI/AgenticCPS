package com.qiji.cps.module.cps.client;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class CpsVendorDescriptor {

    String vendorCode;
    String platformCode;
    String vendorType;
    Set<CpsVendorCapability> capabilities;
    CpsVendorConfigSchema configSchema;
    CpsVendorGovernancePolicy governancePolicy;
    String sdkModule;
    String version;
}
