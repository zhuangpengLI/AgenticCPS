package com.qiji.cps.module.ai.controller.app.chat.vo.voice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会员端 - AI 语音转写响应")
public class AppAiChatVoiceTranscribeRespVO {
    private String requestId;
    private String status;
    private String text;
    private String errorMessage;
}
