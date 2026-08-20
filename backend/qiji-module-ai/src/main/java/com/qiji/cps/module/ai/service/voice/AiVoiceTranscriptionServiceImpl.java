package com.qiji.cps.module.ai.service.voice;

import com.qiji.cps.module.ai.controller.app.chat.vo.voice.AppAiChatVoiceTranscribeReqVO;
import com.qiji.cps.module.ai.controller.app.chat.vo.voice.AppAiChatVoiceTranscribeRespVO;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Provider-neutral transcription boundary. A configured ASR provider can
 * replace this implementation without changing the mobile API contract.
 */
@Service
public class AiVoiceTranscriptionServiceImpl implements AiVoiceTranscriptionService {
    private static final Set<String> SUPPORTED_FORMATS = Set.of("mp3", "wav", "m4a", "webm", "aac", "ogg");
    private final Map<String, Entry> requests = new ConcurrentHashMap<>();

    @Override
    public AppAiChatVoiceTranscribeRespVO submit(AppAiChatVoiceTranscribeReqVO request, Long userId) {
        String format = request.getFormat() == null ? "" : request.getFormat().trim().toLowerCase();
        if (!SUPPORTED_FORMATS.contains(format)) {
            return response(null, "FAILED", null, "暂不支持该音频格式");
        }
        if (request.getDurationMs() != null && (request.getDurationMs() <= 0 || request.getDurationMs() > 60000)) {
            return response(null, "FAILED", null, "录音时长需大于 0 且不超过 60 秒");
        }
        String requestId = UUID.randomUUID().toString();
        // Keep the state explicit until an ASR provider is configured. The UI
        // can show a recoverable error instead of silently pretending success.
        requests.put(requestId, new Entry(userId, "FAILED", null, "当前未配置语音识别服务"));
        return response(requestId, "FAILED", null, "当前未配置语音识别服务");
    }

    @Override
    public AppAiChatVoiceTranscribeRespVO getStatus(String requestId, Long userId) {
        Entry entry = requests.get(requestId);
        if (entry == null || !entry.userId().equals(userId)) return response(requestId, "FAILED", null, "转写任务不存在");
        return response(requestId, entry.status(), entry.text(), entry.errorMessage());
    }

    private AppAiChatVoiceTranscribeRespVO response(String requestId, String status, String text, String error) {
        return new AppAiChatVoiceTranscribeRespVO().setRequestId(requestId).setStatus(status).setText(text).setErrorMessage(error);
    }

    private record Entry(Long userId, String status, String text, String errorMessage) { }
}
