package com.qiji.cps.module.cps.controller.admin.growth;

import com.qiji.cps.module.cps.service.growth.CpsGrowthAnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsGrowthAnalyticsControllerTest {

    @InjectMocks
    private CpsGrowthAnalyticsController controller;

    @Mock
    private CpsGrowthAnalyticsService growthAnalyticsService;

    @Test
    void calculateRoi_delegatesToReadOnlyGrowthService() {
        CpsGrowthAnalyticsService.RoiFacts facts = new CpsGrowthAnalyticsService.RoiFacts(
                10L, 5L, 4L, 3L, 2L, 1000L, 800L, 300L, 100L,
                15L, 3L, 2L, 2L, 1L);
        CpsGrowthAnalyticsService.RoiSummary summary = new CpsGrowthAnalyticsService.RoiSummary(
                400L, 0.5D, 0.75D, true, List.of("late_order_count=1"));
        when(growthAnalyticsService.calculateRoi(facts)).thenReturn(summary);

        CpsGrowthAnalyticsService.RoiSummary result = controller.calculateRoi(facts).getData();

        assertEquals(summary, result);
        verify(growthAnalyticsService).calculateRoi(facts);
    }

    @Test
    void summarizeRisk_delegatesWithTenantThresholds() {
        CpsGrowthAnalyticsController.RiskSummaryReqVO request = new CpsGrowthAnalyticsController.RiskSummaryReqVO(
                new CpsGrowthAnalyticsService.RiskFacts(100L, 6L, 10L, 9L, 600L, 3L, 20_000L, 1L),
                new CpsGrowthAnalyticsService.RiskThresholds(0.05D, 0.95D, 300L, 2L, 10_000L, 0L));
        CpsGrowthAnalyticsService.RiskSummary summary =
                new CpsGrowthAnalyticsService.RiskSummary(0.06D, 0.9D, List.of("UNATTRIBUTED_RATE_HIGH"));
        when(growthAnalyticsService.summarizeRisk(request.facts(), request.thresholds())).thenReturn(summary);

        CpsGrowthAnalyticsService.RiskSummary result = controller.summarizeRisk(request).getData();

        assertEquals(summary, result);
        verify(growthAnalyticsService).summarizeRisk(request.facts(), request.thresholds());
    }

    @Test
    void assignExperiment_delegatesToReplayableAssignment() {
        CpsGrowthAnalyticsController.ExperimentAssignReqVO request =
                new CpsGrowthAnalyticsController.ExperimentAssignReqVO(
                        new CpsGrowthAnalyticsService.ExperimentDefinition(
                                "exp-1", List.of("control", "new-sort"), 10_000),
                        "member-1001");
        CpsGrowthAnalyticsService.ExperimentAssignment assignment =
                new CpsGrowthAnalyticsService.ExperimentAssignment(
                        "exp-1", "control", 17, "hashed", false);
        when(growthAnalyticsService.assignExperiment(request.definition(), request.subjectKey()))
                .thenReturn(assignment);

        CpsGrowthAnalyticsService.ExperimentAssignment result = controller.assignExperiment(request).getData();

        assertEquals(assignment, result);
        verify(growthAnalyticsService).assignExperiment(request.definition(), request.subjectKey());
    }

    @Test
    void reconcileTokenEvents_delegatesToCrossServiceAudit() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 15, 12, 0);
        CpsGrowthAnalyticsController.TokenReconciliationReqVO request =
                new CpsGrowthAnalyticsController.TokenReconciliationReqVO(
                        List.of(new CpsGrowthAnalyticsService.TokenEvent(
                                "CPS", "EX-1", "tenant-1", "idem-1", "SUBMIT", "PROCESSING", now)),
                        now,
                        Duration.ofMinutes(30));
        CpsGrowthAnalyticsService.TokenReconciliationSummary summary =
                new CpsGrowthAnalyticsService.TokenReconciliationSummary(
                        0L, Map.of("EX-1", List.of("PROCESSING_TIMEOUT")));
        when(growthAnalyticsService.reconcileTokenEvents(request.events(), request.now(), request.processingTimeout()))
                .thenReturn(summary);

        CpsGrowthAnalyticsService.TokenReconciliationSummary result =
                controller.reconcileTokenEvents(request).getData();

        assertEquals(summary, result);
        verify(growthAnalyticsService).reconcileTokenEvents(request.events(), request.now(), request.processingTimeout());
    }

    @Test
    void validateBillingBoundary_delegatesToBoundaryGuard() {
        CpsGrowthAnalyticsService.BillingBoundaryCommand command =
                new CpsGrowthAnalyticsService.BillingBoundaryCommand(
                        "billing-service", "WRITE_REBATE_ACCOUNT", true, false, false);
        CpsGrowthAnalyticsService.BillingBoundaryDecision decision =
                new CpsGrowthAnalyticsService.BillingBoundaryDecision(
                        false, "BILLING_MUST_NOT_WRITE_CPS_REBATE_ASSET");
        when(growthAnalyticsService.validateBillingBoundary(command)).thenReturn(decision);

        CpsGrowthAnalyticsService.BillingBoundaryDecision result =
                controller.validateBillingBoundary(command).getData();

        assertEquals(decision, result);
        verify(growthAnalyticsService).validateBillingBoundary(command);
    }
}
