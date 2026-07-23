package com.qiji.cps.module.cps.dal.mysql.onboarding;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.module.cps.dal.dataobject.onboarding.CpsPlatformOnboardingDraftDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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

}
