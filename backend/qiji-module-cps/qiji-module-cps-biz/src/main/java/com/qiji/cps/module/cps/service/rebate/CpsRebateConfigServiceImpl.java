package com.qiji.cps.module.cps.service.rebate;

import com.qiji.cps.framework.common.enums.CommonStatusEnum;
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
                        .eqIfPresent(CpsRebateConfigDO::getPlatformCode, pageReqVO.getPlatformCode())
                        .eqIfPresent(CpsRebateConfigDO::getStatus, pageReqVO.getStatus())
                        .orderByDesc(CpsRebateConfigDO::getPriority)
                        .orderByDesc(CpsRebateConfigDO::getId));
    }

    @Override
    public List<CpsRebateConfigDO> getEnabledRebateConfigList() {
        return rebateConfigMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public CpsRebateConfigDO matchRebateConfig(Long memberLevelId, String platformCode) {
        List<CpsRebateConfigDO> allConfigs = getEnabledRebateConfigList();
        if (allConfigs.isEmpty()) {
            return null;
        }

        // 优先级1: 会员等级 + 平台（精确匹配）
        CpsRebateConfigDO match = findMatch(allConfigs, memberLevelId, platformCode);
        if (match != null) return match;

        // 优先级2: 会员等级 + 全平台
        match = findMatch(allConfigs, memberLevelId, null);
        if (match != null) return match;

        // 优先级3: 全等级 + 平台
        match = findMatch(allConfigs, null, platformCode);
        if (match != null) return match;

        // 优先级4: 全等级 + 全平台（兜底）
        return findMatch(allConfigs, null, null);
    }

    // ==================== 私有方法 ====================

    private void validateRebateConfigExists(Long id) {
        if (rebateConfigMapper.selectById(id) == null) {
            throw exception(REBATE_CONFIG_NOT_EXISTS);
        }
    }

    private CpsRebateConfigDO findMatch(List<CpsRebateConfigDO> configs, Long memberLevelId, String platformCode) {
        return configs.stream()
                .filter(c -> Objects.equals(c.getMemberLevelId(), memberLevelId)
                        && Objects.equals(c.getPlatformCode(), platformCode))
                .findFirst()
                .orElse(null);
    }

}
