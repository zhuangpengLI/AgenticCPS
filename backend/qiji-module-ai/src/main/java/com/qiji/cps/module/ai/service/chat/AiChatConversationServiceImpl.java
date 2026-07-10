package com.qiji.cps.module.ai.service.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.qiji.cps.module.ai.enums.model.AiModelTypeEnum;
import com.qiji.cps.module.ai.enums.chat.AiChatModeEnum;
import com.qiji.cps.module.ai.enums.chat.AiChatOwnerTypeEnum;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMyReqVO;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationPageReqVO;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationUpdateMyReqVO;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiModelDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.qiji.cps.module.ai.dal.mysql.chat.AiChatConversationMapper;
import com.qiji.cps.module.ai.service.knowledge.AiKnowledgeService;
import com.qiji.cps.module.ai.service.model.AiModelService;
import com.qiji.cps.module.ai.service.model.AiChatRoleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.framework.common.util.collection.CollectionUtils.convertList;
import static com.qiji.cps.module.ai.enums.ErrorCodeConstants.CHAT_CONVERSATION_MODEL_ERROR;
import static com.qiji.cps.module.ai.enums.ErrorCodeConstants.CHAT_CONVERSATION_NOT_EXISTS;

/**
 * AI 聊天对话 Service 实现类
 *
 * @author fansili
 */
@Service
@Validated
@Slf4j
public class AiChatConversationServiceImpl implements AiChatConversationService {

    @Resource
    private AiChatConversationMapper chatConversationMapper;

    @Resource
    private AiModelService modalService;
    @Resource
    private AiChatRoleService chatRoleService;
    @Resource
    private AiKnowledgeService knowledgeService;

    @Override
    public Long createChatConversationMy(AiChatConversationCreateMyReqVO createReqVO, Long userId) {
        return createConversation(createReqVO, userId, AiChatOwnerTypeEnum.ADMIN, null);
    }

    @Override
    public Long createMemberConversation(AiChatConversationCreateMyReqVO createReqVO, Long memberId) {
        return createConversation(createReqVO, memberId, AiChatOwnerTypeEnum.MEMBER, memberId);
    }

    private Long createConversation(AiChatConversationCreateMyReqVO createReqVO, Long ownerId,
                                    AiChatOwnerTypeEnum ownerType, Long memberId) {
        // 1.1 获得 AiChatRoleDO 聊天角色
        AiChatRoleDO role = null;
        if (createReqVO.getRoleId() != null) {
            role = ownerType == AiChatOwnerTypeEnum.MEMBER
                    ? chatRoleService.validateMemberEnabledChatRole(createReqVO.getRoleId())
                    : chatRoleService.validateChatRole(createReqVO.getRoleId());
        }
        // 1.2 获得 AiModelDO 聊天模型
        AiModelDO model = role != null && role.getModelId() != null ? modalService.validateModel(role.getModelId())
                : modalService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
        Assert.notNull(model, "必须找到默认模型");
        validateChatModel(model);

        // 1.3 校验知识库
        if (Objects.nonNull(createReqVO.getKnowledgeId())) {
            knowledgeService.validateKnowledgeExists(createReqVO.getKnowledgeId());
        }

        // 2. 创建 AiChatConversationDO 聊天对话
        AiChatConversationDO conversation = new AiChatConversationDO().setUserId(ownerId)
                .setOwnerUserType(ownerType.name()).setMemberId(memberId)
                .setChatMode(AiChatModeEnum.STANDARD.name()).setAllowMutation(false).setPinned(false)
                .setModelId(model.getId()).setModel(model.getModel())
                .setTemperature(model.getTemperature()).setMaxTokens(model.getMaxTokens()).setMaxContexts(model.getMaxContexts());
        if (role != null) {
            conversation.setTitle(role.getName()).setRoleId(role.getId()).setSystemMessage(role.getSystemMessage());
        } else {
            conversation.setTitle(AiChatConversationDO.TITLE_DEFAULT);
        }
        chatConversationMapper.insert(conversation);
        return conversation.getId();
    }

    @Override
    public void updateChatConversationMy(AiChatConversationUpdateMyReqVO updateReqVO, Long userId) {
        updateChatConversation(updateReqVO, AiChatOwnerTypeEnum.ADMIN.name(), userId);
    }

    @Override
    public void updateChatConversation(AiChatConversationUpdateMyReqVO updateReqVO, String ownerUserType, Long ownerId) {
        // 1.1 校验对话是否存在
        AiChatConversationDO conversation = getOwnedConversation(updateReqVO.getId(), ownerUserType, ownerId);
        // 1.2 校验模型是否存在（修改模型的情况）
        AiModelDO model = null;
        if (updateReqVO.getModelId() != null) {
            model = modalService.validateModel(updateReqVO.getModelId());
        }

        // 1.3 校验知识库是否存在
        if (updateReqVO.getKnowledgeId() != null) {
            knowledgeService.validateKnowledgeExists(updateReqVO.getKnowledgeId());
        }

        // 2. 更新对话信息
        // Identity fields are intentionally omitted.  Only mutable conversation settings may be patched.
        AiChatConversationDO updateObj = new AiChatConversationDO().setId(updateReqVO.getId());
        if (updateReqVO.getTitle() != null) {
            updateObj.setTitle(updateReqVO.getTitle());
        }
        if (updateReqVO.getPinned() != null) {
            updateObj.setPinned(updateReqVO.getPinned());
        }
        if (updateReqVO.getModelId() != null) {
            updateObj.setModelId(updateReqVO.getModelId());
        }
        if (updateReqVO.getSystemMessage() != null) {
            updateObj.setSystemMessage(updateReqVO.getSystemMessage());
        }
        if (updateReqVO.getTemperature() != null) {
            updateObj.setTemperature(updateReqVO.getTemperature());
        }
        if (updateReqVO.getMaxTokens() != null) {
            updateObj.setMaxTokens(updateReqVO.getMaxTokens());
        }
        if (updateReqVO.getMaxContexts() != null) {
            updateObj.setMaxContexts(updateReqVO.getMaxContexts());
        }
        if (Boolean.TRUE.equals(updateReqVO.getPinned())) {
            updateObj.setPinnedTime(LocalDateTime.now());
        }
        if (model != null) {
            updateObj.setModel(model.getModel());
        }
        chatConversationMapper.updateById(updateObj);
    }

