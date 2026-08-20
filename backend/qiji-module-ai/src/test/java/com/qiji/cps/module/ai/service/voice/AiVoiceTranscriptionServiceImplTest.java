package com.qiji.cps.module.ai.service.voice;

import com.qiji.cps.module.ai.controller.app.chat.vo.voice.AppAiChatVoiceTranscribeReqVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AiVoiceTranscriptionServiceImplTest {

    private final AiVoiceTranscriptionServiceImpl service = new AiVoiceTranscriptionServiceImpl();

    @Test
    void rejectsUnsupportedFormatAndExcessiveDuration() {
        var unsupported = service.submit(request("exe", 1000L), 1L);
        assertEquals("FAILED", unsupported.getStatus());
        assertEquals("暂不支持该音频格式", unsupported.getErrorMessage());

        var tooLong = service.submit(request("mp3", 60001L), 1L);
        assertEquals("FAILED", tooLong.getStatus());
        assertEquals("录音时长需大于 0 且不超过 60 秒", tooLong.getErrorMessage());
    }

    @Test
    void statusIsBoundToSubmittingMember() {
        var submitted = service.submit(request("webm", 1000L), 7L);
        assertNotNull(submitted.getRequestId());
        assertEquals("FAILED", service.getStatus(submitted.getRequestId(), 7L).getStatus());
        assertEquals("转写任务不存在", service.getStatus(submitted.getRequestId(), 8L).getErrorMessage());
    }

    private static AppAiChatVoiceTranscribeReqVO request(String format, Long durationMs) {
        return new AppAiChatVoiceTranscribeReqVO()
                .setAudioUrl("https://files.example.test/audio." + format)
                .setFormat(format)
                .setDurationMs(durationMs)
                .setLanguage("zh-CN");
    }
}
