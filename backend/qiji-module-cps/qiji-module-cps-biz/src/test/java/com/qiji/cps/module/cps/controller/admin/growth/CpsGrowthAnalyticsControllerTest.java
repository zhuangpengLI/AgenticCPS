package com.qiji.cps.module.cps.controller.admin.growth;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.qiji.cps.framework.jackson.config.QijiJacksonAutoConfiguration;
import com.qiji.cps.module.cps.service.growth.CpsGrowthAnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

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
        List<CpsGrowthAnalyticsService.TokenEvent> events = List.of(
                new CpsGrowthAnalyticsService.TokenEvent(
                        "CPS", "EX-1", "tenant-1", "idem-1", "SUBMIT", "PROCESSING", now));
        CpsGrowthAnalyticsController.TokenReconciliationReqVO request =
                new CpsGrowthAnalyticsController.TokenReconciliationReqVO(
                        List.of(new CpsGrowthAnalyticsController.TokenEventReqVO(
                                "CPS", "EX-1", "tenant-1", "idem-1", "SUBMIT", "PROCESSING", now)),
                        now,
                        Duration.ofMinutes(30));
        CpsGrowthAnalyticsService.TokenReconciliationSummary summary =
                new CpsGrowthAnalyticsService.TokenReconciliationSummary(
                        0L, Map.of("EX-1", List.of("PROCESSING_TIMEOUT")));
        when(growthAnalyticsService.reconcileTokenEvents(events, request.now(), request.processingTimeout()))
                .thenReturn(summary);

        CpsGrowthAnalyticsService.TokenReconciliationSummary result =
                controller.reconcileTokenEvents(request).getData();

        assertEquals(summary, result);
        verify(growthAnalyticsService).reconcileTokenEvents(events, request.now(), request.processingTimeout());
    }

    @Test
    void reconcileTokenEvents_deserializesIsoDurationAndReturnsTimeout() throws Exception {
        CpsGrowthAnalyticsController httpController = new CpsGrowthAnalyticsController();
        ReflectionTestUtils.setField(httpController, "growthAnalyticsService", new CpsGrowthAnalyticsService());
        JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        objectMapper.registerModule(new QijiJacksonAutoConfiguration().timestampSupportModuleBean());
        MockMvc mockMvc = standaloneSetup(httpController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        mockMvc.perform(post("/cps/growth-analytics/token-reconciliation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "events": [{
                                    "side": "CPS",
                                    "businessOrderNo": "EX-HTTP-TIMEOUT",
                                    "tenantId": "tenant-1",
                                    "idempotencyKey": "idem-http-timeout",
                                    "eventType": "SUBMIT",
                                    "status": "PROCESSING",
                                    "eventTime": "2026-07-15T11:30:00"
                                  }],
                                  "now": "2026-07-15T12:00:00",
                                  "processingTimeout": "PT30M"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['data']['diffCodesByOrderNo']['EX-HTTP-TIMEOUT'][0]")
                        .value("PROCESSING_TIMEOUT"));
    }

    @Test
    void reconcileTokenEvents_keepsEpochMillisCompatibility() throws Exception {
        CpsGrowthAnalyticsController httpController = new CpsGrowthAnalyticsController();
        ReflectionTestUtils.setField(httpController, "growthAnalyticsService", new CpsGrowthAnalyticsService());
        JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        objectMapper.registerModule(new QijiJacksonAutoConfiguration().timestampSupportModuleBean());
        MockMvc mockMvc = standaloneSetup(httpController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        LocalDateTime now = LocalDateTime.of(2026, 7, 15, 12, 0);
        long eventTimeMillis = now.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        mockMvc.perform(post("/cps/growth-analytics/token-reconciliation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "events": [{
                                    "side": "CPS",
                                    "businessOrderNo": "EX-HTTP-EPOCH",
                                    "tenantId": "tenant-1",
                                    "idempotencyKey": "idem-http-epoch",
                                    "eventType": "SUBMIT",
                                    "status": "PROCESSING",
                                    "eventTime": %d
                                  }],
                                  "now": %d,
                                  "processingTimeout": "PT30M"
                                }
                                """.formatted(eventTimeMillis, nowMillis)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['data']['diffCodesByOrderNo']['EX-HTTP-EPOCH'][0]")
                        .value("PROCESSING_TIMEOUT"));
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
