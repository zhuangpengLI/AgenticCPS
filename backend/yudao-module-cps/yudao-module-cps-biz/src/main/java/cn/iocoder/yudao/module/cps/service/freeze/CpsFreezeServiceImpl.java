package cn.iocoder.yudao.module.cps.service.freeze;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.cps.controller.admin.freeze.vo.CpsFreezeConfigPageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.freeze.vo.CpsFreezeConfigSaveReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.freeze.vo.CpsFreezeRecordPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO;
import cn.iocoder.yudao.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import cn.iocoder.yudao.module.cps.dal.mysql.freeze.CpsFreezeConfigMapper;
import cn.iocoder.yudao.module.cps.dal.mysql.freeze.CpsFreezeRecordMapper;
import cn.iocoder.yudao.module.cps.enums.CpsFreezeStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.cps.enums.CpsErrorCodeConstants.*;

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

    // ==================== 配置管理 ====================

    @Override
    public Long createFreezeConfig(CpsFreezeConfigSaveReqVO reqVO) {
        CpsFreezeConfigDO config = BeanUtils.toBean(reqVO, CpsFreezeConfigDO.class);
        freezeConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateFreezeConfig(CpsFreezeConfigSaveReqVO reqVO) {
        // 校验配置存在
        validateFreezeConfigExists(reqVO.getId());
        // 更新
        CpsFreezeConfigDO updateObj = BeanUtils.toBean(reqVO, CpsFreezeConfigDO.class);
        freezeConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteFreezeConfig(Long id) {
        // 校验配置存在
        validateFreezeConfigExists(id);
        // 删除
        freezeConfigMapper.deleteById(id);
    }

    @Override
    public PageResult<CpsFreezeConfigDO> getFreezeConfigPage(CpsFreezeConfigPageReqVO reqVO) {
        return freezeConfigMapper.selectPage(reqVO);
    }

    @Override
    public CpsFreezeConfigDO getActiveConfig(String platformCode) {
        return freezeConfigMapper.selectActiveByPlatform(platformCode);
    }

    // ==================== 解冻操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUnfreeze(int batchSize) {
        // 查询到达解冻时间且状态为 frozen 的记录
        List<CpsFreezeRecordDO> list = freezeRecordMapper.selectPendingUnfreeze(batchSize);
        int count = 0;
        for (CpsFreezeRecordDO record : list) {
            try {
                // 更新状态为已解冻
                CpsFreezeRecordDO update = new CpsFreezeRecordDO();
                update.setId(record.getId());
                update.setStatus(CpsFreezeStatusEnum.UNFREEZED.getStatus());
                update.setActualUnfreezeTime(LocalDateTime.now());
                freezeRecordMapper.updateById(update);
                // TODO: 调用返利账户解冻逻辑（账户可用余额 += freezeAmount）
                count++;
            } catch (Exception e) {
                log.error("[batchUnfreeze] 解冻失败, recordId={}", record.getId(), e);
            }
        }
        return count;
    }

    @Override
    public void manualUnfreeze(Long recordId) {
        // 校验记录存在
        CpsFreezeRecordDO record = freezeRecordMapper.selectById(recordId);
        if (record == null) {
            throw exception(FREEZE_RECORD_NOT_EXISTS);
        }
        // 只有 frozen 状态才能解冻
        if (!CpsFreezeStatusEnum.FROZEN.getStatus().equals(record.getStatus())) {
            throw exception(FREEZE_RECORD_STATUS_INVALID);
        }
        // 更新为已解冻
        CpsFreezeRecordDO update = new CpsFreezeRecordDO();
        update.setId(recordId);
        update.setStatus(CpsFreezeStatusEnum.UNFREEZED.getStatus());
        update.setActualUnfreezeTime(LocalDateTime.now());
        freezeRecordMapper.updateById(update);
    }

    @Override
    public PageResult<CpsFreezeRecordDO> getFreezeRecordPage(CpsFreezeRecordPageReqVO reqVO) {
        return freezeRecordMapper.selectPage(reqVO);
    }

    // ==================== 私有方法 ====================

    private void validateFreezeConfigExists(Long id) {
        if (freezeConfigMapper.selectById(id) == null) {
            throw exception(FREEZE_CONFIG_NOT_EXISTS);
        }
    }

}
