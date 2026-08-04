package com.qiji.cps.module.ai.controller.admin.chat.vo.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - AI 对话工具快捷操作 Response VO")
@Data
public class AiChatToolActionRespVO {

    private String intent;
    private String label;
    private String group;
    private String riskLevel;
    private String interactionType;
    private String runningMessage;
    private String successMessage;
    private String promptTemplate;
    private List<Field> fields;

    @Data
    public static class Field {
        private String name;
        private String label;
        private String type;
        private Boolean required;
        private String placeholder;
        private String defaultValue;
        private List<Option> options;
    }

    @Data
    public static class Option {
        private String label;
        private String value;
    }
}
