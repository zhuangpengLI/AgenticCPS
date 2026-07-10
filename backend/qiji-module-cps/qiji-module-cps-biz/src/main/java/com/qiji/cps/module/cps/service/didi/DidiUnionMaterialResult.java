package com.qiji.cps.module.cps.service.didi;
public record DidiUnionMaterialResult(DidiUnionMaterialType materialType, long activityId, long promotionId,
                                      String sourceId, String link, String dsi, String appId, String appSource,
                                      String qrCodeUrl, String posterUrl, String couponCode, String traceId) { }
