package com.qiji.cps.module.cps.controller.app.cpx.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppCpxTrackingLinkCreateReqVO {

    @NotNull(message = "任务编号不能为空")
    private Long taskId;
    private Long offerId;
    private Long materialId;
    private String adzoneId;
    private String channelCode;
}
