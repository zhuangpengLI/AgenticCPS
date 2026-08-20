package com.qiji.cps.module.ai.service.voice;

import com.qiji.cps.module.ai.controller.app.chat.vo.voice.AppAiChatVoiceTranscribeReqVO;
import com.qiji.cps.module.ai.controller.app.chat.vo.voice.AppAiChatVoiceTranscribeRespVO;

public interface AiVoiceTranscriptionService {
    AppAiChatVoiceTranscribeRespVO submit(AppAiChatVoiceTranscribeReqVO request, Long userId);
    AppAiChatVoiceTranscribeRespVO getStatus(String requestId, Long userId);
}
