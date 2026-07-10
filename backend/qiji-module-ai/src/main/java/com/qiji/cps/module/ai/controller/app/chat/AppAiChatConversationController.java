package com.qiji.cps.module.ai.controller.app.chat;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMyReqVO;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationUpdateMyReqVO;
import com.qiji.cps.module.ai.controller.app.chat.vo.conversation.AppAiChatConversationCreateReqVO;
import com.qiji.cps.module.ai.controller.app.chat.vo.conversation.AppAiChatConversationRespVO;
import com.qiji.cps.module.ai.controller.app.chat.vo.conversation.AppAiChatConversationUpdateReqVO;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.enums.chat.AiChatOwnerTypeEnum;
import com.qiji.cps.module.ai.service.chat.AiChatConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;
import static com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "会员端 - AI 聊天会话")
@RestController
@RequestMapping("/ai/chat/conversation")
@Validated
public class AppAiChatConversationController {

    @Resource
    private AiChatConversationService chatConversationService;

    @PostMapping("/create")
    @Operation(summary = "创建会员 AI 聊天会话")
    public CommonResult<Long> create(@RequestBody @Valid AppAiChatConversationCreateReqVO reqVO) {
        return success(chatConversationService.createMemberConversation(
                BeanUtils.toBean(reqVO, AiChatConversationCreateMyReqVO.class), getLoginUserId()));
    }

    @GetMapping("/list")
    @Operation(summary = "获取当前会员的 AI 聊天会话")
    public CommonResult<List<AppAiChatConversationRespVO>> list() {
        List<AiChatConversationDO> conversations = chatConversationService.getChatConversationListByOwner(
                AiChatOwnerTypeEnum.MEMBER.name(), getLoginUserId());
        return success(BeanUtils.toBean(conversations, AppAiChatConversationRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取会员 AI 聊天会话")
    @Parameter(name = "id", required = true)
    public CommonResult<AppAiChatConversationRespVO> get(@RequestParam("id") Long id) {
        AiChatConversationDO conversation = chatConversationService.getOwnedConversation(
                id, AiChatOwnerTypeEnum.MEMBER.name(), getLoginUserId());
        return success(BeanUtils.toBean(conversation, AppAiChatConversationRespVO.class));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会员 AI 聊天会话")
    public CommonResult<Boolean> update(@RequestBody @Valid AppAiChatConversationUpdateReqVO reqVO) {
        chatConversationService.updateChatConversation(BeanUtils.toBean(reqVO, AiChatConversationUpdateMyReqVO.class),
                AiChatOwnerTypeEnum.MEMBER.name(), getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员 AI 聊天会话")
    @Parameter(name = "id", required = true)
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        chatConversationService.deleteChatConversation(id, AiChatOwnerTypeEnum.MEMBER.name(), getLoginUserId());
        return success(true);
    }
}
