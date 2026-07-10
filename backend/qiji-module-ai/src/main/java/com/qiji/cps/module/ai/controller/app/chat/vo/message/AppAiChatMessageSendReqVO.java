package com.qiji.cps.module.ai.controller.app.chat.vo.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "会员端 - AI 聊天消息发送 Request VO")
@Data
public class AppAiChatMessageSendReqVO {

    @NotNull
    private Long conversationId;

    @NotEmpty
    private String content;

    private Boolean useContext;
    private Boolean useSearch;
    private List<String> attachmentUrls;
}
