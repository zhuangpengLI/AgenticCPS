package com.qiji.cps.module.cps.client.haodanku.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HdkActivityItem {

    private String id;

    private String activityId;

    private String activityUrl;

    private String activityPic;

    private String activityName;

    private String activityLabel;

    private String startTime;

    private String endTime;

    private String platform;

    private String describe;

    private String commissionRate;

    private String promotionNum;

    private String promotionType;

    private String activityDate;

    private Integer isChannel;

}
