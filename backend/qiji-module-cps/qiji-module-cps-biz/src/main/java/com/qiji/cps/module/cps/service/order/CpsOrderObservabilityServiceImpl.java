package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderAttributionLogPageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncFailurePageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncCheckpointPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderAttributionLogDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncFailureDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncCheckpointDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderAttributionLogMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncFailureMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncCheckpointMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class CpsOrderObservabilityServiceImpl implements CpsOrderObservabilityService {

    @Resource
    private CpsOrderAttributionLogMapper attributionLogMapper;

    @Resource
    private CpsOrderSyncCheckpointMapper syncCheckpointMapper;

    @Resource
    private CpsOrderSyncFailureMapper syncFailureMapper;

    @Override
    public PageResult<CpsOrderAttributionLogDO> getAttributionLogPage(CpsOrderAttributionLogPageReqVO reqVO) {
        return attributionLogMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<CpsOrderSyncCheckpointDO> getSyncCheckpointPage(CpsOrderSyncCheckpointPageReqVO reqVO) {
        return syncCheckpointMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<CpsOrderSyncFailureDO> getSyncFailurePage(CpsOrderSyncFailurePageReqVO reqVO) {
        return syncFailureMapper.selectPage(reqVO);
    }
}
