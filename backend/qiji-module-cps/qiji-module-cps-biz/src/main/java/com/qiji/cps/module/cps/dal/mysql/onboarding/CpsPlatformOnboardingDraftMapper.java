package com.qiji.cps.module.cps.dal.mysql.onboarding;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.module.cps.dal.dataobject.onboarding.CpsPlatformOnboardingDraftDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * Platform onboarding draft Mapper.
 */
@Mapper
public interface CpsPlatformOnboardingDraftMapper extends BaseMapperX<CpsPlatformOnboardingDraftDO> {

    default CpsPlatformOnboardingDraftDO selectByPlatformCode(String platformCode) {
        return selectOne(CpsPlatformOnboardingDraftDO::getPlatformCode, platformCode);
    }

    @Update("""
            UPDATE cps_platform_onboarding_draft
            SET payload_ciphertext = #{payload, typeHandler=com.qiji.cps.framework.mybatis.core.type.EncryptTypeHandler},
                config_fingerprint = #{fingerprint},
                validated_fingerprint = NULL,
                status = #{status},
                check_summary = NULL,
                validated_at = NULL,
                published_at = NULL,
                draft_version = draft_version + 1,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND draft_version = #{expectedVersion}
              AND deleted = FALSE
            """)
    int updatePayload(@Param("id") Long id,
                      @Param("expectedVersion") Integer expectedVersion,
                      @Param("payload") String payload,
                      @Param("fingerprint") String fingerprint,
                      @Param("status") String status);

    @Update("""
            UPDATE cps_platform_onboarding_draft
            SET status = #{status},
                validated_fingerprint = NULL,
                check_summary = NULL,
                validated_at = NULL,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND draft_version = #{expectedVersion}
              AND deleted = FALSE
            """)
    int markValidating(@Param("id") Long id,
                       @Param("expectedVersion") Integer expectedVersion,
                       @Param("status") String status);

    @Update("""
            UPDATE cps_platform_onboarding_draft
            SET status = #{status},
                validated_fingerprint = #{validatedFingerprint},
                check_summary = #{checkSummary},
                validated_at = #{validatedAt},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND draft_version = #{expectedVersion}
              AND deleted = FALSE
            """)
    int markChecked(@Param("id") Long id,
                    @Param("expectedVersion") Integer expectedVersion,
                    @Param("status") String status,
                    @Param("validatedFingerprint") String validatedFingerprint,
                    @Param("checkSummary") String checkSummary,
                    @Param("validatedAt") LocalDateTime validatedAt);

    @Update("""
            UPDATE cps_platform_onboarding_draft
            SET deleted = TRUE,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND draft_version = #{expectedVersion}
              AND deleted = FALSE
            """)
    int deleteByIdAndVersion(@Param("id") Long id,
                             @Param("expectedVersion") Integer expectedVersion);

}
