package com.qiji.cps.module.cps.client.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CpsOrderPageResultTest {

    @Test
    void pageResultCarriesExplicitNextPageContract() {
        CpsOrderPageResult result = CpsOrderPageResult.page(
                List.of(CpsOrderDTO.builder().platformOrderId("P-1").build()), 2, true);

        assertEquals(CpsOrderPaginationMode.PAGE, result.getPaginationMode());
        assertEquals(2, result.getNextPageNo());
        assertNull(result.getNextCursor());
        assertTrue(result.isHasMore());
    }

    @Test
    void cursorResultCarriesExplicitNextCursorContract() {
        CpsOrderPageResult result = CpsOrderPageResult.cursor(
                List.of(CpsOrderDTO.builder().platformOrderId("C-1").build()), "next", true);

        assertEquals(CpsOrderPaginationMode.CURSOR, result.getPaginationMode());
        assertEquals("next", result.getNextCursor());
        assertNull(result.getNextPageNo());
        assertTrue(result.isHasMore());
    }
}
