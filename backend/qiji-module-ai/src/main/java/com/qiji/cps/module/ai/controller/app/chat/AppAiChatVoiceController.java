package com.qiji.cps.module.ai.controller.app.chat;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.ai.controller.app.chat.vo.voice.AppAiChatVoiceTranscribeReqVO;
import com.qiji.cps.module.ai.controller.app.chat.vo.voice.AppAiChatVoiceTranscribeRespVO;
import com.qiji.cps.module.ai.service.voice.AiVoiceTranscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;
import static com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "会员端 - AI 语音")
@RestController
@RequestMapping("/ai/chat/voice")
public class AppAiChatVoiceController {
    @Resource
    private AiVoiceTranscriptionService transcriptionService;

    @PostMapping("/transcribe")
    @Operation(summary = "提交会员 AI 语音转写任务")
    public CommonResult<AppAiChatVoiceTranscribeRespVO> transcribe(
            @Valid @RequestBody AppAiChatVoiceTranscribeReqVO request) {
        return success(transcriptionService.submit(request, getLoginUserId()));
    }

    @GetMapping("/transcribe/status")
    @Operation(summary = "查询会员 AI 语音转写任务")
    public CommonResult<AppAiChatVoiceTranscribeRespVO> status(@RequestParam String requestId) {
        return success(transcriptionService.getStatus(requestId, getLoginUserId()));
    }
}
