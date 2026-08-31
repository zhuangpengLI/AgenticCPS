package com.qiji.cps.module.cps.service.order;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Builds deterministic, bounded upstream order query windows.
 *
 * <p>Dataoke limits ordinary windows to three hours and promotion-day windows
 * to twenty minutes. A small overlap is intentional: it protects the boundary
 * between adjacent requests; order persistence must remain idempotent.</p>
 */
public final class CpsOrderSyncWindowPlanner {

    public static final Duration ORDINARY_MAX = Duration.ofHours(3);
    public static final Duration PROMOTION_MAX = Duration.ofMinutes(20);
    public static final Duration DEFAULT_OVERLAP = Duration.ofMinutes(5);

    private CpsOrderSyncWindowPlanner() {
    }

    public static List<Window> plan(LocalDateTime start, LocalDateTime end) {
        return plan(start, end, DEFAULT_OVERLAP, date -> false);
    }

    public static List<Window> plan(LocalDateTime start, LocalDateTime end,
                                    Duration overlap,
                                    Predicate<LocalDate> promotionDay) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start must not be after end");
        }
        Duration effectiveOverlap = overlap == null ? DEFAULT_OVERLAP : overlap;
        if (effectiveOverlap.isNegative() || effectiveOverlap.compareTo(Duration.ofMinutes(20)) > 0) {
            throw new IllegalArgumentException("overlap must be between 0 and 20 minutes");
        }
        Predicate<LocalDate> effectivePromotionDay = promotionDay == null ? date -> false : promotionDay;
        if (start.equals(end)) {
            return List.of(new Window(start, end, effectivePromotionDay.test(start.toLocalDate())));
        }

        List<Window> windows = new ArrayList<>();
        LocalDateTime cursor = start;
        while (cursor.isBefore(end)) {
            boolean promotion = effectivePromotionDay.test(cursor.toLocalDate());
            Duration max = promotion ? PROMOTION_MAX : ORDINARY_MAX;
            LocalDateTime windowEnd = cursor.plus(max);
            if (windowEnd.isAfter(end)) {
                windowEnd = end;
            }
            windows.add(new Window(cursor, windowEnd, promotion));
            if (windowEnd.equals(end)) {
                break;
            }
            LocalDateTime next = windowEnd.minus(effectiveOverlap);
            // An overlap equal to the maximum window must still make progress.
            cursor = next.isAfter(cursor) ? next : windowEnd;
        }
        return List.copyOf(windows);
    }

    public record Window(LocalDateTime start, LocalDateTime end, boolean promotionDay) {
        public Window {
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("window start must not be after end");
            }
        }

        public Duration duration() {
            return Duration.between(start, end);
        }
    }
}
