package com.qiji.cps.module.cps.service.order;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CpsOrderSyncWindowPlannerTest {

    @Test
    void plansOrdinaryRangeWithBoundedWindowsAndOverlap() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = start.plusHours(8);

        List<CpsOrderSyncWindowPlanner.Window> windows =
                CpsOrderSyncWindowPlanner.plan(start, end);

        assertEquals(3, windows.size());
        windows.forEach(window -> assertTrue(window.duration().compareTo(Duration.ofHours(3)) <= 0));
        assertEquals(start, windows.get(0).start());
        assertEquals(end, windows.get(windows.size() - 1).end());
        assertEquals(windows.get(0).end().minusMinutes(5), windows.get(1).start());
    }

    @Test
    void promotionDayUsesTwentyMinuteWindows() {
        LocalDate promotion = LocalDate.of(2026, 11, 11);
        List<CpsOrderSyncWindowPlanner.Window> windows = CpsOrderSyncWindowPlanner.plan(
                promotion.atStartOfDay(), promotion.atStartOfDay().plusHours(1),
                Duration.ofMinutes(5), promotion::equals);

        assertEquals(4, windows.size());
        windows.forEach(window -> {
            assertTrue(window.promotionDay());
            assertTrue(window.duration().compareTo(Duration.ofMinutes(20)) <= 0);
        });
    }

    @Test
    void rejectsInvalidRangeAndOverlap() {
        LocalDateTime now = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class, () ->
                CpsOrderSyncWindowPlanner.plan(now.plusMinutes(1), now));
        assertThrows(IllegalArgumentException.class, () ->
                CpsOrderSyncWindowPlanner.plan(now, now.plusHours(1), Duration.ofMinutes(21), null));
    }
}
