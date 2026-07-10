package com.qiji.cps.module.cps.service.didi;
import java.util.List;
public record DidiUnionOrderAttributionResult(String orderId, String traceId,
        List<SuccessItem> successList, List<FailItem> failList) {
    public record SuccessItem(String estimateTime, String estimateChannel, int receiveStatus,
                              String receiveTime, String sceneName) { }
    public record FailItem(String failReason, String sceneName) { }
}
