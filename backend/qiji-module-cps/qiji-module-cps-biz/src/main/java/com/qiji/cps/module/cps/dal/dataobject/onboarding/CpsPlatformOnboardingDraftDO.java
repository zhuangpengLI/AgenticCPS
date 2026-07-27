package com.qiji.cps.module.cps.dal.dataobject.onboarding;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.mybatis.core.type.EncryptTypeHandler;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Tenant-scoped platform onboarding draft.
 */
@TableName(value = "cps_platform_onboarding_draft", autoResultMap = true)
@KeySequence("cps_platform_onboarding_draft_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsPlatformOnboardingDraftDO extends TenantBaseDO {

    @TableId
    @ToString.Include
    private Long id;

    @ToString.Include
    private String platformCode;

    @ToString.Include
    private String mode;

    @TableField(typeHandler = EncryptTypeHandler.class)
    private String payloadCiphertext;

    @ToString.Include
    private Integer draftVersion;

    private String configFingerprint;

    private String validatedFingerprint;

    @ToString.Include
    private String status;

    private String checkSummary;

    private LocalDateTime validatedAt;

    private LocalDateTime publishedAt;

}
