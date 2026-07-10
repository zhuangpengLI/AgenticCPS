package com.qiji.cps.module.cps.service.didi;
public interface DidiUnionMaterialService {
    DidiUnionMaterialResult generate(DidiUnionMaterialType type, long activityId, Long promotionId);
    boolean testConnection();
    DidiUnionOrderAttributionResult queryOrderAttribution(String orderId);
}
