package com.qiji.cps.module.cps.service.freeze;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.freeze.vo.CpsFreezeConfigPageReqVO;
import com.qiji.cps.module.cps.controller.admin.freeze.vo.CpsFreezeConfigSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.freeze.vo.CpsFreezeRecordPageReqVO;
import com.qiji.cps.module.cps.controller.admin.freeze.vo.CpsManualUnfreezeReqVO;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeConfigMapper;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeRecordMapper;
import com.qiji.cps.module.cps.enums.CpsFreezeStatusEnum;
import com.qiji.cps.module.cps.service.rebate.asset.CpsAssetOperatorContext;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.*;

/**
 * CPS 冻结解冻 Service 实现类
 *
 * <p>核心流程：
 * <ol>
 *   <li>配置管理：支持按平台维度维护解冻天数（全平台默认 + 平台专属）</li>
 *   <li>自动解冻：定时任务扫描到期的 frozen 记录，批量更新为 unfreezed</li>
 *   <li>手动解冻：管理员对单条冻结记录执行手动解冻</li>
 * </ol>
 * </p>
 *
 * @author CPS System
 */
@Slf4j
@Service
@Validated
public class CpsFreezeServiceImpl implements CpsFreezeService {

    @Resource
    private CpsFreezeConfigMapper freezeConfigMapper;

    @Resource
    private CpsFreezeRecordMapper freezeRecordMapper;

    @Resource
    private CpsRebateAssetService rebateAssetService;

    // ==================== 配置管理 ====================

