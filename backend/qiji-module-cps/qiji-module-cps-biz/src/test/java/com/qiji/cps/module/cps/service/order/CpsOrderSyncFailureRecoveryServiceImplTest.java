package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncFailureDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncFailureMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsOrderSyncFailureRecoveryServiceImplTest {

    @InjectMocks
    private CpsOrderSyncFailureRecoveryServiceImpl service;

    @Mock
    private CpsOrderSyncFailureMapper failureMapper;

    @Test
    void recordFailure_insertsRetryableFailureWithMaskedRawSummaryAndIdempotencyKey() {
        CpsOrderSyncFailureRecordCommand command = command("order-sync:jd:jingdong:0:4:window:page-1");
        when(failureMapper.selectByIdempotencyKey(command.idempotencyKey())).thenReturn(null);

        service.recordFailure(command);

        ArgumentCaptor<CpsOrderSyncFailureDO> captor = ArgumentCaptor.forClass(CpsOrderSyncFailureDO.class);
        verify(failureMapper).insert(captor.capture());
        CpsOrderSyncFailureDO failure = captor.getValue();
        assertEquals("PENDING", failure.getStatus());
        assertEquals(0, failure.getRetryCount());
        assertEquals(3, failure.getMaxRetryCount());
        assertEquals(command.idempotencyKey(), failure.getIdempotencyKey());
        assertTrue(failure.getRawSummary().contains("***"));
        assertTrue(!failure.getRawSummary().contains("13800138000"));
        assertTrue(!failure.getRawSummary().contains("secret-token"));
        assertNotNull(failure.getNextRetryTime());
    }

    @Test
    void recordFailure_ignoresExistingIdempotencyKey() {
        CpsOrderSyncFailureRecordCommand command = command("same-batch");
        when(failureMapper.selectByIdempotencyKey(command.idempotencyKey()))
                .thenReturn(CpsOrderSyncFailureDO.builder().id(1L).build());

        service.recordFailure(command);

        verify(failureMapper, never()).insert(any(CpsOrderSyncFailureDO.class));
    }

    @Test
    void replayFailure_queuesImmediateManualReplayWithAuditFields() {
        CpsOrderSyncFailureDO failure = CpsOrderSyncFailureDO.builder()
                .id(7L).status("PENDING").retryCount(1).maxRetryCount(3).version(0).build();
        when(failureMapper.selectById(7L)).thenReturn(failure);

        service.replayFailure(7L, 1001L, "manual replay");

        ArgumentCaptor<CpsOrderSyncFailureDO> captor = ArgumentCaptor.forClass(CpsOrderSyncFailureDO.class);
        verify(failureMapper).updateById(captor.capture());
        CpsOrderSyncFailureDO update = captor.getValue();
        assertEquals("RETRYING", update.getStatus());
        assertEquals(2, update.getRetryCount());
        assertEquals(1001L, update.getReplayOperatorId());
        assertEquals("manual replay", update.getReplayAuditNote());
        assertNotNull(update.getLastReplayTime());
        assertNotNull(update.getNextRetryTime());
    }

    @Test
    void replayFailure_movesToDeadLetterWhenRetryLimitReached() {
        CpsOrderSyncFailureDO failure = CpsOrderSyncFailureDO.builder()
                .id(8L).status("RETRYING").retryCount(2).maxRetryCount(3).version(1).build();
        when(failureMapper.selectById(8L)).thenReturn(failure);

        service.replayFailure(8L, 1001L, "third failure");

        ArgumentCaptor<CpsOrderSyncFailureDO> captor = ArgumentCaptor.forClass(CpsOrderSyncFailureDO.class);
        verify(failureMapper).updateById(captor.capture());
        assertEquals("DEAD", captor.getValue().getStatus());
        assertEquals(3, captor.getValue().getRetryCount());
        assertEquals(null, captor.getValue().getNextRetryTime());
    }

    @Test
    void scheduleNextRetry_usesExponentialBackoffBeforeDeadLetter() {
        CpsOrderSyncFailureDO failure = CpsOrderSyncFailureDO.builder()
                .id(9L).status("PENDING").retryCount(1).maxRetryCount(3)
                .failureReason("vendor timeout").version(2).build();
        when(failureMapper.selectById(9L)).thenReturn(failure);

        service.scheduleNextRetry(9L, "vendor timeout again");

        ArgumentCaptor<CpsOrderSyncFailureDO> captor = ArgumentCaptor.forClass(CpsOrderSyncFailureDO.class);
        verify(failureMapper).updateById(captor.capture());
        CpsOrderSyncFailureDO update = captor.getValue();
        assertEquals("RETRYING", update.getStatus());
        assertEquals(2, update.getRetryCount());
        assertTrue(update.getNextRetryTime().isAfter(LocalDateTime.now()));
        assertEquals("vendor timeout again", update.getFailureReason());
    }

    private CpsOrderSyncFailureRecordCommand command(String batchNo) {
        return CpsOrderSyncFailureRecordCommand.builder()
                .platformCode("jd")
                .vendorCode("jingdong")
                .orderScene(0)
                .queryType("4")
                .paginationMode("PAGE")
                .pageNo(1)
                .syncBatchNo(batchNo)
                .failureStage("PERSIST_PAGE")
                .requestSnapshot("pageNo=1,pageSize=50")
                .rawSummary("{\"mobile\":\"13800138000\",\"accessToken\":\"secret-token\"}")
                .failureReason("db rejected")
                .idempotencyKey("order-sync-failure:" + batchNo)
                .build();
    }
}
