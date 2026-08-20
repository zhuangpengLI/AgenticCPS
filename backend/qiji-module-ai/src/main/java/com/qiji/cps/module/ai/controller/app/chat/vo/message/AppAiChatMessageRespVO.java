package com.qiji.cps.module.ai.controller.app.chat.vo.message;

import com.qiji.cps.module.ai.framework.ai.core.webserch.AiWebSearchResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "会员端 - AI 聊天消息 Response VO")
@Data
public class AppAiChatMessageRespVO {
    private Long id;
    private Long conversationId;
    private Long replyId;
    private String type;
    private Long roleId;
    private String model;
    private Long modelId;
    private String content;
    private String reasoningContent;
    private Boolean useContext;
    private List<Long> segmentIds;
    private List<KnowledgeSegment> segments;
    private List<AiWebSearchResponse.WebPage> webSearchPages;
    private List<String> attachmentUrls;
    private List<java.util.Map<String, Object>> blocks;
    private LocalDateTime createTime;
    private String roleName;

    @Data
    public static class KnowledgeSegment {
        private Long id;
        private String content;
        private Long documentId;
        private String documentName;
    }
}
