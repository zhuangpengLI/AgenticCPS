package cn.iocoder.yudao.module.cps.service.freeze;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.cps.controller.admin.freeze.vo.CpsFreezeConfigPageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.freeze.vo.CpsFreezeConfigSaveReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.freeze.vo.CpsFreezeRecordPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO;
import cn.iocoder.yudao.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;

/**
 * CPS 冻结解冻 Service 接口
 *
 * <p>提供冻结配置 CRUD 管理、批量自动解冻、手动解冻等功能。</p>
 *
 * @author CPS System
 */
public interface CpsFreezeService {

    // ==================== 配置管理 ====================

    /**
     * 创建冻结配置
     *
     * @param reqVO 创建信息
     * @return 配置ID
     */
    Long createFreezeConfig(CpsFreezeConfigSaveReqVO reqVO);

    /**
     * 更新冻结配置
     *
     * @param reqVO 更新信息
     */
    void updateFreezeConfig(CpsFreezeConfigSaveReqVO reqVO);

    /**
     * 删除冻结配置
     *
     * @param id 配置ID
     */
    void deleteFreezeConfig(Long id);

    /**
     * 分页查询冻结配置
     *
     * @param reqVO 分页请求
     * @return 分页结果
     */
    PageResult<CpsFreezeConfigDO> getFreezeConfigPage(CpsFreezeConfigPageReqVO reqVO);

    /**
     * 获取指定平台的启用配置（优先取平台专属，其次取全平台默认）
     *
     * @param platformCode 平台编码
     * @return 启用的冻结配置，可能为 null
     */
    CpsFreezeConfigDO getActiveConfig(String platformCode);

    // ==================== 解冻操作 ====================

    /**
     * 自动批量解冻（定时任务调用）
     * <p>扫描已到达解冻时间且状态为 frozen 的记录，批量执行解冻操作。</p>
     *
     * @param batchSize 每批处理数量
     * @return 本次成功解冻的记录数
     */
    int batchUnfreeze(int batchSize);

    /**
     * 手动解冻指定记录（管理员操作）
     *
     * @param recordId 冻结记录ID
     */
    void manualUnfreeze(Long recordId);

    /**
     * 分页查询冻结记录
     *
     * @param reqVO 分页请求
     * @return 分页结果
     */
    PageResult<CpsFreezeRecordDO> getFreezeRecordPage(CpsFreezeRecordPageReqVO reqVO);

}