    @Override
    public Long createFreezeConfig(CpsFreezeConfigSaveReqVO reqVO) {
        validateRange(reqVO, null);
        CpsFreezeConfigDO config = BeanUtils.toBean(reqVO, CpsFreezeConfigDO.class);
        freezeConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateFreezeConfig(CpsFreezeConfigSaveReqVO reqVO) {
        // 校验配置存在
        CpsFreezeConfigDO current = validateFreezeConfigExists(reqVO.getId());
        if (isGlobalFallback(current) && !isGlobalFallback(reqVO)
                && freezeConfigMapper.selectEnabledRules().stream()
                .filter(this::isGlobalFallback)
                .noneMatch(rule -> !Objects.equals(rule.getId(), reqVO.getId()))) {
            throw new IllegalStateException("不能停用或改变当前租户最后一条全平台全金额兜底冻结规则");
        }
        validateRange(reqVO, reqVO.getId());
        // 更新
        CpsFreezeConfigDO updateObj = BeanUtils.toBean(reqVO, CpsFreezeConfigDO.class);
        freezeConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteFreezeConfig(Long id) {
        // 校验配置存在
        CpsFreezeConfigDO config = validateFreezeConfigExists(id);
        if (isGlobalFallback(config) && freezeConfigMapper.selectEnabledRules().stream()
                .filter(this::isGlobalFallback).noneMatch(rule -> !Objects.equals(rule.getId(), id))) {
            throw new IllegalStateException("不能删除当前租户最后一条全平台全金额兜底冻结规则");
        }
        // 删除
        freezeConfigMapper.deleteById(id);
    }

    @Override
    public PageResult<CpsFreezeConfigDO> getFreezeConfigPage(CpsFreezeConfigPageReqVO reqVO) {
        return freezeConfigMapper.selectPage(reqVO);
    }

    @Override
    public CpsFreezeConfigDO getActiveConfig(String platformCode) {
        return getActiveConfig(platformCode, 0L);
    }

    @Override
    public CpsFreezeConfigDO getActiveConfig(String platformCode, long rebateAmountCent) {
        List<CpsFreezeConfigDO> rules = freezeConfigMapper.selectEnabledRules();
        CpsFreezeConfigDO exact = findRange(rules, platformCode, rebateAmountCent);
        return exact != null ? exact : findRange(rules, null, rebateAmountCent);
    }

    // ==================== 解冻操作 ====================

    @Override
    public int batchUnfreeze(int batchSize) {
        // 查询到达解冻时间且状态为 frozen 的记录
        List<CpsFreezeRecordDO> list = freezeRecordMapper.selectPendingUnfreeze(batchSize);
        int count = 0;
        for (CpsFreezeRecordDO record : list) {
            try {
                String key = "auto-unfreeze:" + record.getId();
                rebateAssetService.releaseOrderRebate(record.getId(),
                        CpsAssetOperatorContext.system(key, "冻结期到期自动解冻"));
                count++;
            } catch (Exception e) {
                log.error("[batchUnfreeze] 解冻失败, recordId={}", record.getId(), e);
            }
        }
        return count;
    }

    @Override
    public void manualUnfreeze(Long recordId) {
        throw new IllegalArgumentException("手动解冻必须提供原因和幂等键");
    }

    @Override
    public void manualUnfreeze(CpsManualUnfreezeReqVO reqVO, Long adminUserId) {
        rebateAssetService.manualReleaseOrderRebate(reqVO.getRecordId(),
                CpsAssetOperatorContext.admin(String.valueOf(adminUserId), reqVO.getIdempotencyKey(), reqVO.getReason()));
    }

    @Override
    public PageResult<CpsFreezeRecordDO> getFreezeRecordPage(CpsFreezeRecordPageReqVO reqVO) {
        return freezeRecordMapper.selectPage(reqVO);
    }

    // ==================== 私有方法 ====================

    private CpsFreezeConfigDO validateFreezeConfigExists(Long id) {
        CpsFreezeConfigDO config = freezeConfigMapper.selectById(id);
        if (config == null) {
            throw exception(FREEZE_CONFIG_NOT_EXISTS);
        }
        return config;
    }

    private void validateRange(CpsFreezeConfigSaveReqVO reqVO, Long excludeId) {
        long min = reqVO.getMinAmountCent() == null ? 0L : reqVO.getMinAmountCent();
        Long max = reqVO.getMaxAmountCent();
        if (min < 0L || (max != null && max <= min)) {
            throw new IllegalArgumentException("冻结金额区间必须满足 min >= 0 且 max > min");
        }
        if (!Integer.valueOf(1).equals(reqVO.getStatus())) {
            return;
        }
        for (CpsFreezeConfigDO existing : freezeConfigMapper.selectEnabledRules()) {
            if (Objects.equals(existing.getId(), excludeId)
                    || !Objects.equals(existing.getPlatformCode(), reqVO.getPlatformCode())) {
                continue;
            }
            long existingMin = existing.getMinAmountCent() == null ? 0L : existing.getMinAmountCent();
            if (overlaps(min, max, existingMin, existing.getMaxAmountCent())) {
                throw new IllegalArgumentException("同一平台启用中的冻结金额区间不能重叠");
            }
        }
    }

    private CpsFreezeConfigDO findRange(List<CpsFreezeConfigDO> rules, String platformCode, long amountCent) {
        return rules.stream().filter(rule -> Objects.equals(rule.getPlatformCode(), platformCode))
                .filter(rule -> {
                    long min = rule.getMinAmountCent() == null ? 0L : rule.getMinAmountCent();
                    return amountCent >= min && (rule.getMaxAmountCent() == null || amountCent < rule.getMaxAmountCent());
                }).findFirst().orElse(null);
    }

    private boolean overlaps(long minA, Long maxA, long minB, Long maxB) {
        long upperA = maxA == null ? Long.MAX_VALUE : maxA;
        long upperB = maxB == null ? Long.MAX_VALUE : maxB;
        return minA < upperB && minB < upperA;
    }

    private boolean isGlobalFallback(CpsFreezeConfigDO config) {
        return Integer.valueOf(1).equals(config.getStatus()) && config.getPlatformCode() == null
                && (config.getMinAmountCent() == null || config.getMinAmountCent() == 0L)
                && config.getMaxAmountCent() == null;
    }

    private boolean isGlobalFallback(CpsFreezeConfigSaveReqVO config) {
        return Integer.valueOf(1).equals(config.getStatus()) && config.getPlatformCode() == null
                && (config.getMinAmountCent() == null || config.getMinAmountCent() == 0L)
                && config.getMaxAmountCent() == null;
    }

}
