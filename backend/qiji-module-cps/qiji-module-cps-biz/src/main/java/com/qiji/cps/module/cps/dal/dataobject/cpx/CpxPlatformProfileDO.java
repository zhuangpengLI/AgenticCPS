package com.qiji.cps.module.cps.dal.dataobject.cpx;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("cpx_platform_profile")
@KeySequence("cpx_platform_profile_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpxPlatformProfileDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String platformCode;
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
