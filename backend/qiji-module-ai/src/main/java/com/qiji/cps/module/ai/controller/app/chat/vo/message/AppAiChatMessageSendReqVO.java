package com.qiji.cps.module.ai.controller.app.chat.vo.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Schema(description = "会员端 - AI 聊天消息发送 Request VO")
@Data
public class AppAiChatMessageSendReqVO {

    @NotNull
    private Long conversationId;

    private String content;

    private Boolean useContext;
    private Boolean useSearch;
    @Size(max = 3, message = "最多上传 3 个附件")
    private List<String> attachmentUrls;

    private String toolIntent;

    private UUID intentRequestId;
}
