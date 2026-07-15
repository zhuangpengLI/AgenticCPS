package com.qiji.cps.module.cps.service.rebate;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateConfigPageReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateConfigSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateConfigMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
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

    @Resource
    private CpsRebateConfigMapper rebateConfigMapper;

    @Override
    public Long createRebateConfig(CpsRebateConfigSaveReqVO createReqVO) {
        CpsRebateConfigDO config = BeanUtils.toBean(createReqVO, CpsRebateConfigDO.class);
        rebateConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateRebateConfig(CpsRebateConfigSaveReqVO updateReqVO) {
        validateRebateConfigExists(updateReqVO.getId());
        CpsRebateConfigDO updateObj = BeanUtils.toBean(updateReqVO, CpsRebateConfigDO.class);
        rebateConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteRebateConfig(Long id) {
        validateRebateConfigExists(id);
        rebateConfigMapper.deleteById(id);
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

    // ==================== 私有方法 ====================

    private void validateRebateConfigExists(Long id) {
        if (rebateConfigMapper.selectById(id) == null) {
            throw exception(REBATE_CONFIG_NOT_EXISTS);
        }
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
