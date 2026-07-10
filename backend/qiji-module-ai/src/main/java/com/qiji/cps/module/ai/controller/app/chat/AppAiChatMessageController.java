package com.qiji.cps.module.ai.controller.app.chat;

import cn.hutool.core.collection.CollUtil;
import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.ai.controller.admin.chat.vo.message.AiChatMessageRespVO;
import com.qiji.cps.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import com.qiji.cps.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import com.qiji.cps.module.ai.controller.app.chat.vo.message.AppAiChatMessageRespVO;
import com.qiji.cps.module.ai.controller.app.chat.vo.message.AppAiChatMessageSendReqVO;
import com.qiji.cps.module.ai.controller.app.chat.vo.message.AppAiChatMessageSendRespVO;
import com.qiji.cps.module.ai.controller.app.chat.vo.role.AppAiChatRoleSimpleRespVO;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatMessageDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.qiji.cps.module.ai.enums.chat.AiChatOwnerTypeEnum;
import com.qiji.cps.module.ai.service.chat.AiChatConversationService;
import com.qiji.cps.module.ai.service.chat.AiChatMessageService;
import com.qiji.cps.module.ai.service.model.AiChatRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;
import static com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "会员端 - AI 聊天消息")
@RestController
@RequestMapping("/ai/chat/message")
public class AppAiChatMessageController {

    @Resource
    private AiChatMessageService chatMessageService;
    @Resource
    private AiChatConversationService chatConversationService;
    @Resource
    private AiChatRoleService chatRoleService;

    @PostMapping("/send")
    @Operation(summary = "发送会员 AI 聊天消息")
    public CommonResult<AppAiChatMessageSendRespVO> sendMessage(
            @Valid @RequestBody AppAiChatMessageSendReqVO reqVO) {
        AiChatMessageSendRespVO result = chatMessageService.sendMessage(toAdminReq(reqVO),
                AiChatOwnerTypeEnum.MEMBER.name(), getLoginUserId());
        return success(BeanUtils.toBean(result, AppAiChatMessageSendRespVO.class));
    }

    @PostMapping(value = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式发送会员 AI 聊天消息")
    public Flux<CommonResult<AiChatMessageSendRespVO>> sendChatMessageStream(
            @Valid @RequestBody AppAiChatMessageSendReqVO reqVO) {
        return chatMessageService.sendChatMessageStream(toAdminReq(reqVO),
                AiChatOwnerTypeEnum.MEMBER.name(), getLoginUserId());
    }

    @GetMapping("/list")
    @Operation(summary = "获取会员 AI 聊天消息")
    @Parameter(name = "conversationId", required = true)
    public CommonResult<List<AppAiChatMessageRespVO>> listMessages(@RequestParam("conversationId") Long conversationId) {
        // The owner-bound lookup is deliberately performed before loading message rows.
        chatConversationService.getOwnedConversation(conversationId, AiChatOwnerTypeEnum.MEMBER.name(), getLoginUserId());
        List<AiChatMessageDO> messages = chatMessageService.getChatMessageListByConversationId(conversationId);
        return success(CollUtil.isEmpty(messages) ? Collections.emptyList()
                : BeanUtils.toBean(messages, AppAiChatMessageRespVO.class));
    }

    @GetMapping("/role/simple-list")
    @Operation(summary = "获取会员可用 AI 聊天角色")
    public CommonResult<List<AppAiChatRoleSimpleRespVO>> getRoleSimpleList() {
        List<AiChatRoleDO> roles = chatRoleService.getMemberEnabledChatRoleList();
        return success(BeanUtils.toBean(roles, AppAiChatRoleSimpleRespVO.class));
    }

    private AiChatMessageSendReqVO toAdminReq(AppAiChatMessageSendReqVO reqVO) {
        return BeanUtils.toBean(reqVO, AiChatMessageSendReqVO.class);
    }
}
