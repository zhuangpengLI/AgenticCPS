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

    private String rawPayload;

}
