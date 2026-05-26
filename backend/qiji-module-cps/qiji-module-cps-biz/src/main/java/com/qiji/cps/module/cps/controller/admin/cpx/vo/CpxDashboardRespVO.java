package com.qiji.cps.module.cps.controller.admin.cpx.vo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CpxDashboardRespVO {

    private Integer taskCount;
    private Integer onlineTaskCount;
    private Map<String, Long> taskCountByMethod = new LinkedHashMap<>();
    private Integer impressionCount;
    private Integer clickCount;
    private Integer leadCount;
    private Integer actionCount;
    private Integer conversionCount;
    private Integer settlementCount;
    private Integer settlementAmount;
    private Integer rewardAmount;
}
