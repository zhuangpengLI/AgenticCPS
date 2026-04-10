package com.qiji.cps.module.cps.service.rebate;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateConfigPageReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateConfigSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * CPS返利配置 Service 接口
 *
 * @author CPS System
 */
public interface CpsRebateConfigService {

    /**
     * 创建返利配置
     */
    Long createRebateConfig(@Valid CpsRebateConfigSaveReqVO createReqVO);

    /**
     * 更新返利配置
     */
    void updateRebateConfig(@Valid CpsRebateConfigSaveReqVO updateReqVO);

    /**
     * 删除返利配置
     */
    void deleteRebateConfig(Long id);

    /**
     * 获取返利配置
     */
    CpsRebateConfigDO getRebateConfig(Long id);

    /**
     * 获取返利配置分页
     */
    PageResult<CpsRebateConfigDO> getRebateConfigPage(CpsRebateConfigPageReqVO pageReqVO);

    /**
     * 获取所有已启用的返利配置（按优先级降序，用于返利计算）
     */
    List<CpsRebateConfigDO> getEnabledRebateConfigList();

    /**
     * 根据会员等级和平台编码匹配最优返利配置
     * <p>
     * 匹配优先级：
     * 1. 会员等级 + 平台编码（精确匹配）
     * 2. 会员等级 + 全平台（platformCode=null）
     * 3. 全等级 + 平台编码（memberLevelId=null）
     * 4. 全等级 + 全平台（兜底配置）
     * </p>
     *
     * @param memberLevelId 会员等级ID（null表示无等级）
     * @param platformCode  平台编码（null表示全平台）
     * @return 最优返利配置，null表示无配置
     */
    CpsRebateConfigDO matchRebateConfig(Long memberLevelId, String platformCode);

}
