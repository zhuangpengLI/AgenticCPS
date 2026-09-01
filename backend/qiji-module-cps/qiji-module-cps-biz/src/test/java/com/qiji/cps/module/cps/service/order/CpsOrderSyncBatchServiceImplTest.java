package com.qiji.cps.module.cps.service.order;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncBatchDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncWindowDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncBatchMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncWindowMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CpsOrderSyncBatchServiceImplTest {

    private CpsOrderSyncBatchServiceImpl service;

    @Mock private CpsOrderSyncBatchMapper batchMapper;
    @Mock private CpsOrderSyncWindowMapper windowMapper;
    @Mock private CpsOrderService orderService;

    @BeforeEach
    void setUp() {
        service = new CpsOrderSyncBatchServiceImpl();
        ReflectionTestUtils.setField(service, "batchMapper", batchMapper);
        ReflectionTestUtils.setField(service, "windowMapper", windowMapper);
        ReflectionTestUtils.setField(service, "orderService", orderService);
    }

    @Test
    void executeDueWindows_syncsPendingWindowAndCompletesBatch() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 8, 0);
        LocalDateTime end = start.plusHours(3);
        CpsOrderSyncWindowDO pending = CpsOrderSyncWindowDO.builder().id(11L).batchId(1L)
                .platformCode("taobao").vendorCode("dataoke").queryType(4)
                .windowStart(start).windowEnd(end).status("PENDING").retryCount(0).maxRetryCount(5).build();
        CpsOrderSyncBatchDO batch = CpsOrderSyncBatchDO.builder().id(1L).status("PENDING")
                .totalWindows(1).build();
        CpsOrderSyncWindowDO completed = CpsOrderSyncWindowDO.builder().id(11L).batchId(1L)
                .status("SUCCESS").build();
        when(windowMapper.selectExecutable(any(), eq(1))).thenReturn(List.of(pending));
        when(batchMapper.selectById(1L)).thenReturn(batch);
        when(windowMapper.selectList(any(Wrapper.class))).thenReturn(List.of(completed));

        assertEquals(1, service.executeDueWindows(1));

        verify(orderService).manualSync("taobao", "dataoke", 3, 4, null, start, end);
        verify(windowMapper, times(2)).updateById(org.mockito.ArgumentMatchers.any(CpsOrderSyncWindowDO.class));
        ArgumentCaptor<CpsOrderSyncBatchDO> batchCaptor = ArgumentCaptor.forClass(CpsOrderSyncBatchDO.class);
        verify(batchMapper, times(2)).updateById(batchCaptor.capture());
        CpsOrderSyncBatchDO completedBatch = batchCaptor.getAllValues().get(1);
        assertEquals("SUCCESS", completedBatch.getStatus());
        assertEquals(1, completedBatch.getSuccessWindows());
    }

    @Test
    void deleteBatch_rejectsRunningBatchAndCascadesStoppedBatch() {
        when(batchMapper.selectById(1L)).thenReturn(CpsOrderSyncBatchDO.builder().id(1L).status("RUNNING").build());
        assertThrows(IllegalStateException.class, () -> service.delete(1L));

        when(batchMapper.selectById(2L)).thenReturn(CpsOrderSyncBatchDO.builder().id(2L).status("PAUSED").build());
        service.delete(2L);

        verify(windowMapper).delete(any(Wrapper.class));
        verify(batchMapper).deleteById(2L);
    }

    @Test
    void create_rejectsEpochTimeRange() {
        LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 8, 0);

        assertThrows(IllegalArgumentException.class,
                () -> service.create("jd", "OFFICIAL", "MANUAL", 4, epoch, epoch.plusHours(1)));
    }
}
