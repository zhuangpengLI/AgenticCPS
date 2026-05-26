package com.qiji.cps.module.cps.service.cpx;

public final class CpxTaskConstants {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_ONLINE = 1;
    public static final int STATUS_OFFLINE = 2;

    public static final String EVENT_IMPRESSION = "IMPRESSION";
    public static final String EVENT_CLICK = "CLICK";
    public static final String EVENT_LEAD = "LEAD";
    public static final String EVENT_ACTION = "ACTION";
    public static final String EVENT_ORDER = "ORDER";
    public static final String EVENT_IMPORT = "IMPORT";
    public static final String EVENT_ADJUST = "ADJUST";

    public static final String CONVERSION_PENDING = "PENDING";
    public static final String SETTLEMENT_PENDING = "PENDING";

    private CpxTaskConstants() {
    }
}
