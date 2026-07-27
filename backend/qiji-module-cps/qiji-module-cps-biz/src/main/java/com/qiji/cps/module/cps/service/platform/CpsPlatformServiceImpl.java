package com.qiji.cps.module.cps.service.platform;

import com.qiji.cps.framework.common.enums.CommonStatusEnum;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.config.CpsCacheConfig;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformPageReqVO;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.dal.dataobject.vendor.CpsApiVendorDO;
import com.qiji.cps.module.cps.dal.mysql.platform.CpsPlatformMapper;
import com.qiji.cps.module.cps.service.adzone.CpsAdzoneService;
import com.qiji.cps.module.cps.service.onboarding.CpsPlatformOnboardingCacheInvalidator;
import com.qiji.cps.module.cps.service.vendor.CpsApiVendorService;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.*;

/**
 * CPS平台配置 Service 实现类
 *
 * @author CPS System
 */
@Service
@Validated
public class CpsPlatformServiceImpl implements CpsPlatformService {

    private static final Integer CPS_ENABLE_STATUS = 1;

    @Resource
    private CpsPlatformMapper platformMapper;

    @Resource
    private CpsApiVendorService vendorService;

    @Resource
    private CpsAdzoneService adzoneService;

    @Resource
    private CpsPlatformOnboardingCacheInvalidator cacheInvalidator;

    @Override
    public Long createPlatform(CpsPlatformSaveReqVO createReqVO) {
        // 校验平台编码唯一
        validatePlatformCodeUnique(null, createReqVO.getPlatformCode());
        validatePublishableReferences(createReqVO);
        // 插入
        CpsPlatformDO platform = BeanUtils.toBean(createReqVO, CpsPlatformDO.class);
        platformMapper.insert(platform);
        cacheInvalidator.evictPlatformAfterCommit(platform.getPlatformCode());
        return platform.getId();
    }

    @Override
    public void updatePlatform(CpsPlatformSaveReqVO updateReqVO) {
        // 校验存在
        CpsPlatformDO existing = validatePlatformExists(updateReqVO.getId());
        if (!Objects.equals(existing.getPlatformCode(), updateReqVO.getPlatformCode())) {
            throw exception(ONBOARDING_CONFIG_INVALID, "平台编码创建后不可修改");
        }
        // 校验平台编码唯一
        validatePlatformCodeUnique(updateReqVO.getId(), updateReqVO.getPlatformCode());
        validatePublishableReferences(updateReqVO);
        // 更新
        CpsPlatformDO updateObj = BeanUtils.toBean(updateReqVO, CpsPlatformDO.class);
        platformMapper.updateById(updateObj);
        cacheInvalidator.evictPlatformAfterCommit(updateReqVO.getPlatformCode());
    }

    @Override
    public void deletePlatform(Long id) {
        // 校验存在
        CpsPlatformDO platform = validatePlatformExists(id);
        if (CPS_ENABLE_STATUS.equals(platform.getStatus())) {
            throw exception(ONBOARDING_PLATFORM_ENABLED);
        }
        // 删除
        platformMapper.deleteById(id);
        cacheInvalidator.evictPlatformAfterCommit(platform.getPlatformCode());
    }

    @Override
    public CpsPlatformDO getPlatform(Long id) {
        return platformMapper.selectById(id);
    }

