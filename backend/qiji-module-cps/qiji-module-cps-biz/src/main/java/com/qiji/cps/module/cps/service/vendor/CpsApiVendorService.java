package com.qiji.cps.module.cps.service.vendor;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.controller.admin.vendor.vo.CpsApiVendorPageReqVO;
import com.qiji.cps.module.cps.controller.admin.vendor.vo.CpsApiVendorSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.vendor.CpsApiVendorDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * CPS API 供应商配置 Service 接口
 *
 * @author CPS System
 */
public interface CpsApiVendorService {

    /**
     * 创建供应商配置
     */
    Long createVendor(@Valid CpsApiVendorSaveReqVO createReqVO);

    /**
     * 更新供应商配置
     */
    void updateVendor(@Valid CpsApiVendorSaveReqVO updateReqVO);

    /**
     * 删除供应商配置
     */
    void deleteVendor(Long id);

    /**
     * 获取供应商配置
     */
    CpsApiVendorDO getVendor(Long id);

    /**
     * 获取供应商配置分页
     */
    PageResult<CpsApiVendorDO> getVendorPage(CpsApiVendorPageReqVO pageReqVO);

    /**
     * 根据供应商编码和平台编码获取供应商配置
     */
    CpsApiVendorDO getVendorByCodeAndPlatform(String vendorCode, String platformCode);

    /**
     * 获取指定平台的所有已启用供应商列表
     */
    List<CpsApiVendorDO> getEnabledVendorsByPlatform(String platformCode);

    /**
     * 获取所有已启用的供应商列表
     */
    List<CpsApiVendorDO> getEnabledVendorList();

    /**
     * 构建供应商运行时配置 DTO（从 DO 转换为传入客户端的配置）
     */
    CpsVendorConfig buildVendorConfig(CpsApiVendorDO vendorDO);

    /**
     * 根据供应商编码和平台编码获取运行时配置
     */
    CpsVendorConfig getVendorConfig(String vendorCode, String platformCode);

}
