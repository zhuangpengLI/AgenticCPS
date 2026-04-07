package cn.iocoder.yudao.module.cps.service.platform;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.cps.controller.admin.platform.vo.CpsPlatformPageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.platform.CpsPlatformDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * CPS平台配置 Service 接口
 *
 * @author CPS System
 */
public interface CpsPlatformService {

    /**
     * 创建CPS平台配置
     */
    Long createPlatform(@Valid CpsPlatformSaveReqVO createReqVO);

    /**
     * 更新CPS平台配置
     */
    void updatePlatform(@Valid CpsPlatformSaveReqVO updateReqVO);

    /**
     * 删除CPS平台配置
     */
    void deletePlatform(Long id);

    /**
     * 获取CPS平台配置
     */
    CpsPlatformDO getPlatform(Long id);

    /**
     * 获取CPS平台配置分页
     */
    PageResult<CpsPlatformDO> getPlatformPage(CpsPlatformPageReqVO pageReqVO);

    /**
     * 获取已启用的平台列表
     */
    List<CpsPlatformDO> getEnabledPlatformList();

    /**
     * 根据平台编码获取平台配置
     */
    CpsPlatformDO getPlatformByCode(String platformCode);

}