    @Override
    public PageResult<CpsPlatformDO> getPlatformPage(CpsPlatformPageReqVO pageReqVO) {
        return platformMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CpsPlatformDO> getEnabledPlatformList() {
        return platformMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    @Cacheable(cacheNames = CpsCacheConfig.CACHE_PLATFORM, key = "#platformCode",
            cacheManager = "cpsCacheManager")
    public CpsPlatformDO getPlatformByCode(String platformCode) {
        return platformMapper.selectByPlatformCode(platformCode);
    }

    @Override
    public Long upsertPlatformForOnboarding(CpsPlatformSaveReqVO saveReqVO,
                                            List<String> supportedVendorCodes) {
        CpsPlatformDO existing = platformMapper.selectByPlatformCode(saveReqVO.getPlatformCode());
        validateOnboardingReferences(saveReqVO);
        CpsPlatformDO target = BeanUtils.toBean(saveReqVO, CpsPlatformDO.class);
        target.setSupportedVendors(supportedVendorCodes == null ? null
                : supportedVendorCodes.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(java.util.stream.Collectors.joining(",")));
        if (existing == null) {
            platformMapper.insert(target);
            return target.getId();
        }
        if (!Objects.equals(existing.getPlatformCode(), saveReqVO.getPlatformCode())) {
            throw exception(ONBOARDING_CONFIG_INVALID, "平台编码创建后不可修改");
        }
        target.setId(existing.getId());
        platformMapper.updateById(target);
        return existing.getId();
    }

    private CpsPlatformDO validatePlatformExists(Long id) {
        CpsPlatformDO platform = platformMapper.selectById(id);
        if (platform == null) {
            throw exception(PLATFORM_NOT_EXISTS);
        }
        return platform;
    }

    private void validatePlatformCodeUnique(Long id, String platformCode) {
        CpsPlatformDO platform = platformMapper.selectByPlatformCode(platformCode);
        if (platform == null) {
            return;
        }
        if (id == null || !id.equals(platform.getId())) {
            throw exception(PLATFORM_CODE_DUPLICATE, platformCode);
        }
    }

    private void validateDefaultVendor(String platformCode, String activeVendorCode) {
        if (!StringUtils.hasText(activeVendorCode)) {
            return;
        }
        CpsApiVendorDO vendor = vendorService.getVendorByCodeAndPlatform(activeVendorCode, platformCode);
        if (vendor == null || !CPS_ENABLE_STATUS.equals(vendor.getStatus())) {
            throw exception(VENDOR_NOT_EXISTS);
        }
    }

    private void validateDefaultAdzone(String platformCode, String defaultAdzoneId) {
        if (!StringUtils.hasText(defaultAdzoneId)) {
            return;
        }
        List<CpsAdzoneDO> adzones = adzoneService.getAdzoneListByPlatformCode(platformCode);
        boolean exists = adzones.stream()
                .anyMatch(adzone -> defaultAdzoneId.equals(adzone.getAdzoneId())
                        && CPS_ENABLE_STATUS.equals(adzone.getStatus()));
        if (!exists) {
            throw exception(ADZONE_NOT_EXISTS);
        }
    }

    private void validatePublishableReferences(CpsPlatformSaveReqVO request) {
        if (CPS_ENABLE_STATUS.equals(request.getStatus())
                && (!StringUtils.hasText(request.getActiveVendorCode())
                || !StringUtils.hasText(request.getDefaultAdzoneId()))) {
            throw exception(ONBOARDING_CONFIG_INVALID,
                    "启用平台必须绑定主供应商和运行时默认推广位");
        }
        validateDefaultVendor(request.getPlatformCode(), request.getActiveVendorCode());
        validateDefaultAdzone(request.getPlatformCode(), request.getDefaultAdzoneId());
    }

    private void validateOnboardingReferences(CpsPlatformSaveReqVO request) {
        if (CPS_ENABLE_STATUS.equals(request.getStatus())
                && (!StringUtils.hasText(request.getActiveVendorCode())
                || !StringUtils.hasText(request.getDefaultAdzoneId()))) {
            throw exception(ONBOARDING_CONFIG_INVALID,
                    "启用平台必须绑定主供应商和运行时默认推广位");
        }
        if (StringUtils.hasText(request.getActiveVendorCode())) {
            boolean vendorExists = vendorService.getVendorListByPlatform(request.getPlatformCode())
                    .stream()
                    .anyMatch(vendor -> request.getActiveVendorCode().equals(vendor.getVendorCode())
                            && CPS_ENABLE_STATUS.equals(vendor.getStatus()));
            if (!vendorExists) {
                throw exception(VENDOR_NOT_EXISTS);
            }
        }
        if (StringUtils.hasText(request.getDefaultAdzoneId())) {
            boolean adzoneExists = adzoneService.getAdzoneListByPlatform(request.getPlatformCode())
                    .stream()
                    .anyMatch(adzone -> request.getDefaultAdzoneId().equals(adzone.getAdzoneId())
                            && CPS_ENABLE_STATUS.equals(adzone.getStatus()));
            if (!adzoneExists) {
                throw exception(ADZONE_NOT_EXISTS);
            }
        }
    }

}
