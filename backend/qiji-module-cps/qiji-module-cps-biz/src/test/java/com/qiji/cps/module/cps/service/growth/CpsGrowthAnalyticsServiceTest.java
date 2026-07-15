package com.qiji.cps.module.cps.service.growth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsGrowthAnalyticsServiceTest {

    private final CpsGrowthAnalyticsService service = new CpsGrowthAnalyticsService();

    @Test
    @DisplayName("calculateRoi keeps ROI traceable to immutable events, trusted orders, settlement, and asset ledgers")
    void calculateRoi_keepsTraceableFinancialInputs() {
        CpsGrowthAnalyticsService.RoiSummary summary = service.calculateRoi(
                new CpsGrowthAnalyticsService.RoiFacts(
                        100L, 25L, 10L, 8L, 6L,
                        125_000L, 32_000L, 18_000L, 2_000L,
                        125L, 8L, 6L, 6L, 1L));

        assertEquals(12_000L, summary.netRevenueCent());
        assertEquals(0.25D, summary.clickThroughRate());
        assertEquals(0.8D, summary.orderConversionRate());
        assertTrue(summary.traceable());
        assertTrue(summary.explanations().contains("late_order_count=1"));
        assertTrue(summary.explanations().contains("refund_reversal_cent=2000"));
    }

    @Test
    @DisplayName("summarizeRisk emits tenant-level alerts for attribution, transfer, sync, debt, and asset diff")
    void summarizeRisk_emitsTenantLevelAlerts() {
        CpsGrowthAnalyticsService.RiskSummary summary = service.summarizeRisk(
                new CpsGrowthAnalyticsService.RiskFacts(
                        100L, 8L, 200L, 180L, 7_200L, 12L, 50_000L, 1_500L),
                new CpsGrowthAnalyticsService.RiskThresholds(
                        0.05D, 0.95D, 300L, 5L, 10_000L, 0L));

        assertEquals(0.08D, summary.unattributedRate());
        assertEquals(0.9D, summary.transferSuccessRate());
        assertTrue(summary.alertCodes().contains("UNATTRIBUTED_RATE_HIGH"));
        assertTrue(summary.alertCodes().contains("TRANSFER_SUCCESS_RATE_LOW"));
        assertTrue(summary.alertCodes().contains("SYNC_DELAY_HIGH"));
        assertTrue(summary.alertCodes().contains("REFUND_DEBT_HIGH"));
        assertTrue(summary.alertCodes().contains("ASSET_DIFF_NONZERO"));
    }

    @Test
    @DisplayName("assignExperiment is replayable and stores no raw member identity")
    void assignExperiment_isReplayableAndPrivacySafe() {
        CpsGrowthAnalyticsService.ExperimentDefinition definition =
                new CpsGrowthAnalyticsService.ExperimentDefinition(
                        "exp-theme-sort", List.of("control", "commission_first"), 10_000);

        CpsGrowthAnalyticsService.ExperimentAssignment first =
                service.assignExperiment(definition, "member-1001");
        CpsGrowthAnalyticsService.ExperimentAssignment second =
                service.assignExperiment(definition, "member-1001");

        assertEquals(first, second);
        assertFalse(first.subjectHash().contains("member-1001"));
        assertTrue(first.bucket() >= 0 && first.bucket() < 10_000);
        assertFalse(first.settlementMutationAllowed());
    }

    @Test
    @DisplayName("reconcileTokenEvents identifies one-sided success, duplicate credit, timeout, and rollback mismatch")
    void reconcileTokenEvents_identifiesCrossServiceDiffs() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 15, 12, 0);

        CpsGrowthAnalyticsService.TokenReconciliationSummary summary = service.reconcileTokenEvents(
                List.of(
                        event("CPS", "EX-1", "tenant-1", "idem-1", "SUBMIT", "SUCCESS", now.minusMinutes(2)),
                        event("TOKENHUB", "EX-1", "tenant-1", "idem-1", "CREDIT", "SUCCESS", now.minusMinutes(1)),
                        event("CPS", "EX-2", "tenant-1", "idem-2", "SUBMIT", "SUCCESS", now.minusMinutes(2)),
                        event("TOKENHUB", "EX-3", "tenant-1", "idem-3", "CREDIT", "SUCCESS", now.minusMinutes(2)),
                        event("TOKENHUB", "EX-3", "tenant-1", "idem-3", "CREDIT", "SUCCESS", now.minusMinutes(1)),
                        event("CPS", "EX-4", "tenant-1", "idem-4", "SUBMIT", "PROCESSING", now.minusHours(2)),
                        event("CPS", "EX-5", "tenant-1", "idem-5", "ROLLBACK", "SUCCESS", now.minusMinutes(2)),
                        event("TOKENHUB", "EX-5", "tenant-1", "idem-5", "CREDIT", "SUCCESS", now.minusMinutes(1)),
                        event("TOKENHUB", "EX-6", "tenant-1", "idem-6", "CREDIT", "SUCCESS", now.minusMinutes(1))),
                now,
                Duration.ofMinutes(30));

        assertEquals(1L, summary.matchedSuccessCount());
        assertTrue(summary.diffCodesByOrderNo().get("EX-2").contains("TOKENHUB_MISSING_SUCCESS"));
        assertTrue(summary.diffCodesByOrderNo().get("EX-3").contains("TOKENHUB_DUPLICATE_CREDIT"));
        assertTrue(summary.diffCodesByOrderNo().get("EX-4").contains("PROCESSING_TIMEOUT"));
        assertTrue(summary.diffCodesByOrderNo().get("EX-5").contains("ROLLBACK_MISMATCH"));
        assertTrue(summary.diffCodesByOrderNo().containsKey("EX-6"));
        assertTrue(summary.diffCodesByOrderNo().get("EX-6").contains("CPS_MISSING_SUCCESS"));
    }

    @Test
    @DisplayName("reconcileTokenEvents treats the threshold as timed out and ignores resolved processing")
    void reconcileTokenEvents_usesLatestCpsSubmitStateForTimeout() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 15, 12, 0);

        CpsGrowthAnalyticsService.TokenReconciliationSummary summary = service.reconcileTokenEvents(
                List.of(
                        event("CPS", "EX-TIMEOUT", "tenant-1", "idem-timeout", "SUBMIT", "PROCESSING",
                                now.minusMinutes(30)),
                        event("CPS", "EX-RESOLVED", "tenant-1", "idem-resolved", "SUBMIT", "PROCESSING",
                                now.minusHours(2)),
                        event("CPS", "EX-RESOLVED", "tenant-1", "idem-resolved", "SUBMIT", "SUCCESS",
                                now.minusMinutes(1)),
                        event("TOKENHUB", "EX-RESOLVED", "tenant-1", "idem-resolved", "CREDIT", "SUCCESS",
                                now)),
                now,
                Duration.ofMinutes(30));

        assertTrue(summary.diffCodesByOrderNo().containsKey("EX-TIMEOUT"));
        assertTrue(summary.diffCodesByOrderNo().get("EX-TIMEOUT").contains("PROCESSING_TIMEOUT"));
        assertFalse(summary.diffCodesByOrderNo().containsKey("EX-RESOLVED"));
    }

    @Test
    @DisplayName("validateBillingBoundary allows confirmed asset consumption but rejects CPS asset writes")
    void validateBillingBoundary_rejectsCpsAssetWrites() {
        CpsGrowthAnalyticsService.BillingBoundaryDecision consumeDecision = service.validateBillingBoundary(
                new CpsGrowthAnalyticsService.BillingBoundaryCommand(
                        "billing-service", "CONSUME_CONFIRMED_ASSET_EVENT", false, false, false));
        CpsGrowthAnalyticsService.BillingBoundaryDecision writeDecision = service.validateBillingBoundary(
                new CpsGrowthAnalyticsService.BillingBoundaryCommand(
                        "billing-service", "WRITE_REBATE_ACCOUNT", true, true, true));
        CpsGrowthAnalyticsService.BillingBoundaryDecision unknownDecision = service.validateBillingBoundary(
                new CpsGrowthAnalyticsService.BillingBoundaryCommand(
                        "billing-service", "UNKNOWN_UNCONFIRMED_EVENT", false, false, false));

        assertTrue(consumeDecision.allowed());
        assertFalse(writeDecision.allowed());
        assertEquals("BILLING_MUST_NOT_WRITE_CPS_REBATE_ASSET", writeDecision.reasonCode());
        assertFalse(unknownDecision.allowed());
        assertEquals("ONLY_CONFIRMED_ASSET_EVENT_CONSUMPTION_ALLOWED", unknownDecision.reasonCode());
    }

    private static CpsGrowthAnalyticsService.TokenEvent event(String side, String orderNo, String tenantId,
                                                             String idempotencyKey, String eventType,
                                                             String status, LocalDateTime eventTime) {
        return new CpsGrowthAnalyticsService.TokenEvent(
                side, orderNo, tenantId, idempotencyKey, eventType, status, eventTime);
    }
}
