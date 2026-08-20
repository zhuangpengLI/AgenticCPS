package com.qiji.cps.module.ai.controller.app.chat.vo.voice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "会员端 - AI 语音转写请求")
public class AppAiChatVoiceTranscribeReqVO {
    @NotBlank
    @Size(max = 2048)
    private String audioUrl;
    @NotBlank
    private String format;
    @PositiveOrZero
    private Long durationMs;
    private String language;
}
