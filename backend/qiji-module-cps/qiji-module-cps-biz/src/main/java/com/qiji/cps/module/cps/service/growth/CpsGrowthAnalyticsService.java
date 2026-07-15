package com.qiji.cps.module.cps.service.growth;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CpsGrowthAnalyticsService {

    public RoiSummary calculateRoi(RoiFacts facts) {
        long netRevenueCent = facts.commissionCent() - facts.rebateCostCent() - facts.refundReversalCent();
        List<String> explanations = new ArrayList<>();
        explanations.add("late_order_count=" + facts.lateOrderCount());
        explanations.add("refund_reversal_cent=" + facts.refundReversalCent());
        return new RoiSummary(
                netRevenueCent,
                divide(facts.clickCount(), facts.exposureCount()),
                divide(facts.orderCount(), facts.transferCount()),
                isRoiTraceable(facts),
                List.copyOf(explanations));
    }

    public RiskSummary summarizeRisk(RiskFacts facts, RiskThresholds thresholds) {
        double unattributedRate = divide(facts.unattributedOrderCount(), facts.totalOrderCount());
        double transferSuccessRate = divide(facts.transferSuccessCount(), facts.transferAttemptCount());
        List<String> alertCodes = new ArrayList<>();
        if (unattributedRate > thresholds.maxUnattributedRate()) {
            alertCodes.add("UNATTRIBUTED_RATE_HIGH");
        }
        if (transferSuccessRate < thresholds.minTransferSuccessRate()) {
            alertCodes.add("TRANSFER_SUCCESS_RATE_LOW");
        }
        if (facts.maxSyncDelaySeconds() > thresholds.maxSyncDelaySeconds()) {
            alertCodes.add("SYNC_DELAY_HIGH");
        }
        if (facts.syncFailureCount() > thresholds.maxSyncFailureCount()) {
            alertCodes.add("SYNC_FAILURE_HIGH");
        }
        if (facts.refundDebtCent() > thresholds.maxRefundDebtCent()) {
            alertCodes.add("REFUND_DEBT_HIGH");
        }
        if (Math.abs(facts.assetDiffCent()) > thresholds.maxAssetDiffCent()) {
            alertCodes.add("ASSET_DIFF_NONZERO");
        }
        return new RiskSummary(unattributedRate, transferSuccessRate, List.copyOf(alertCodes));
    }

    public ExperimentAssignment assignExperiment(ExperimentDefinition definition, String subjectKey) {
        if (definition.variants() == null || definition.variants().isEmpty()) {
            throw new IllegalArgumentException("experiment variants must not be empty");
        }
        int bucketSize = definition.bucketSize() <= 0 ? 10_000 : definition.bucketSize();
        String subjectHash = sha256(definition.experimentId() + ":" + subjectKey);
        int bucket = Math.floorMod(subjectHash.hashCode(), bucketSize);
        String variantCode = definition.variants().get(bucket % definition.variants().size());
        return new ExperimentAssignment(definition.experimentId(), variantCode, bucket, subjectHash, false);
    }

    public TokenReconciliationSummary reconcileTokenEvents(List<TokenEvent> events,
                                                           LocalDateTime now,
                                                           Duration processingTimeout) {
        Map<String, List<TokenEvent>> grouped = events.stream()
                .sorted(Comparator.comparing(TokenEvent::eventTime))
                .collect(Collectors.groupingBy(this::eventKey, LinkedHashMap::new, Collectors.toList()));

        long matchedSuccessCount = 0;
        Map<String, List<String>> diffCodesByOrderNo = new LinkedHashMap<>();
        for (List<TokenEvent> group : grouped.values()) {
            String orderNo = group.get(0).businessOrderNo();
            long cpsSuccessCount = count(group, "CPS", "SUBMIT", "SUCCESS");
            long tokenCreditSuccessCount = count(group, "TOKENHUB", "CREDIT", "SUCCESS");
            long cpsRollbackSuccessCount = count(group, "CPS", "ROLLBACK", "SUCCESS");
            List<String> diffCodes = new ArrayList<>();

            if (cpsSuccessCount > 0 && tokenCreditSuccessCount == 0) {
                diffCodes.add("TOKENHUB_MISSING_SUCCESS");
            }
            if (tokenCreditSuccessCount > 1) {
                diffCodes.add("TOKENHUB_DUPLICATE_CREDIT");
            }
            if (hasTimedOutProcessing(group, now, processingTimeout)) {
                diffCodes.add("PROCESSING_TIMEOUT");
            }
            if (cpsRollbackSuccessCount > 0 && tokenCreditSuccessCount > 0) {
                diffCodes.add("ROLLBACK_MISMATCH");
            }
            if (cpsSuccessCount == 1 && tokenCreditSuccessCount == 1 && cpsRollbackSuccessCount == 0
                    && diffCodes.isEmpty()) {
                matchedSuccessCount++;
            }
            if (!diffCodes.isEmpty()) {
                diffCodesByOrderNo.put(orderNo, List.copyOf(diffCodes));
            }
        }
        return new TokenReconciliationSummary(matchedSuccessCount, diffCodesByOrderNo);
    }

    public BillingBoundaryDecision validateBillingBoundary(BillingBoundaryCommand command) {
        boolean billingService = "billing-service".equals(command.serviceName());
        boolean forbiddenWrite = command.writesCpsRebateAccount()
                || command.readsCpsRebateRules()
                || command.mutatesAttribution()
                || "WRITE_REBATE_ACCOUNT".equals(command.action())
                || "CALCULATE_REBATE".equals(command.action())
                || "FREEZE_REBATE".equals(command.action())
                || "BIND_ATTRIBUTION".equals(command.action());
        if (billingService && forbiddenWrite) {
            return new BillingBoundaryDecision(false, "BILLING_MUST_NOT_WRITE_CPS_REBATE_ASSET");
        }
        return new BillingBoundaryDecision(true, "ALLOWED_CONFIRMED_ASSET_EVENT_CONSUMPTION");
    }

    private boolean isRoiTraceable(RoiFacts facts) {
        return facts.immutableMarketingEventCount() >= facts.exposureCount() + facts.clickCount()
                && facts.trustedOrderCount() >= facts.orderCount()
                && facts.settledOrderCount() >= facts.validOrderCount()
                && facts.assetLedgerCount() >= facts.settledOrderCount();
    }

    private double divide(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return (double) numerator / (double) denominator;
    }

    private long count(List<TokenEvent> events, String side, String eventType, String status) {
        return events.stream()
                .filter(event -> side.equals(event.side()))
                .filter(event -> eventType.equals(event.eventType()))
                .filter(event -> status.equals(event.status()))
                .count();
    }

    private boolean hasTimedOutProcessing(List<TokenEvent> events, LocalDateTime now, Duration timeout) {
        return events.stream()
                .anyMatch(event -> "CPS".equals(event.side())
                        && "PROCESSING".equals(event.status())
                        && event.eventTime().plus(timeout).isBefore(now));
    }

    private String eventKey(TokenEvent event) {
        return event.tenantId() + "|" + event.businessOrderNo() + "|" + event.idempotencyKey();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record RoiFacts(long exposureCount,
                           long clickCount,
                           long transferCount,
                           long orderCount,
                           long validOrderCount,
                           long platformSettlementCent,
                           long commissionCent,
                           long rebateCostCent,
                           long refundReversalCent,
                           long immutableMarketingEventCount,
                           long trustedOrderCount,
                           long settledOrderCount,
                           long assetLedgerCount,
                           long lateOrderCount) {
    }

    public record RoiSummary(long netRevenueCent,
                             double clickThroughRate,
                             double orderConversionRate,
                             boolean traceable,
                             List<String> explanations) {
    }

    public record RiskFacts(long totalOrderCount,
                            long unattributedOrderCount,
                            long transferAttemptCount,
                            long transferSuccessCount,
                            long maxSyncDelaySeconds,
                            long syncFailureCount,
                            long refundDebtCent,
                            long assetDiffCent) {
    }

    public record RiskThresholds(double maxUnattributedRate,
                                 double minTransferSuccessRate,
                                 long maxSyncDelaySeconds,
                                 long maxSyncFailureCount,
                                 long maxRefundDebtCent,
                                 long maxAssetDiffCent) {
    }

    public record RiskSummary(double unattributedRate,
                              double transferSuccessRate,
                              List<String> alertCodes) {
    }

    public record ExperimentDefinition(String experimentId,
                                       List<String> variants,
                                       int bucketSize) {
    }

    public record ExperimentAssignment(String experimentId,
                                       String variantCode,
                                       int bucket,
                                       String subjectHash,
                                       boolean settlementMutationAllowed) {
    }

    public record TokenEvent(String side,
                             String businessOrderNo,
                             String tenantId,
                             String idempotencyKey,
                             String eventType,
                             String status,
                             LocalDateTime eventTime) {
    }

    public record TokenReconciliationSummary(long matchedSuccessCount,
                                             Map<String, List<String>> diffCodesByOrderNo) {
    }

    public record BillingBoundaryCommand(String serviceName,
                                         String action,
                                         boolean writesCpsRebateAccount,
                                         boolean readsCpsRebateRules,
                                         boolean mutatesAttribution) {
    }

    public record BillingBoundaryDecision(boolean allowed,
                                          String reasonCode) {
    }
}
