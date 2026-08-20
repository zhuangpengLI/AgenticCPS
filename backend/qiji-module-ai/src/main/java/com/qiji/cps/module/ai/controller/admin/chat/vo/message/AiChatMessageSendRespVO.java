package com.qiji.cps.module.ai.controller.admin.chat.vo.message;

import com.qiji.cps.module.ai.framework.ai.core.webserch.AiWebSearchResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - AI 聊天消息发送 Response VO")
@Data
public class AiChatMessageSendRespVO {

    @Schema(description = "流式事件类型", example = "MESSAGE_DELTA")
    private String eventType;

    @Schema(description = "工具执行状态，仅工具事件返回")
    private ToolExecution toolExecution;

    @Schema(description = "发送消息", requiredMode = Schema.RequiredMode.REQUIRED)
    private Message send;

    @Schema(description = "接收消息", requiredMode = Schema.RequiredMode.REQUIRED)
    private Message receive;

    @Schema(description = "消息")
    @Data
    public static class Message {

        @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        private Long id;

        @Schema(description = "消息类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "role")
        private String type; // 参见 MessageType 枚举类

        @Schema(description = "聊天内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "你好，你好啊")
        private String content;

        @Schema(description = "推理内容", example = "要达到这个目标，你需要...")
        private String reasoningContent;

        @Schema(description = "知识库段落编号数组", example = "[1,2,3]")
        private List<Long> segmentIds;

        @Schema(description = "知识库段落数组")
        private List<AiChatMessageRespVO.KnowledgeSegment> segments;

        @Schema(description = "联网搜索的网页内容数组")
        private List<AiWebSearchResponse.WebPage> webSearchPages;

        @Schema(description = "结构化 UI 块")
        private List<java.util.Map<String, Object>> blocks;

        @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime createTime;

    }

    @Schema(description = "用户友好的工具执行信息")
    @Data
    public static class ToolExecution {
        private String executionId;
        private String toolName;
        private String intent;
        private String label;
        private String status;
        private String message;
    }

}
