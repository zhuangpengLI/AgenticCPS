package com.qiji.cps.module.ai.controller.app.chat.vo.message;

import com.qiji.cps.module.ai.framework.ai.core.webserch.AiWebSearchResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "会员端 - AI 聊天消息发送 Response VO")
@Data
public class AppAiChatMessageSendRespVO {
    private Message send;
    private Message receive;

    @Data
    public static class Message {
        private Long id;
        private String type;
        private String content;
        private String reasoningContent;
        private List<Long> segmentIds;
        private List<AppAiChatMessageRespVO.KnowledgeSegment> segments;
        private List<AiWebSearchResponse.WebPage> webSearchPages;
        private LocalDateTime createTime;
    }
}
