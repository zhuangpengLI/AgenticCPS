package com.qiji.cps.module.ai.controller.app.chat.vo.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "会员端 - AI 聊天会话更新 Request VO")
@Data
public class AppAiChatConversationUpdateReqVO {

    @Schema(description = "会话编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull
    private Long id;

    private String title;
    private Boolean pinned;
    private Long modelId;
    private Long knowledgeId;
    private String systemMessage;
    private Double temperature;
    private Integer maxTokens;
    private Integer maxContexts;
}
