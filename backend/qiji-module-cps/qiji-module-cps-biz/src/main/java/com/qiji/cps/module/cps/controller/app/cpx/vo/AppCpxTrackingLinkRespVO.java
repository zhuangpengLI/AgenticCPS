package com.qiji.cps.module.cps.controller.app.cpx.vo;

import lombok.Data;

@Data
public class AppCpxTrackingLinkRespVO {

    private String trackingId;
    private String trackingUrl;
    private String targetUrl;
    private String promotionMethod;
    private String platformCode;
}
