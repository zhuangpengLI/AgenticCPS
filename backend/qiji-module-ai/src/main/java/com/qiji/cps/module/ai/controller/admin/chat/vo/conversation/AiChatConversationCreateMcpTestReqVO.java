package com.qiji.cps.module.ai.controller.admin.chat.vo.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CPS MCP 自测会话创建 Request VO")
@Data
public class AiChatConversationCreateMcpTestReqVO {

    @Schema(description = "CPS 聊天角色编号；为空时默认 CPS 联盟助手")
    private Long roleId;

    @Schema(description = "用于测试的会员编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "测试会员不能为空")
    private Long memberId;
}
