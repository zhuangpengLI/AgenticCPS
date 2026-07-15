package com.qiji.cps.module.cps.controller.admin.growth;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.service.growth.CpsGrowthAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - CPS growth analytics")
@RestController
@RequestMapping("/cps/growth-analytics")
@Validated
public class CpsGrowthAnalyticsController {

    @Resource
    private CpsGrowthAnalyticsService growthAnalyticsService;

    @PostMapping("/roi")
    @Operation(summary = "Calculate traceable ROI summary")
    @PreAuthorize("@ss.hasPermission('cps:growth-analytics:query')")
    public CommonResult<CpsGrowthAnalyticsService.RoiSummary> calculateRoi(
            @Valid @RequestBody CpsGrowthAnalyticsService.RoiFacts facts) {
        return success(growthAnalyticsService.calculateRoi(facts));
    }

    @PostMapping("/risk-summary")
    @Operation(summary = "Summarize tenant-level growth and risk alerts")
    @PreAuthorize("@ss.hasPermission('cps:growth-analytics:query')")
    public CommonResult<CpsGrowthAnalyticsService.RiskSummary> summarizeRisk(
            @Valid @RequestBody RiskSummaryReqVO reqVO) {
        return success(growthAnalyticsService.summarizeRisk(reqVO.facts(), reqVO.thresholds()));
    }

    @PostMapping("/experiment/assign")
    @Operation(summary = "Assign a replayable display-only experiment variant")
    @PreAuthorize("@ss.hasPermission('cps:growth-analytics:query')")
    public CommonResult<CpsGrowthAnalyticsService.ExperimentAssignment> assignExperiment(
            @Valid @RequestBody ExperimentAssignReqVO reqVO) {
        return success(growthAnalyticsService.assignExperiment(reqVO.definition(), reqVO.subjectKey()));
    }

    @PostMapping("/token-reconciliation")
    @Operation(summary = "Reconcile CPS and TokenHub exchange events")
    @PreAuthorize("@ss.hasPermission('cps:growth-analytics:query')")
    public CommonResult<CpsGrowthAnalyticsService.TokenReconciliationSummary> reconcileTokenEvents(
            @Valid @RequestBody TokenReconciliationReqVO reqVO) {
        return success(growthAnalyticsService.reconcileTokenEvents(
                reqVO.events().stream().map(TokenEventReqVO::toTokenEvent).toList(),
                reqVO.now(), reqVO.processingTimeout()));
    }

    @PostMapping("/billing-boundary/validate")
    @Operation(summary = "Validate billing-service CPS boundary")
    @PreAuthorize("@ss.hasPermission('cps:growth-analytics:query')")
    public CommonResult<CpsGrowthAnalyticsService.BillingBoundaryDecision> validateBillingBoundary(
            @Valid @RequestBody CpsGrowthAnalyticsService.BillingBoundaryCommand command) {
        return success(growthAnalyticsService.validateBillingBoundary(command));
    }

    public record RiskSummaryReqVO(CpsGrowthAnalyticsService.RiskFacts facts,
                                   CpsGrowthAnalyticsService.RiskThresholds thresholds) {
    }

    public record ExperimentAssignReqVO(CpsGrowthAnalyticsService.ExperimentDefinition definition,
                                        String subjectKey) {
    }

    public record TokenReconciliationReqVO(List<TokenEventReqVO> events,
                                           @JsonDeserialize(using = EpochOrIsoLocalDateTimeDeserializer.class)
                                           LocalDateTime now,
                                           Duration processingTimeout) {
    }

    public record TokenEventReqVO(String side,
                                  String businessOrderNo,
                                  String tenantId,
                                  String idempotencyKey,
                                  String eventType,
                                  String status,
                                  @JsonDeserialize(using = EpochOrIsoLocalDateTimeDeserializer.class)
                                  LocalDateTime eventTime) {

        CpsGrowthAnalyticsService.TokenEvent toTokenEvent() {
            return new CpsGrowthAnalyticsService.TokenEvent(
                    side, businessOrderNo, tenantId, idempotencyKey, eventType, status, eventTime);
        }
    }

    public static class EpochOrIsoLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return fromEpochMillis(parser.getLongValue());
            }
            String value = parser.getValueAsString();
            if (value != null && value.matches("-?\\d+")) {
                return fromEpochMillis(Long.parseLong(value));
            }
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException | NullPointerException exception) {
                return (LocalDateTime) context.handleWeirdStringValue(
                        LocalDateTime.class, value, "Expected epoch milliseconds or ISO-8601 local date-time");
            }
        }

        private LocalDateTime fromEpochMillis(long epochMillis) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        }
    }
}
