package com.qiji.cps.module.ai.controller.app.chat.vo.conversation;

import com.fhs.core.trans.vo.VO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "会员端 - AI 聊天会话 Response VO")
@Data
public class AppAiChatConversationRespVO implements VO {

    private Long id;
    private String title;
    private Boolean pinned;
    private Long roleId;
    private Long modelId;
    private String model;
    private String modelName;
    private String systemMessage;
    private Double temperature;
    private Integer maxTokens;
    private Integer maxContexts;
    private LocalDateTime createTime;
    private String roleAvatar;
    private String roleName;
}
