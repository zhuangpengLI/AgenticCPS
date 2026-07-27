package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import com.qiji.cps.module.cps.client.CpsVendorConfigSchema;
import com.qiji.cps.module.cps.client.CpsVendorGovernancePolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsVendorDescriptorRespVO {

    private String vendorCode;
    private String platformCode;
    private String vendorType;
    private List<String> capabilities;
    private CpsVendorConfigSchema configSchema;
    private CpsVendorGovernancePolicy governancePolicy;
    private String sdkModule;
    private String version;
}
