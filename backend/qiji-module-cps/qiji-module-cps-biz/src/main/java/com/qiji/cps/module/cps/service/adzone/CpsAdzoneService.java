package com.qiji.cps.module.cps.service.adzone;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneBatchCreateRespVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzonePageReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Set;

/**
 * CPS推广位 Service 接口
 *
 * @author CPS System
 */
public interface CpsAdzoneService {

    /**
     * 创建推广位
     */
    Long createAdzone(@Valid CpsAdzoneSaveReqVO createReqVO);

    /**
     * 批量创建推广位，保留逐条成功/失败结果
     */
    CpsAdzoneBatchCreateRespVO batchCreateAdzones(@Valid List<CpsAdzoneSaveReqVO> items);

    /**
     * 更新推广位
     */
    void updateAdzone(@Valid CpsAdzoneSaveReqVO updateReqVO);

    /**
     * 删除推广位
     */
    void deleteAdzone(Long id);

    /**
     * 获取推广位
     */
    CpsAdzoneDO getAdzone(Long id);

    /**
     * 获取推广位分页
     */
    PageResult<CpsAdzoneDO> getAdzonePage(CpsAdzonePageReqVO pageReqVO);

    /**
     * 获取平台下的推广位列表
     */
    List<CpsAdzoneDO> getAdzoneListByPlatformCode(String platformCode);

    /**
     * 获取平台下全部推广位，包括禁用推广位。
     */
    List<CpsAdzoneDO> getAdzoneListByPlatform(String platformCode);

    /**
     * 按平台编码和推广位 ID 稳定键保存平台接入推广位。
     */
    Long upsertAdzoneForOnboarding(@Valid CpsAdzoneSaveReqVO saveReqVO);

    /**
     * 删除不再由当前平台接入配置管理的推广位。
     */
    void deleteAdzonesNotIn(String platformCode, Set<String> retainedAdzoneIds);

    /**
     * 获取会员在指定平台的启用专属推广位
     */
    CpsAdzoneDO getMemberAdzone(String platformCode, Long memberId);

}
