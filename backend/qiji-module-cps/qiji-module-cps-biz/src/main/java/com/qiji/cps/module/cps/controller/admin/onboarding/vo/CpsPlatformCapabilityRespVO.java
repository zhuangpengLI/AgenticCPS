package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsPlatformCapabilityRespVO {

    private String platformCode;
    private String platformName;
    private List<String> capabilities;
    private List<CpsVendorDescriptorRespVO> vendors;
}
