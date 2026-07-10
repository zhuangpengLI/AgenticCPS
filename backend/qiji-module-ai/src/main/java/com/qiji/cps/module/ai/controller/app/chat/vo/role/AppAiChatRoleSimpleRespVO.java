package com.qiji.cps.module.ai.controller.app.chat.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "会员端 - AI 聊天角色简要 Response VO")
@Data
public class AppAiChatRoleSimpleRespVO {
    private Long id;
    private String name;
    private String avatar;
    private String category;
    private String description;
    private Long modelId;
}
