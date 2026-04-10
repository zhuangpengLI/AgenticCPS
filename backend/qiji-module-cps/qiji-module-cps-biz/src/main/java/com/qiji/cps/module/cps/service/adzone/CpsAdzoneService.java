package com.qiji.cps.module.cps.service.adzone;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzonePageReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import jakarta.validation.Valid;

import java.util.List;

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

}
