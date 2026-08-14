package com.qiji.cps.module.cps.controller.openapi.didi.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DidiRewardCallbackReqVO {
    @JsonProperty("activity_id") private String activityId;
    @JsonProperty("callback_info") private String callbackInfo;
    @JsonProperty("is_reward_sent") private Boolean rewardSent;
    @JsonProperty("retry_times") private Integer retryTimes;
    @JsonProperty("trace_id") private String traceId;
}
