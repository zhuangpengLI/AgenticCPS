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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsOrderObservabilityServiceImplTest {

    @InjectMocks
    private CpsOrderObservabilityServiceImpl service;

    @Mock
    private CpsOrderAttributionLogMapper attributionLogMapper;
    @Mock
    private CpsOrderSyncCheckpointMapper syncCheckpointMapper;
    @Mock
    private CpsOrderSyncFailureMapper syncFailureMapper;

    @Test
    void getAttributionLogPage_delegatesFiltersToMapper() {
        CpsOrderAttributionLogPageReqVO request = new CpsOrderAttributionLogPageReqVO();
        request.setPlatformCode("taobao");
        request.setResult("REJECTED");
        PageResult<CpsOrderAttributionLogDO> expected = new PageResult<>(List.of(), 0L);
        when(attributionLogMapper.selectPage(request)).thenReturn(expected);

        assertSame(expected, service.getAttributionLogPage(request));

        verify(attributionLogMapper).selectPage(request);
    }

    @Test
    void getSyncCheckpointPage_delegatesFiltersToMapper() {
        CpsOrderSyncCheckpointPageReqVO request = new CpsOrderSyncCheckpointPageReqVO();
        request.setPlatformCode("taobao");
        request.setLastSyncStatus("PARTIAL");
        PageResult<CpsOrderSyncCheckpointDO> expected = new PageResult<>(List.of(), 0L);
        when(syncCheckpointMapper.selectPage(request)).thenReturn(expected);

        assertSame(expected, service.getSyncCheckpointPage(request));

        verify(syncCheckpointMapper).selectPage(request);
    }

    @Test
    void getSyncFailurePage_delegatesFiltersToMapper() {
        CpsOrderSyncFailurePageReqVO request = new CpsOrderSyncFailurePageReqVO();
        request.setPlatformCode("jd");
        request.setStatus("PENDING");
        PageResult<CpsOrderSyncFailureDO> expected = new PageResult<>(List.of(), 0L);
        when(syncFailureMapper.selectPage(request)).thenReturn(expected);

        assertSame(expected, service.getSyncFailurePage(request));

        verify(syncFailureMapper).selectPage(request);
    }
}
