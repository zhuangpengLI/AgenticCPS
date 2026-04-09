package cn.iocoder.yudao.module.cps.service.vendor;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.cps.client.dto.CpsVendorConfig;
import cn.iocoder.yudao.module.cps.config.CpsCacheConfig;
import cn.iocoder.yudao.module.cps.controller.admin.vendor.vo.CpsApiVendorPageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.vendor.vo.CpsApiVendorSaveReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.vendor.CpsApiVendorDO;
import cn.iocoder.yudao.module.cps.dal.mysql.vendor.CpsApiVendorMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.cps.enums.CpsErrorCodeConstants.*;

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

    @Override
    public Long createVendor(CpsApiVendorSaveReqVO createReqVO) {
        // 校验供应商+平台组合唯一
        validateVendorPlatformUnique(null, createReqVO.getVendorCode(), createReqVO.getPlatformCode());
        // 插入
        CpsApiVendorDO vendor = BeanUtils.toBean(createReqVO, CpsApiVendorDO.class);
        vendorMapper.insert(vendor);
        return vendor.getId();
    }

    @Override
    @CacheEvict(cacheNames = CpsCacheConfig.CACHE_API_VENDOR,
            key = "#updateReqVO.vendorCode + ':' + #updateReqVO.platformCode",
            cacheManager = "cpsCacheManager")
    public void updateVendor(CpsApiVendorSaveReqVO updateReqVO) {
        // 校验存在
        validateVendorExists(updateReqVO.getId());
        // 校验供应商+平台组合唯一
        validateVendorPlatformUnique(updateReqVO.getId(), updateReqVO.getVendorCode(), updateReqVO.getPlatformCode());
        // 更新
        CpsApiVendorDO updateObj = BeanUtils.toBean(updateReqVO, CpsApiVendorDO.class);
        vendorMapper.updateById(updateObj);
    }

    @Override
    public void deleteVendor(Long id) {
        validateVendorExists(id);
        vendorMapper.deleteById(id);
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

    private void validateVendorExists(Long id) {
        if (vendorMapper.selectById(id) == null) {
            throw exception(VENDOR_NOT_EXISTS);
        }
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

    private Map<String, String> parseExtraConfig(String extraConfigJson) {
        if (extraConfigJson == null || extraConfigJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(extraConfigJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("[parseExtraConfig] JSON解析失败: {}", extraConfigJson, e);
            return new HashMap<>();
        }
    }

}
