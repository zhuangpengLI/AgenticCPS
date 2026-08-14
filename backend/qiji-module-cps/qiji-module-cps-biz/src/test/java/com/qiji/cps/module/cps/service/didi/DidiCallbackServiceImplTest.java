package com.qiji.cps.module.cps.service.didi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.controller.openapi.didi.vo.DidiOrderCallbackReqVO;
import com.qiji.cps.module.cps.controller.openapi.didi.vo.DidiRewardCallbackReqVO;
import com.qiji.cps.module.cps.dal.dataobject.didi.CpsDidiCallbackEventDO;
import com.qiji.cps.module.cps.dal.mysql.didi.CpsDidiCallbackEventMapper;
import com.qiji.cps.module.cps.dal.mysql.vendor.CpsApiVendorMapper;
import com.qiji.cps.module.cps.service.order.CpsOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DidiCallbackServiceImplTest {
    @Mock CpsApiVendorMapper vendorMapper;
    @Mock CpsDidiCallbackEventMapper eventMapper;
    @Mock CpsOrderService orderService;
    @Mock DidiCallbackSignatureVerifier verifier;
    private DidiCallbackServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DidiCallbackServiceImpl(vendorMapper, eventMapper, orderService, verifier, new ObjectMapper());
    }

    @Test
    void orderCallbackMapsPaidOrderAndCommission() {
        DidiOrderCallbackReqVO request = new DidiOrderCallbackReqVO();
        request.setOrderId("o-1"); request.setOrderStatus(2); request.setStatus(2);
        request.setPayPrice(1234L); request.setCpaProfit(100L); request.setCpsProfit(23L);
        request.setPayTime(1_700_000_000L); request.setPromotionId("p-1"); request.setSourceId("member-88");
        when(eventMapper.selectByIdempotencyKey(any())).thenReturn(CpsDidiCallbackEventDO.builder().id(9L).build());

        assertTrue(service.processOrder("app", "{}", request));

        ArgumentCaptor<CpsOrderDTO> captor = ArgumentCaptor.forClass(CpsOrderDTO.class);
        verify(orderService).saveOrUpdateOrder(captor.capture());
        assertEquals("didi", captor.getValue().getPlatformCode());
        assertEquals(new BigDecimal("12.34"), captor.getValue().getFinalPrice());
        assertEquals(new BigDecimal("1.23"), captor.getValue().getCommissionAmount());
        assertEquals(1, captor.getValue().getPlatformStatus());
        assertEquals("member-88", captor.getValue().getExternalId());
    }

    @Test
    void repeatedRewardTraceIsAcknowledgedWithoutSecondInsert() {
        DidiRewardCallbackReqVO request = new DidiRewardCallbackReqVO();
        request.setTraceId("trace-1"); request.setCallbackInfo("{\"source_id\":\"88\"}");
        doThrow(new DuplicateKeyException("duplicate")).when(eventMapper)
                .insert(any(CpsDidiCallbackEventDO.class));

        assertTrue(service.processReward("app", "{}", request));
        verify(eventMapper, times(1)).insert(any(CpsDidiCallbackEventDO.class));
    }

    @Test
    void failedOrderEventCanBeRetried() {
        DidiOrderCallbackReqVO request = new DidiOrderCallbackReqVO();
        request.setOrderId("retry-order"); request.setOrderStatus(2); request.setStatus(2);
        when(eventMapper.selectByIdempotencyKey(any())).thenReturn(
                CpsDidiCallbackEventDO.builder().id(10L).processStatus("FAILED").build(),
                CpsDidiCallbackEventDO.builder().id(10L).processStatus("FAILED").build());

        assertTrue(service.processOrder("app", "{}", request));

        verify(orderService).saveOrUpdateOrder(any(CpsOrderDTO.class));
        verify(eventMapper, never()).insert(any(CpsDidiCallbackEventDO.class));
    }
}
