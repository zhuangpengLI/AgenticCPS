package com.qiji.cps.module.cps.service.rebate;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateConfigPageReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateConfigSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateConfigMapper;
import com.qiji.cps.module.cps.service.onboarding.CpsPlatformOnboardingCacheInvalidator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_CONFIG_INVALID;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.REBATE_CONFIG_AMOUNT_RANGE_INVALID;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.REBATE_CONFIG_NOT_EXISTS;

/**
 * CPS返利配置 Service 实现类
 *
 * @author CPS System
 */
@Slf4j
@Service
@Validated
public class CpsRebateConfigServiceImpl implements CpsRebateConfigService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Resource
    private CpsRebateConfigMapper rebateConfigMapper;

    @Resource
    private CpsPlatformOnboardingCacheInvalidator cacheInvalidator;

    @Override
    public Long createRebateConfig(CpsRebateConfigSaveReqVO createReqVO) {
        normalizeAndValidate(createReqVO);
        validateScopeUnique(null, createReqVO);
        CpsRebateConfigDO config = BeanUtils.toBean(createReqVO, CpsRebateConfigDO.class);
        rebateConfigMapper.insert(config);
        cacheInvalidator.evictRebateAfterCommit();
        return config.getId();
    }

    @Override
    public void updateRebateConfig(CpsRebateConfigSaveReqVO updateReqVO) {
        normalizeAndValidate(updateReqVO);
        validateRebateConfigExists(updateReqVO.getId());
        validateScopeUnique(updateReqVO.getId(), updateReqVO);
        CpsRebateConfigDO updateObj = BeanUtils.toBean(updateReqVO, CpsRebateConfigDO.class);
        rebateConfigMapper.updateById(updateObj);
        cacheInvalidator.evictRebateAfterCommit();
    }

    @Override
    public void deleteRebateConfig(Long id) {
        validateRebateConfigExists(id);
        rebateConfigMapper.deleteById(id);
        cacheInvalidator.evictRebateAfterCommit();
    }

    @Override
    public CpsRebateConfigDO getRebateConfig(Long id) {
        return rebateConfigMapper.selectById(id);
    }

    @Override
    public PageResult<CpsRebateConfigDO> getRebateConfigPage(CpsRebateConfigPageReqVO pageReqVO) {
        return rebateConfigMapper.selectPage(pageReqVO,
                new LambdaQueryWrapperX<CpsRebateConfigDO>()
                        .eqIfPresent(CpsRebateConfigDO::getMemberLevelId, pageReqVO.getMemberLevelId())
                        .eqIfPresent(CpsRebateConfigDO::getMemberId, pageReqVO.getMemberId())
                        .eqIfPresent(CpsRebateConfigDO::getPlatformCode, pageReqVO.getPlatformCode())
                        .eqIfPresent(CpsRebateConfigDO::getStatus, pageReqVO.getStatus())
                        .orderByDesc(CpsRebateConfigDO::getPriority)
                        .orderByDesc(CpsRebateConfigDO::getId));
    }

    @Override
    public List<CpsRebateConfigDO> getEnabledRebateConfigList() {
        // CPS 配置表历史契约为 1=启用、0=禁用，不使用框架通用状态枚举。
        return rebateConfigMapper.selectListByStatus(1);
    }

    @Override
    public CpsRebateConfigDO matchRebateConfig(Long memberLevelId, String platformCode) {
        return matchRebateConfig(null, memberLevelId, platformCode);
    }

    @Override
    public CpsRebateConfigDO matchRebateConfig(Long memberId, Long memberLevelId, String platformCode) {
        List<CpsRebateConfigDO> allConfigs = getEnabledRebateConfigList();
        if (allConfigs.isEmpty()) {
            return null;
        }

        CpsRebateConfigDO match = findMatch(allConfigs, memberId, null, platformCode);
        if (match != null) return match;
        match = findMatch(allConfigs, memberId, null, null);
        if (match != null) return match;
        match = findMatch(allConfigs, null, memberLevelId, platformCode);
        if (match != null) return match;
        match = findMatch(allConfigs, null, memberLevelId, null);
        if (match != null) return match;
        match = findMatch(allConfigs, null, null, platformCode);
        if (match != null) return match;
        return findMatch(allConfigs, null, null, null);
    }

    @Override
    public List<CpsRebateConfigDO> getManagedRebateRulesByPlatform(String platformCode) {
        return rebateConfigMapper.selectManagedRulesByPlatformCode(platformCode);
    }

    @Override
    public Long upsertManagedRebateRuleForOnboarding(CpsRebateConfigSaveReqVO saveReqVO) {
        normalizeAndValidate(saveReqVO);
        if (!StringUtils.hasText(saveReqVO.getPlatformCode()) || saveReqVO.getMemberId() != null) {
            throw exception(ONBOARDING_CONFIG_INVALID,
                    "平台接入只管理指定平台的非个人返利规则");
        }
        List<CpsRebateConfigDO> matches = rebateConfigMapper.selectListByScope(
                null, saveReqVO.getMemberLevelId(), saveReqVO.getPlatformCode(),
                saveReqVO.getPriority());
        if (matches.size() > 1) {
            throw exception(ONBOARDING_CONFIG_INVALID, "返利规则作用域重复");
        }
        if (matches.isEmpty()) {
            CpsRebateConfigDO created = BeanUtils.toBean(saveReqVO, CpsRebateConfigDO.class);
            rebateConfigMapper.insert(created);
            return created.getId();
        }
        CpsRebateConfigDO existing = matches.get(0);
        saveReqVO.setId(existing.getId());
        CpsRebateConfigDO updated = BeanUtils.toBean(saveReqVO, CpsRebateConfigDO.class);
        rebateConfigMapper.updateById(updated);
        return existing.getId();
    }

    @Override
    public void deleteManagedRebateRulesNotIn(String platformCode,
                                               Set<String> retainedScopeKeys) {
        Set<String> retained = retainedScopeKeys == null ? Set.of() : retainedScopeKeys;
        for (CpsRebateConfigDO rule
                : rebateConfigMapper.selectManagedRulesByPlatformCode(platformCode)) {
            String scopeKey = CpsRebateConfigService.managedScopeKey(
                    rule.getMemberLevelId(), rule.getPriority());
            if (!retained.contains(scopeKey)) {
                rebateConfigMapper.deleteById(rule.getId());
            }
        }
    }

    // ==================== 私有方法 ====================

    private void validateRebateConfigExists(Long id) {
        if (rebateConfigMapper.selectById(id) == null) {
            throw exception(REBATE_CONFIG_NOT_EXISTS);
        }
    }

    private void normalizeAndValidate(CpsRebateConfigSaveReqVO request) {
        if (StringUtils.hasText(request.getPlatformCode())) {
            request.setPlatformCode(request.getPlatformCode().trim().toLowerCase(Locale.ROOT));
        }
        if (request.getPriority() == null) {
            request.setPriority(0);
        } else if (request.getPriority() < 0) {
            throw exception(ONBOARDING_CONFIG_INVALID, "返利规则优先级必须为非负整数");
        }
        if (request.getStatus() != null
                && request.getStatus() != 0 && request.getStatus() != 1) {
            throw exception(ONBOARDING_CONFIG_INVALID, "返利规则状态只能为 0 或 1");
        }
        if (request.getRebateRate() == null
                || request.getRebateRate().compareTo(BigDecimal.ZERO) < 0
                || request.getRebateRate().compareTo(ONE_HUNDRED) > 0) {
            throw exception(ONBOARDING_CONFIG_INVALID, "返利比例必须在 0 到 100 之间");
        }
        if (isNegative(request.getMinRebateAmount())
                || isNegative(request.getMaxRebateAmount())) {
            throw exception(ONBOARDING_CONFIG_INVALID, "返利金额不能为负数");
        }
        if (request.getMinRebateAmount() != null && request.getMaxRebateAmount() != null
                && request.getMinRebateAmount().compareTo(request.getMaxRebateAmount()) > 0) {
            throw exception(REBATE_CONFIG_AMOUNT_RANGE_INVALID);
        }
    }

    private void validateScopeUnique(Long id, CpsRebateConfigSaveReqVO request) {
        boolean duplicate = rebateConfigMapper.selectListByScope(
                        request.getMemberId(), request.getMemberLevelId(),
                        request.getPlatformCode(), request.getPriority())
                .stream()
                .anyMatch(existing -> id == null || !id.equals(existing.getId()));
        if (duplicate) {
            throw exception(ONBOARDING_CONFIG_INVALID, "返利规则作用域重复");
        }
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    private CpsRebateConfigDO findMatch(List<CpsRebateConfigDO> configs, Long memberId,
                                        Long memberLevelId, String platformCode) {
        return configs.stream()
                .filter(c -> Objects.equals(c.getMemberId(), memberId)
                        && Objects.equals(c.getMemberLevelId(), memberLevelId)
                        && Objects.equals(c.getPlatformCode(), platformCode))
                .max(java.util.Comparator
                        .comparing(CpsRebateConfigDO::getPriority,
                                java.util.Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(CpsRebateConfigDO::getId,
                                java.util.Comparator.nullsFirst(Long::compareTo)))
                .orElse(null);
    }

}
