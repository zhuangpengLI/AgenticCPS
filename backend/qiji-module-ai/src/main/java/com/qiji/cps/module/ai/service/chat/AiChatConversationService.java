package com.qiji.cps.module.ai.service.chat;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMyReqVO;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMcpTestReqVO;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationPageReqVO;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationUpdateMyReqVO;
import com.qiji.cps.module.ai.controller.admin.chat.vo.message.AiChatMessageRespVO;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;

import java.util.List;

/**
 * AI 聊天对话 Service 接口
 *
 * @author fansili
 */
public interface AiChatConversationService {

    /**
     * 创建【我的】聊天对话
     *
     * @param createReqVO 创建信息
     * @param userId 用户编号
     * @return 编号
     */
    Long createChatConversationMy(AiChatConversationCreateMyReqVO createReqVO, Long userId);

    /**
     * 创建会员聊天对话。会员身份由登录上下文提供，不接受请求体中的身份字段。
     *
     * @param createReqVO 创建信息
     * @param memberId    会员编号
     * @return 编号
     */
    Long createMemberConversation(AiChatConversationCreateMyReqVO createReqVO, Long memberId);

    Long createMcpTestConversation(AiChatConversationCreateMcpTestReqVO createReqVO, Long adminUserId);

    /**
     * 更新【我的】聊天对话
     *
     * @param updateReqVO 更新信息
     * @param userId 用户编号
     */
    void updateChatConversationMy(AiChatConversationUpdateMyReqVO updateReqVO, Long userId);

    /**
     * 按显式所有者更新对话。
     */
    void updateChatConversation(AiChatConversationUpdateMyReqVO updateReqVO, String ownerUserType, Long ownerId);

    /**
     * 获得【我的】聊天对话列表
     *
     * @param userId 用户编号
     * @return 聊天对话列表
     */
    List<AiChatConversationDO> getChatConversationListByUserId(Long userId);

    /**
     * 按所有者类型和所有者编号获得对话列表。
     */
    List<AiChatConversationDO> getChatConversationListByOwner(String ownerUserType, Long ownerId);

    /**
     * 获得聊天对话
     *
     * @param id 编号
     * @return 聊天对话
     */
    AiChatConversationDO getChatConversation(Long id);

    /**
     * 按显式所有者获得对话，避免仅使用 user_id 造成管理员和会员身份串线。
     */
    AiChatConversationDO getOwnedConversation(Long id, String ownerUserType, Long ownerId);

    /**
     * 删除【我的】聊天对话
     *
     * @param id 编号
     * @param userId 用户编号
     */
    void deleteChatConversationMy(Long id, Long userId);

    /**
     * 按显式所有者删除对话。
     */
    void deleteChatConversation(Long id, String ownerUserType, Long ownerId);

    /**
     * 【管理员】删除聊天对话
     *
     * @param id 编号
     */
    void deleteChatConversationByAdmin(Long id);

    /**
     * 校验聊天对话是否存在
     *
     * @param id 编号
     * @return 聊天对话
     */
    AiChatConversationDO validateChatConversationExists(Long id);

    /**
     * 删除【我的】 + 非置顶的聊天对话
     *
     * @param userId 用户编号
     */
    void deleteChatConversationMyByUnpinned(Long userId);

    /**
     * 获得聊天对话的分页列表
     *
     * @param pageReqVO 分页查询
     * @return 聊天对话的分页列表
     */
    PageResult<AiChatConversationDO> getChatConversationPage(AiChatConversationPageReqVO pageReqVO);

}
