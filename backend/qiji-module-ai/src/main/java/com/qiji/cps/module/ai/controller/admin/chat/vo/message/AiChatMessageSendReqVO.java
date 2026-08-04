package com.qiji.cps.module.ai.controller.admin.chat.vo.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Schema(description = "管理后台 - AI 聊天消息发送 Request VO")
@Data
public class AiChatMessageSendReqVO {

    @Schema(description = "聊天对话编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "聊天对话编号不能为空")
    private Long conversationId;

    @Schema(description = "聊天内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "帮我写个 Java 算法")
    @NotEmpty(message = "聊天内容不能为空")
    private String content;

    @Schema(description = "是否携带上下文", example = "true")
    private Boolean useContext;

    @Schema(description = "是否联网搜索", example = "true")
    private Boolean useSearch;

    @Schema(description = "附件 URL 数组", example = "https://www.iocoder.cn/1.png")
    private List<String> attachmentUrls;

    @Schema(description = "服务端白名单工具意图，仅作为路由建议", example = "SEARCH_GOODS")
    private String toolIntent;

    @Schema(description = "快捷操作请求标识，可用于写操作幂等", example = "5bffb2af-9d7a-4c23-a223-3df01131728c")
    private UUID intentRequestId;

}
