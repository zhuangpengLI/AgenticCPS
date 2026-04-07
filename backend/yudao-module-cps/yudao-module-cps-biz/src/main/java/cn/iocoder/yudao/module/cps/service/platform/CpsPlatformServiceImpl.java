package cn.iocoder.yudao.module.cps.service.platform;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.cps.controller.admin.platform.vo.CpsPlatformPageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.platform.CpsPlatformDO;
import cn.iocoder.yudao.module.cps.dal.mysql.platform.CpsPlatformMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.cps.enums.CpsErrorCodeConstants.*;

/**
 * CPS平台配置 Service 实现类
 *
 * @author CPS System
 */
@Service
@Validated
public class CpsPlatformServiceImpl implements CpsPlatformService {

    @Resource
    private CpsPlatformMapper platformMapper;

    @Override
    public Long createPlatform(CpsPlatformSaveReqVO createReqVO) {
        // 校验平台编码唯一
        validatePlatformCodeUnique(null, createReqVO.getPlatformCode());
        // 插入
        CpsPlatformDO platform = BeanUtils.toBean(createReqVO, CpsPlatformDO.class);
        platformMapper.insert(platform);
        return platform.getId();
    }

    @Override
    public void updatePlatform(CpsPlatformSaveReqVO updateReqVO) {
        // 校验存在
        validatePlatformExists(updateReqVO.getId());
        // 校验平台编码唯一
        validatePlatformCodeUnique(updateReqVO.getId(), updateReqVO.getPlatformCode());
        // 更新
        CpsPlatformDO updateObj = BeanUtils.toBean(updateReqVO, CpsPlatformDO.class);
        platformMapper.updateById(updateObj);
    }

    @Override
    public void deletePlatform(Long id) {
        // 校验存在
        validatePlatformExists(id);
        // 删除
        platformMapper.deleteById(id);
    }

    @Override
    public CpsPlatformDO getPlatform(Long id) {
        return platformMapper.selectById(id);
    }

    @Override
    public PageResult<CpsPlatformDO> getPlatformPage(CpsPlatformPageReqVO pageReqVO) {
        return platformMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CpsPlatformDO> getEnabledPlatformList() {
        return platformMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public CpsPlatformDO getPlatformByCode(String platformCode) {
        return platformMapper.selectByPlatformCode(platformCode);
    }

    private void validatePlatformExists(Long id) {
        if (platformMapper.selectById(id) == null) {
            throw exception(PLATFORM_NOT_EXISTS);
        }
    }

    private void validatePlatformCodeUnique(Long id, String platformCode) {
        CpsPlatformDO platform = platformMapper.selectByPlatformCode(platformCode);
        if (platform == null) {
            return;
        }
        if (id == null || !id.equals(platform.getId())) {
            throw exception(PLATFORM_CODE_DUPLICATE, platformCode);
        }
    }

}
