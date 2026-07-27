package com.qiji.cps.module.cps.service.vendor;

import com.qiji.cps.framework.common.enums.CommonStatusEnum;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorConfigValidationResult;
import com.qiji.cps.module.cps.client.CpsVendorDescriptor;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.config.CpsCacheConfig;
import com.qiji.cps.module.cps.controller.admin.vendor.vo.CpsApiVendorPageReqVO;
import com.qiji.cps.module.cps.controller.admin.vendor.vo.CpsApiVendorSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.vendor.CpsApiVendorDO;
import com.qiji.cps.module.cps.dal.mysql.vendor.CpsApiVendorMapper;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import com.qiji.cps.module.cps.service.onboarding.CpsPlatformOnboardingCacheInvalidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.*;

/**
 * CPS API 供应商配置 Service 实现类
 *
 * @author CPS System
 */
@Service
@Validated
@Slf4j
public class CpsApiVendorServiceImpl implements CpsApiVendorService {

    @Resource
    private CpsApiVendorMapper vendorMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private List<CpsApiVendorClient> vendorClients;

    @Resource
    private CpsPlatformOnboardingCacheInvalidator cacheInvalidator;

    @Override
    public Long createVendor(CpsApiVendorSaveReqVO createReqVO) {
        // 校验供应商+平台组合唯一
        validateVendorPlatformUnique(null, createReqVO.getVendorCode(), createReqVO.getPlatformCode());
        validateVendorConfig(createReqVO);
        validateVendorEnableReady(createReqVO);
        // 插入
        CpsApiVendorDO vendor = BeanUtils.toBean(createReqVO, CpsApiVendorDO.class);
        vendorMapper.insert(vendor);
        cacheInvalidator.evictVendorAfterCommit();
        return vendor.getId();
    }

    @Override
    public void updateVendor(CpsApiVendorSaveReqVO updateReqVO) {
        // 校验存在
        CpsApiVendorDO existing = validateVendorExists(updateReqVO.getId());
        preserveStoredCredentials(updateReqVO, existing);
        // 校验供应商+平台组合唯一
        validateVendorPlatformUnique(updateReqVO.getId(), updateReqVO.getVendorCode(), updateReqVO.getPlatformCode());
        validateVendorConfig(updateReqVO);
        validateVendorEnableReady(updateReqVO);
        // 更新
        CpsApiVendorDO updateObj = BeanUtils.toBean(updateReqVO, CpsApiVendorDO.class);
        vendorMapper.updateById(updateObj);
        cacheInvalidator.evictVendorAfterCommit();
    }

    @Override
    public void deleteVendor(Long id) {
        validateVendorExists(id);
        vendorMapper.deleteById(id);
        cacheInvalidator.evictVendorAfterCommit();
    }

    @Override
    public CpsApiVendorDO getVendor(Long id) {
        return vendorMapper.selectById(id);
    }

    @Override
    public PageResult<CpsApiVendorDO> getVendorPage(CpsApiVendorPageReqVO pageReqVO) {
        return vendorMapper.selectPage(pageReqVO);
    }

    @Override
    @Cacheable(cacheNames = CpsCacheConfig.CACHE_API_VENDOR,
            key = "#vendorCode + ':' + #platformCode",
            cacheManager = "cpsCacheManager")
    public CpsApiVendorDO getVendorByCodeAndPlatform(String vendorCode, String platformCode) {
        return vendorMapper.selectByVendorAndPlatform(vendorCode, platformCode);
    }

    @Override
    public List<CpsApiVendorDO> getEnabledVendorsByPlatform(String platformCode) {
        return vendorMapper.selectListByPlatformCode(platformCode);
    }

