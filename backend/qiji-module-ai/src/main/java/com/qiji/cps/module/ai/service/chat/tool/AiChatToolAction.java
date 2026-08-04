package com.qiji.cps.module.ai.service.chat.tool;

import lombok.Data;

import java.util.List;

/**
 * Describes a user-facing chat action while keeping the internal tool name server-side.
 */
@Data
public class AiChatToolAction {

    private String intent;
    private String toolName;
    private String label;
    private String group;
    private AiChatToolRiskLevel riskLevel;
    private AiChatToolInteractionType interactionType;
    private String runningMessage;
    private String successMessage;
    private String promptTemplate;
    private List<AiChatToolActionField> fields;

}
