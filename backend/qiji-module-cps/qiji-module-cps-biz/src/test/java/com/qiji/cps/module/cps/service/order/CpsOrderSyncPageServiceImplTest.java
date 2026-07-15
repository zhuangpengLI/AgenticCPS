package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsOrderSyncPageServiceImplTest {

    private CpsOrderSyncPageServiceImpl pageService;

    @Mock
    private CpsOrderService orderService;

    @BeforeEach
    void setUp() {
        pageService = new CpsOrderSyncPageServiceImpl();
        ReflectionTestUtils.setField(pageService, "orderService", orderService);
    }

    @Test
    void persistPage_deduplicatesRepeatedPlatformOrderInSamePageBeforeSaving() {
        CpsOrderDTO first = order("jd", "JD-1001");
        CpsOrderDTO duplicate = order("jd", "JD-1001");
        CpsOrderDTO otherPlatformSameId = order("taobao", "JD-1001");
        when(orderService.saveOrUpdateOrder(first)).thenReturn(1);
        when(orderService.saveOrUpdateOrder(otherPlatformSameId)).thenReturn(1);

        int[] stats = pageService.persistPage(List.of(first, duplicate, otherPlatformSameId));

        assertArrayEquals(new int[]{2, 0, 1}, stats);
        verify(orderService, times(1)).saveOrUpdateOrder(first);
        verify(orderService, times(1)).saveOrUpdateOrder(otherPlatformSameId);
    }

    private CpsOrderDTO order(String platformCode, String platformOrderId) {
        return CpsOrderDTO.builder()
                .platformCode(platformCode)
                .platformOrderId(platformOrderId)
                .build();
    }
}