    @Override
    public List<CpsApiVendorDO> getEnabledVendorList() {
        return vendorMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public List<CpsApiVendorDO> getVendorListByPlatform(String platformCode) {
        return vendorMapper.selectAllByPlatformCode(platformCode);
    }

    @Override
    public Long upsertVendorForOnboarding(CpsApiVendorSaveReqVO saveReqVO) {
        CpsApiVendorDO existing = vendorMapper.selectByVendorAndPlatform(
                saveReqVO.getVendorCode(), saveReqVO.getPlatformCode());
        if (existing == null) {
            validateVendorConfig(saveReqVO);
            validateVendorEnableReady(saveReqVO);
            CpsApiVendorDO created = BeanUtils.toBean(saveReqVO, CpsApiVendorDO.class);
            vendorMapper.insert(created);
            return created.getId();
        }
        saveReqVO.setId(existing.getId());
        preserveStoredCredentials(saveReqVO, existing);
        validateVendorConfig(saveReqVO);
        validateVendorEnableReady(saveReqVO);
        CpsApiVendorDO updated = BeanUtils.toBean(saveReqVO, CpsApiVendorDO.class);
        vendorMapper.updateById(updated);
        return existing.getId();
    }

    @Override
    public void deleteVendorsNotIn(String platformCode, Set<String> retainedVendorCodes) {
        Set<String> retained = new HashSet<>();
        if (retainedVendorCodes != null) {
            retainedVendorCodes.stream()
                    .filter(StringUtils::hasText)
                    .map(code -> code.trim().toLowerCase(Locale.ROOT))
                    .forEach(retained::add);
        }
        for (CpsApiVendorDO vendor : vendorMapper.selectAllByPlatformCode(platformCode)) {
            String vendorCode = vendor.getVendorCode() == null ? null
                    : vendor.getVendorCode().trim().toLowerCase(Locale.ROOT);
            if (!retained.contains(vendorCode)) {
                vendorMapper.deleteById(vendor.getId());
            }
        }
    }

    @Override
    public CpsVendorConfig buildVendorConfig(CpsApiVendorDO vendorDO) {
        if (vendorDO == null) {
            return null;
        }
        Map<String, String> extraConfigMap = parseExtraConfig(vendorDO.getExtraConfig());
        return CpsVendorConfig.builder()
                .vendorCode(vendorDO.getVendorCode())
                .vendorType(vendorDO.getVendorType())
                .platformCode(vendorDO.getPlatformCode())
                .appKey(vendorDO.getAppKey())
                .appSecret(vendorDO.getAppSecret())
                .apiBaseUrl(vendorDO.getApiBaseUrl())
                .authToken(vendorDO.getAuthToken())
                .defaultAdzoneId(vendorDO.getDefaultAdzoneId())
                .extraConfig(extraConfigMap)
                .build();
    }

    @Override
    public CpsVendorConfig getVendorConfig(String vendorCode, String platformCode) {
        CpsApiVendorDO vendorDO = getVendorByCodeAndPlatform(vendorCode, platformCode);
        return buildVendorConfig(vendorDO);
    }

    // ==================== 私有方法 ====================

    private CpsApiVendorDO validateVendorExists(Long id) {
        CpsApiVendorDO vendor = vendorMapper.selectById(id);
        if (vendor == null) {
            throw exception(VENDOR_NOT_EXISTS);
        }
        return vendor;
    }

    private void validateVendorPlatformUnique(Long id, String vendorCode, String platformCode) {
        CpsApiVendorDO vendor = vendorMapper.selectByVendorAndPlatform(vendorCode, platformCode);
        if (vendor == null) {
            return;
        }
        if (id == null || !id.equals(vendor.getId())) {
            throw exception(VENDOR_PLATFORM_DUPLICATE, vendorCode, platformCode);
        }
    }

    private void validateVendorEnableReady(CpsApiVendorSaveReqVO reqVO) {
        if (!CommonStatusEnum.ENABLE.getStatus().equals(reqVO.getStatus())) {
            return;
        }
        if (!CpsVendorCodeEnum.OFFICIAL.getCode().equals(reqVO.getVendorCode())) {
            return;
        }
        CpsApiVendorClient client = findVendorClient(reqVO.getVendorCode(), reqVO.getPlatformCode());
        if (client == null) {
            throw exception(VENDOR_CAPABILITY_NOT_READY, reqVO.getVendorCode(), reqVO.getPlatformCode(),
                    "未注册 official client");
        }
        CpsVendorDescriptor descriptor = client.describe();
        Set<CpsVendorCapability> capabilities = descriptor == null ? Set.of() : descriptor.getCapabilities();
        boolean hasBusinessCapability = capabilities != null && capabilities.stream()
                .anyMatch(capability -> capability != CpsVendorCapability.CONNECTION_TEST);
        if (!hasBusinessCapability) {
            throw exception(VENDOR_CAPABILITY_NOT_READY, reqVO.getVendorCode(), reqVO.getPlatformCode(),
                    "仅声明 CONNECTION_TEST，缺少真实业务能力验收");
        }
    }

    private void validateVendorConfig(CpsApiVendorSaveReqVO reqVO) {
        Map<String, String> extraConfig = parseExtraConfigStrict(reqVO.getExtraConfig());
        if (!CommonStatusEnum.ENABLE.getStatus().equals(reqVO.getStatus())) {
            return;
        }
        CpsApiVendorClient client = findVendorClient(reqVO.getVendorCode(), reqVO.getPlatformCode());
        if (client == null) {
            return;
        }
        CpsVendorDescriptor descriptor = client.describe();
        if (descriptor == null || descriptor.getConfigSchema() == null) {
            throw exception(ONBOARDING_CONFIG_INVALID, "供应商配置校验规则未注册");
        }
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode(reqVO.getVendorCode())
                .vendorType(reqVO.getVendorType())
                .platformCode(reqVO.getPlatformCode())
                .appKey(reqVO.getAppKey())
                .appSecret(reqVO.getAppSecret())
                .apiBaseUrl(reqVO.getApiBaseUrl())
                .authToken(reqVO.getAuthToken())
                .defaultAdzoneId(reqVO.getDefaultAdzoneId())
                .extraConfig(extraConfig)
                .build();
        CpsVendorConfigValidationResult result = descriptor.getConfigSchema().validate(config);
        if (!result.isValid()) {
            throw exception(ONBOARDING_CONFIG_INVALID,
                    "供应商配置字段校验失败：" + String.join(",", result.getErrors()));
        }
    }

    private void preserveStoredCredentials(CpsApiVendorSaveReqVO request,
                                           CpsApiVendorDO existing) {
        if (!StringUtils.hasText(request.getAppSecret())) {
            request.setAppSecret(existing.getAppSecret());
        }
        if (!StringUtils.hasText(request.getAuthToken())) {
            request.setAuthToken(existing.getAuthToken());
        }
    }

    private CpsApiVendorClient findVendorClient(String vendorCode, String platformCode) {
        if (vendorClients == null) {
            return null;
        }
        return vendorClients.stream()
                .filter(client -> vendorCode.equals(client.getVendorCode())
                        && platformCode.equals(client.getPlatformCode()))
                .findFirst()
                .orElse(null);
    }

    private Map<String, String> parseExtraConfig(String extraConfigJson) {
        if (extraConfigJson == null || extraConfigJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(extraConfigJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("[parseExtraConfig] JSON解析失败, errorType={}", e.getClass().getSimpleName());
            return new HashMap<>();
        }
    }

    private Map<String, String> parseExtraConfigStrict(String extraConfigJson) {
        if (!StringUtils.hasText(extraConfigJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(extraConfigJson,
                    new TypeReference<Map<String, String>>() { });
        } catch (Exception e) {
            throw exception(ONBOARDING_CONFIG_INVALID, "供应商扩展配置格式无效");
        }
    }

}
