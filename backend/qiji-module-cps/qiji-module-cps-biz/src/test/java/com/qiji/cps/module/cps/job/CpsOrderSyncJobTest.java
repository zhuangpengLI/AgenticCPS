package com.qiji.cps.module.cps.job;

import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderPageResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncCheckpointDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncLogDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncCheckpointMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncLogMapper;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncFailureRecordCommand;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncFailureRecoveryService;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncPageService;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsOrderSyncJobTest {

    private CpsOrderSyncJob job;

    @Mock
    private CpsPlatformService platformService;
    @Mock
    private CpsPlatformClientFactory platformClientFactory;
    @Mock
    private CpsPlatformClient client;
    @Mock
    private CpsOrderSyncPageService pageService;
    @Mock
    private CpsOrderSyncLogMapper syncLogMapper;
    @Mock
    private CpsOrderSyncCheckpointMapper checkpointMapper;
    @Mock
    private CpsOrderSyncFailureRecoveryService failureRecoveryService;

    @BeforeEach
    void setUp() {
        job = new CpsOrderSyncJob();
        ReflectionTestUtils.setField(job, "platformService", platformService);
        ReflectionTestUtils.setField(job, "platformClientFactory", platformClientFactory);
        ReflectionTestUtils.setField(job, "pageService", pageService);
        ReflectionTestUtils.setField(job, "syncLogMapper", syncLogMapper);
        ReflectionTestUtils.setField(job, "checkpointMapper", checkpointMapper);
        ReflectionTestUtils.setField(job, "failureRecoveryService", failureRecoveryService);

        CpsPlatformDO platform = CpsPlatformDO.builder()
                .platformCode("jd")
                .activeVendorCode("jingdong")
                .build();
        lenient().when(platformService.getEnabledPlatformList()).thenReturn(List.of(platform));
        lenient().when(platformClientFactory.getRequiredClient("jd")).thenReturn(client);
        lenient().when(pageService.persistPage(anyList())).thenReturn(new int[]{1, 0, 0});
        AtomicLong ids = new AtomicLong(1L);
        lenient().when(checkpointMapper.insert(any(CpsOrderSyncCheckpointDO.class))).thenAnswer(invocation -> {
            CpsOrderSyncCheckpointDO checkpoint = invocation.getArgument(0);
            checkpoint.setId(ids.getAndIncrement());
            return 1;
        });
        lenient().when(checkpointMapper.updateById(any(CpsOrderSyncCheckpointDO.class))).thenReturn(1);
    }

    @Test
    void pagePaginationPersistsEachPageBeforeAdvancingCheckpoint() throws Exception {
        when(client.queryOrderPage(any())).thenReturn(
                CpsOrderPageResult.page(List.of(order("1")), 2, true),
                CpsOrderPageResult.page(List.of(order("2")), null, false));

        String result = job.execute("");

        verify(pageService, times(2)).persistPage(anyList());
        ArgumentCaptor<CpsOrderQueryRequest> requestCaptor = ArgumentCaptor.forClass(CpsOrderQueryRequest.class);
        verify(client, times(2)).queryOrderPage(requestCaptor.capture());
        assertEquals(List.of(1, 2), requestCaptor.getAllValues().stream().map(CpsOrderQueryRequest::getPageNo).toList());
        ArgumentCaptor<CpsOrderSyncCheckpointDO> checkpointCaptor =
                ArgumentCaptor.forClass(CpsOrderSyncCheckpointDO.class);
        verify(checkpointMapper, times(2)).updateById(checkpointCaptor.capture());
        CpsOrderSyncCheckpointDO completed = checkpointCaptor.getValue();
        assertEquals("SUCCESS", completed.getLastSyncStatus());
        assertEquals("jingdong", completed.getVendorCode());
        assertEquals(0, completed.getOrderScene());
        assertEquals("4", completed.getQueryType());
        assertEquals(null, completed.getNextPageNo());
        assertTrue(result.contains("新增2"));
    }

    @Test
    void pagePaginationFetchesMoreThanFiftyOrdersWithoutLoopOrLoss() throws Exception {
        when(client.queryOrderPage(any())).thenReturn(
                CpsOrderPageResult.page(orders("JD-P1-", 25), 2, true),
                CpsOrderPageResult.page(orders("JD-P2-", 25), 3, true),
                CpsOrderPageResult.page(orders("JD-P3-", 10), null, false));
        List<CpsOrderDTO> persistedOrders = new ArrayList<>();
        when(pageService.persistPage(anyList())).thenAnswer(invocation -> {
            List<CpsOrderDTO> pageOrders = invocation.getArgument(0);
            persistedOrders.addAll(pageOrders);
            return new int[]{pageOrders.size(), 0, 0};
        });

        String result = job.execute("");

        ArgumentCaptor<CpsOrderQueryRequest> requestCaptor = ArgumentCaptor.forClass(CpsOrderQueryRequest.class);
        verify(client, times(3)).queryOrderPage(requestCaptor.capture());
        assertEquals(List.of(1, 2, 3), requestCaptor.getAllValues().stream().map(CpsOrderQueryRequest::getPageNo).toList());
        assertEquals(List.of(50, 50, 50), requestCaptor.getAllValues().stream().map(CpsOrderQueryRequest::getPageSize).toList());
        assertEquals(60, persistedOrders.size());
        Set<String> orderIds = new HashSet<>(persistedOrders.stream().map(CpsOrderDTO::getPlatformOrderId).toList());
        assertEquals(60, orderIds.size());
        assertTrue(result.contains("新增60"));
    }

    @Test
    void cursorPaginationUsesExplicitNextCursor() throws Exception {
        when(client.queryOrderPage(any())).thenReturn(
                CpsOrderPageResult.cursor(List.of(order("1")), "cursor-2", true),
                CpsOrderPageResult.cursor(List.of(order("2")), null, false));

        job.execute("");

        ArgumentCaptor<CpsOrderQueryRequest> requestCaptor = ArgumentCaptor.forClass(CpsOrderQueryRequest.class);
        verify(client, times(2)).queryOrderPage(requestCaptor.capture());
        assertEquals(null, requestCaptor.getAllValues().get(0).getPositionIndex());
        assertEquals("cursor-2", requestCaptor.getAllValues().get(1).getPositionIndex());
    }

    @Test
    void upstreamExceptionDoesNotPersistAnEmptyPageOrAdvanceCheckpoint() throws Exception {
        when(client.queryOrderPage(any())).thenThrow(new IllegalStateException("vendor unavailable"));

        job.execute("");

        verify(pageService, never()).persistPage(anyList());
        ArgumentCaptor<CpsOrderSyncCheckpointDO> checkpointCaptor =
                ArgumentCaptor.forClass(CpsOrderSyncCheckpointDO.class);
        verify(checkpointMapper).updateById(checkpointCaptor.capture());
        assertEquals("FAILED", checkpointCaptor.getValue().getLastSyncStatus());
        assertEquals(1, checkpointCaptor.getValue().getNextPageNo());
        ArgumentCaptor<CpsOrderSyncLogDO> logCaptor = ArgumentCaptor.forClass(CpsOrderSyncLogDO.class);
        verify(syncLogMapper).insert(logCaptor.capture());
        assertEquals(2, logCaptor.getValue().getSyncStatus());
        assertTrue(logCaptor.getValue().getErrorMsg().contains("vendor unavailable"));
    }

    @Test
    void failedPageLeavesCheckpointAtThatPageAndMarksPartialSuccess() throws Exception {
        when(client.queryOrderPage(any())).thenReturn(
                CpsOrderPageResult.page(List.of(order("1")), 2, true),
                CpsOrderPageResult.page(List.of(order("2")), null, false));
        when(pageService.persistPage(anyList())).thenAnswer(invocation -> {
            List<CpsOrderDTO> orders = invocation.getArgument(0);
            if ("2".equals(orders.get(0).getPlatformOrderId())) {
                throw new IllegalStateException("db rejected order 2");
            }
            return new int[]{1, 0, 0};
        });

        job.execute("");

        ArgumentCaptor<CpsOrderSyncCheckpointDO> checkpointCaptor =
                ArgumentCaptor.forClass(CpsOrderSyncCheckpointDO.class);
        verify(checkpointMapper, times(2)).updateById(checkpointCaptor.capture());
        CpsOrderSyncCheckpointDO partial = checkpointCaptor.getValue();
        assertEquals("PARTIAL", partial.getLastSyncStatus());
        assertEquals(2, partial.getNextPageNo());
        assertTrue(partial.getFailureSummary().contains("db rejected order 2"));
        ArgumentCaptor<CpsOrderSyncLogDO> logCaptor = ArgumentCaptor.forClass(CpsOrderSyncLogDO.class);
        verify(syncLogMapper).insert(logCaptor.capture());
        assertEquals(3, logCaptor.getValue().getSyncStatus());
        assertEquals(1, logCaptor.getValue().getNewCount());
    }

    @Test
    void failedPageRecordsRetryableFailureDetailWithSanitizedRawSummary() throws Exception {
        CpsOrderDTO order = order("2");
        order.setRawPayload("{\"mobile\":\"13800138000\",\"accessToken\":\"secret-token\",\"buyer\":\"alice\"}");
        when(client.queryOrderPage(any())).thenReturn(CpsOrderPageResult.page(List.of(order), null, false));
        when(pageService.persistPage(anyList())).thenThrow(new IllegalStateException("db rejected order 2"));

        job.execute("");

        ArgumentCaptor<CpsOrderSyncFailureRecordCommand> failureCaptor =
                ArgumentCaptor.forClass(CpsOrderSyncFailureRecordCommand.class);
        verify(failureRecoveryService).recordFailure(failureCaptor.capture());
        CpsOrderSyncFailureRecordCommand failure = failureCaptor.getValue();
        assertEquals("jd", failure.platformCode());
        assertEquals("jingdong", failure.vendorCode());
        assertEquals("PERSIST_PAGE", failure.failureStage());
        assertEquals(1, failure.pageNo());
        assertTrue(failure.syncBatchNo().startsWith("order-sync:jd:jingdong:0:4:"));
        assertTrue(failure.failureReason().contains("db rejected order 2"));
        assertTrue(failure.rawSummary().contains("***"));
        assertTrue(!failure.rawSummary().contains("13800138000"));
        assertTrue(!failure.rawSummary().contains("secret-token"));
    }

    @Test
    void maxPageLimitWithMoreDataMarksPartialAndRetainsNextPage() throws Exception {
        when(client.queryOrderPage(any())).thenAnswer(invocation -> {
            CpsOrderQueryRequest request = invocation.getArgument(0);
            return CpsOrderPageResult.page(List.of(order(String.valueOf(request.getPageNo()))),
                    request.getPageNo() + 1, true);
        });

        job.execute("");

        verify(pageService, times(20)).persistPage(anyList());
        ArgumentCaptor<CpsOrderSyncCheckpointDO> checkpointCaptor =
                ArgumentCaptor.forClass(CpsOrderSyncCheckpointDO.class);
        verify(checkpointMapper, times(21)).updateById(checkpointCaptor.capture());
        CpsOrderSyncCheckpointDO partial = checkpointCaptor.getValue();
        assertEquals("PARTIAL", partial.getLastSyncStatus());
        assertEquals(21, partial.getNextPageNo());
        assertTrue(partial.getFailureSummary().contains("20"));
        ArgumentCaptor<CpsOrderSyncLogDO> logCaptor = ArgumentCaptor.forClass(CpsOrderSyncLogDO.class);
        verify(syncLogMapper).insert(logCaptor.capture());
        assertEquals(3, logCaptor.getValue().getSyncStatus());
    }

    @Test
    void taobaoScenesUseIndependentCheckpointKeys() throws Exception {
        CpsPlatformDO taobao = CpsPlatformDO.builder()
                .platformCode("taobao")
                .activeVendorCode("dataoke")
                .build();
        when(platformService.getEnabledPlatformList()).thenReturn(List.of(taobao));
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(client);
        when(client.queryOrderPage(any())).thenReturn(CpsOrderPageResult.page(List.of(), null, false));

        job.execute("");

        verify(checkpointMapper).selectByKey("taobao", "dataoke", 1, "4");
        verify(checkpointMapper).selectByKey("taobao", "dataoke", 2, "4");
        verify(checkpointMapper).selectByKey("taobao", "dataoke", 3, "4");
    }

    @Test
    void resumedPaginationReusesCheckpointQueryEndTimeUntilWindowCompletes() throws Exception {
        LocalDateTime fixedEndTime = LocalDateTime.of(2026, 7, 13, 10, 30);
        CpsOrderSyncCheckpointDO checkpoint = CpsOrderSyncCheckpointDO.builder()
                .id(99L)
                .platformCode("jd")
                .vendorCode("jingdong")
                .orderScene(0)
                .queryType("4")
                .paginationMode("PAGE")
                .nextPageNo(2)
                .watermarkTime(LocalDateTime.of(2026, 7, 13, 8, 30))
                .queryEndTime(fixedEndTime)
                .lastSyncStatus("PARTIAL")
                .version(1)
                .build();
        when(checkpointMapper.selectByKey("jd", "jingdong", 0, "4")).thenReturn(checkpoint);
        when(client.queryOrderPage(any())).thenReturn(CpsOrderPageResult.page(List.of(), null, false));

        job.execute("");

        ArgumentCaptor<CpsOrderQueryRequest> requestCaptor = ArgumentCaptor.forClass(CpsOrderQueryRequest.class);
        verify(client).queryOrderPage(requestCaptor.capture());
        assertEquals("2026-07-13 10:30:00", requestCaptor.getValue().getEndTime());
        assertEquals(2, requestCaptor.getValue().getPageNo());
        ArgumentCaptor<CpsOrderSyncCheckpointDO> checkpointCaptor =
                ArgumentCaptor.forClass(CpsOrderSyncCheckpointDO.class);
        verify(checkpointMapper).updateById(checkpointCaptor.capture());
        assertEquals(fixedEndTime, checkpointCaptor.getValue().getWatermarkTime());
        assertEquals(null, checkpointCaptor.getValue().getQueryEndTime());
    }

    @Test
    void checkpointUpdateFailureIsReportedAsPlatformFailure() throws Exception {
        when(client.queryOrderPage(any())).thenReturn(
                CpsOrderPageResult.page(List.of(order("1")), 2, true));
        when(checkpointMapper.updateById(any(CpsOrderSyncCheckpointDO.class))).thenReturn(0);

        String result = job.execute("");

        ArgumentCaptor<CpsOrderSyncLogDO> logCaptor = ArgumentCaptor.forClass(CpsOrderSyncLogDO.class);
        verify(syncLogMapper).insert(logCaptor.capture());
        assertEquals(2, logCaptor.getValue().getSyncStatus());
        assertTrue(logCaptor.getValue().getErrorMsg().contains("checkpoint"));
        assertTrue(result.contains("失败1平台"));
    }

    @Test
    void enabledPlatformsContinueIndependentlyWhenOneVendorFails() throws Exception {
        CpsPlatformClient failedClient = mock(CpsPlatformClient.class);
        CpsPlatformClient successClient = mock(CpsPlatformClient.class);
        CpsPlatformDO jd = CpsPlatformDO.builder()
                .platformCode("jd")
                .activeVendorCode("jingdong")
                .build();
        CpsPlatformDO pdd = CpsPlatformDO.builder()
                .platformCode("pdd")
                .activeVendorCode("pinduoduo")
                .build();
        when(platformService.getEnabledPlatformList()).thenReturn(List.of(jd, pdd));
        when(platformClientFactory.getRequiredClient("jd")).thenReturn(failedClient);
        when(platformClientFactory.getRequiredClient("pdd")).thenReturn(successClient);
        when(failedClient.queryOrderPage(any())).thenThrow(new IllegalStateException("jd timeout"));
        when(successClient.queryOrderPage(any())).thenReturn(CpsOrderPageResult.page(List.of(order("PDD-1")), null, false));

        String result = job.execute("");

        verify(failedClient).queryOrderPage(any());
        verify(successClient).queryOrderPage(any());
        verify(checkpointMapper).selectByKey("jd", "jingdong", 0, "4");
        verify(checkpointMapper).selectByKey("pdd", "pinduoduo", 0, "4");
        assertTrue(result.contains("成功1平台，失败1平台"));
    }

    private CpsOrderDTO order(String id) {
        return CpsOrderDTO.builder().platformCode("jd").platformOrderId(id).build();
    }

    private List<CpsOrderDTO> orders(String prefix, int count) {
        List<CpsOrderDTO> orders = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            orders.add(order(prefix + i));
        }
        return orders;
    }
}
