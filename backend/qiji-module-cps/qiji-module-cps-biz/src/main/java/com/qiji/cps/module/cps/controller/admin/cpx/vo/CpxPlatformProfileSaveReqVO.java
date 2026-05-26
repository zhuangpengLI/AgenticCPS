package com.qiji.cps.module.cps.controller.admin.cpx.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CpxPlatformProfileSaveReqVO {

    private Long id;
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;
    @NotBlank(message = "平台名称不能为空")
    private String platformName;
    private String platformLogo;
    private String supportedMethods;
    private String apiBaseUrl;
    private String callbackUrl;
    private String importTemplate;
    private String healthStatus;
    private Integer status;
    private String remark;
    private String extraConfig;
}
