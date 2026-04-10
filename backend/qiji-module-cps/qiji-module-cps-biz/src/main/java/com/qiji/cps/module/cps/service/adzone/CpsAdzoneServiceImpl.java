package com.qiji.cps.module.cps.service.adzone;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzonePageReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.*;

/**
 * CPS推广位 Service 实现类
 *
 * @author CPS System
 */
@Service
@Validated
public class CpsAdzoneServiceImpl implements CpsAdzoneService {

    @Resource
    private CpsAdzoneMapper adzoneMapper;

    @Override
    public Long createAdzone(CpsAdzoneSaveReqVO createReqVO) {
        CpsAdzoneDO adzone = BeanUtils.toBean(createReqVO, CpsAdzoneDO.class);
        adzoneMapper.insert(adzone);
        return adzone.getId();
    }

    @Override
    public void updateAdzone(CpsAdzoneSaveReqVO updateReqVO) {
        validateAdzoneExists(updateReqVO.getId());
        CpsAdzoneDO updateObj = BeanUtils.toBean(updateReqVO, CpsAdzoneDO.class);
        adzoneMapper.updateById(updateObj);
    }

    @Override
    public void deleteAdzone(Long id) {
        validateAdzoneExists(id);
        adzoneMapper.deleteById(id);
    }

    @Override
    public CpsAdzoneDO getAdzone(Long id) {
        return adzoneMapper.selectById(id);
    }

    @Override
    public PageResult<CpsAdzoneDO> getAdzonePage(CpsAdzonePageReqVO pageReqVO) {
        return adzoneMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CpsAdzoneDO> getAdzoneListByPlatformCode(String platformCode) {
        return adzoneMapper.selectListByPlatformCode(platformCode);
    }

    private void validateAdzoneExists(Long id) {
        if (adzoneMapper.selectById(id) == null) {
            throw exception(ADZONE_NOT_EXISTS);
        }
    }

}