    @Override
    public List<AiChatConversationDO> getChatConversationListByUserId(Long userId) {
        return getChatConversationListByOwner(AiChatOwnerTypeEnum.ADMIN.name(), userId);
    }

    @Override
    public List<AiChatConversationDO> getChatConversationListByOwner(String ownerUserType, Long ownerId) {
        validateOwner(ownerUserType, ownerId);
        List<AiChatConversationDO> list = chatConversationMapper
                .selectListByOwnerUserTypeAndUserId(ownerUserType, ownerId);
        if (AiChatOwnerTypeEnum.MEMBER.name().equals(ownerUserType)) {
            list = list.stream().filter(conversation -> ObjUtil.equal(conversation.getMemberId(), ownerId)).toList();
        }
        return normalizeHistoricalIdentity(list);
    }

    @Override
    public AiChatConversationDO getChatConversation(Long id) {
        return normalizeHistoricalIdentity(chatConversationMapper.selectById(id));
    }

    @Override
    public AiChatConversationDO getOwnedConversation(Long id, String ownerUserType, Long ownerId) {
        validateOwner(ownerUserType, ownerId);
        AiChatConversationDO conversation = chatConversationMapper
                .selectByIdAndOwnerUserTypeAndUserId(id, ownerUserType, ownerId);
        if (conversation == null) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        conversation = normalizeHistoricalIdentity(conversation);
        if (AiChatOwnerTypeEnum.MEMBER.name().equals(ownerUserType)
                && ObjUtil.notEqual(conversation.getMemberId(), ownerId)) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        return conversation;
    }

    @Override
    public void deleteChatConversationMy(Long id, Long userId) {
        deleteChatConversation(id, AiChatOwnerTypeEnum.ADMIN.name(), userId);
    }

    @Override
    public void deleteChatConversation(Long id, String ownerUserType, Long ownerId) {
        getOwnedConversation(id, ownerUserType, ownerId);
        chatConversationMapper.deleteById(id);
    }

    @Override
    public void deleteChatConversationByAdmin(Long id) {
        // 1. 校验对话是否存在
        AiChatConversationDO conversation = validateChatConversationExists(id);
        if (conversation == null) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        // 2. 执行删除
        chatConversationMapper.deleteById(id);
    }

    private void validateChatModel(AiModelDO model) {
        if (ObjectUtil.isAllNotEmpty(model.getTemperature(), model.getMaxTokens(), model.getMaxContexts())) {
            return;
        }
        Assert.equals(model.getType(), AiModelTypeEnum.CHAT.getType(), "模型类型不正确：" + model);
        throw exception(CHAT_CONVERSATION_MODEL_ERROR);
    }

    public AiChatConversationDO validateChatConversationExists(Long id) {
        AiChatConversationDO conversation = chatConversationMapper.selectById(id);
        if (conversation == null) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        return normalizeHistoricalIdentity(conversation);
    }

    @Override
    public void deleteChatConversationMyByUnpinned(Long userId) {
        List<AiChatConversationDO> list = chatConversationMapper.selectListByOwnerUserTypeAndUserIdAndPinned(
                AiChatOwnerTypeEnum.ADMIN.name(), userId, false);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list = list.stream().filter(conversation -> ObjUtil.equal(conversation.getMemberId(), userId)
                || !AiChatOwnerTypeEnum.MEMBER.name().equals(conversation.getOwnerUserType())).toList();
        chatConversationMapper.deleteByIds(convertList(list, AiChatConversationDO::getId));
    }

    @Override
    public PageResult<AiChatConversationDO> getChatConversationPage(AiChatConversationPageReqVO pageReqVO) {
        PageResult<AiChatConversationDO> result = chatConversationMapper.selectChatConversationPage(pageReqVO);
        if (result != null) {
            result.setList(normalizeHistoricalIdentity(result.getList()));
        }
        return result;
    }

    private List<AiChatConversationDO> normalizeHistoricalIdentity(List<AiChatConversationDO> conversations) {
        if (conversations == null) {
            return null;
        }
        conversations.forEach(this::normalizeHistoricalIdentity);
        return conversations;
    }

    private AiChatConversationDO normalizeHistoricalIdentity(AiChatConversationDO conversation) {
        if (conversation == null) {
            return null;
        }
        if (conversation.getOwnerUserType() == null) {
            conversation.setOwnerUserType(AiChatOwnerTypeEnum.ADMIN.name());
        }
        if (conversation.getChatMode() == null) {
            conversation.setChatMode(AiChatModeEnum.STANDARD.name());
        }
        return conversation;
    }

    private void validateOwner(String ownerUserType, Long ownerId) {
        if (ownerUserType == null || ownerId == null) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
    }

}
