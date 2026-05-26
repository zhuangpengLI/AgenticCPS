package com.qiji.cps.module.cps.controller.openapi.cpx.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CpxEventCreateReqVO {

    private String trackingId;
    @NotNull(message = "任务编号不能为空")
    private Long taskId;
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;
    @NotBlank(message = "推广方式不能为空")
    private String promotionMethod;
    private String eventType;
    @NotBlank(message = "来源事件编号不能为空")
    private String sourceEventId;
    private String idempotencyKey;
    private Long memberId;
    private String clientIp;
    private String userAgent;
    private LocalDateTime eventTime;
    private String rawPayload;
}
