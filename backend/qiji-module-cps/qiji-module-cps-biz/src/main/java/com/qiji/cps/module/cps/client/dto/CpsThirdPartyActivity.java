package com.qiji.cps.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 第三方活动统一对象。
 */
@Data
@Builder
public class CpsThirdPartyActivity {

    private String sourceType;

    private String externalActivityId;

    /** Vendor parameter used by the activity conversion endpoint. */
    private String promotionActivityId;

    private String activityName;

    private String activityType;

    private String platformCode;

    private String mainPic;

    private String icon;

    private String shortDesc;

    private String rebateDesc;

    private String billingType;

    private Integer promotionCount;

    private String tagText;

    private String jumpType;

    private String jumpUrl;

    private String searchKeyword;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Map<String, Object> extraFields;

    /** JSON snapshot of the minimal vendor fields needed after synchronization. */
    private String vendorMetadata;

    private String rawPayload;

    /** Whether the vendor can list this activity through an official activity API. */
    private Boolean supportsList;

    /** Whether the vendor can generate a real external promotion link for this activity. */
    private Boolean supportsPromotionLink;

    /** Whether the vendor exposes order synchronization for this activity family. */
    private Boolean supportsOrders;

    /** Whether the vendor response includes mini-program material for this activity. */
    private Boolean supportsMiniProgram;

    /** Whether this activity belongs to local-life promotion scenarios. */
    private Boolean supportsLocalLife;

}
