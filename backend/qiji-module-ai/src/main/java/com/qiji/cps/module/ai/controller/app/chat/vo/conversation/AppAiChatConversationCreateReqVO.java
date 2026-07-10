package com.qiji.cps.module.ai.controller.app.chat.vo.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "会员端 - AI 聊天会话创建 Request VO")
@Data
public class AppAiChatConversationCreateReqVO {

    @Schema(description = "聊天角色编号", example = "666")
    private Long roleId;

    @Schema(description = "知识库编号", example = "1204")
    private Long knowledgeId;

}
