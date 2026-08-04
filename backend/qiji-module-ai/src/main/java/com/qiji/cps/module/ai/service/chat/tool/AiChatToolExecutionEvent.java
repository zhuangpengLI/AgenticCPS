package com.qiji.cps.module.ai.service.chat.tool;

import lombok.Data;

@Data
public class AiChatToolExecutionEvent {

    private String eventType;
    private String executionId;
    private String intent;
    private String label;
    private String status;
    private String message;

}
