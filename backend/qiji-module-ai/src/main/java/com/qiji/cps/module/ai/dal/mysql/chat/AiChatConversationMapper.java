package com.qiji.cps.module.ai.dal.mysql.chat;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationPageReqVO;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.qiji.cps.module.ai.enums.chat.AiChatOwnerTypeEnum.ADMIN;

/**
 * AI 聊天对话 Mapper
 *
 * @author AgenticCPS源码
 */
@Mapper
public interface AiChatConversationMapper extends BaseMapperX<AiChatConversationDO> {

    default List<AiChatConversationDO> selectListByUserId(Long userId) {
        return selectList(AiChatConversationDO::getUserId, userId);
    }

    default List<AiChatConversationDO> selectListByOwnerUserTypeAndUserId(String ownerUserType, Long userId) {
        LambdaQueryWrapperX<AiChatConversationDO> query = new LambdaQueryWrapperX<AiChatConversationDO>()
                .eq(AiChatConversationDO::getUserId, userId);
        if (ADMIN.name().equals(ownerUserType)) {
            query.and(wrapper -> wrapper.eq(AiChatConversationDO::getOwnerUserType, ownerUserType)
                    .or().isNull(AiChatConversationDO::getOwnerUserType));
        } else {
            query.eq(AiChatConversationDO::getOwnerUserType, ownerUserType);
        }
        return selectList(query);
    }

    default AiChatConversationDO selectByIdAndOwnerUserTypeAndUserId(Long id, String ownerUserType, Long userId) {
        LambdaQueryWrapperX<AiChatConversationDO> query = new LambdaQueryWrapperX<AiChatConversationDO>()
                .eq(AiChatConversationDO::getId, id)
                .eq(AiChatConversationDO::getUserId, userId);
        if (ADMIN.name().equals(ownerUserType)) {
            query.and(wrapper -> wrapper.eq(AiChatConversationDO::getOwnerUserType, ownerUserType)
                    .or().isNull(AiChatConversationDO::getOwnerUserType));
        } else {
            query.eq(AiChatConversationDO::getOwnerUserType, ownerUserType);
        }
        return selectOne(query);
    }

    default List<AiChatConversationDO> selectListByUserIdAndPinned(Long userId, boolean pinned) {
        return selectList(new LambdaQueryWrapperX<AiChatConversationDO>()
                .eq(AiChatConversationDO::getUserId, userId)
                .eq(AiChatConversationDO::getPinned, pinned));
    }

    default List<AiChatConversationDO> selectListByOwnerUserTypeAndUserIdAndPinned(String ownerUserType,
                                                                                     Long userId,
                                                                                     boolean pinned) {
        LambdaQueryWrapperX<AiChatConversationDO> query = new LambdaQueryWrapperX<AiChatConversationDO>()
                .eq(AiChatConversationDO::getUserId, userId)
                .eq(AiChatConversationDO::getPinned, pinned);
        if (ADMIN.name().equals(ownerUserType)) {
            query.and(wrapper -> wrapper.eq(AiChatConversationDO::getOwnerUserType, ownerUserType)
                    .or().isNull(AiChatConversationDO::getOwnerUserType));
        } else {
            query.eq(AiChatConversationDO::getOwnerUserType, ownerUserType);
        }
        return selectList(query);
    }

    default PageResult<AiChatConversationDO> selectChatConversationPage(AiChatConversationPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<AiChatConversationDO>()
                .eqIfPresent(AiChatConversationDO::getUserId, pageReqVO.getUserId())
                .likeIfPresent(AiChatConversationDO::getTitle, pageReqVO.getTitle())
                .betweenIfPresent(AiChatConversationDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(AiChatConversationDO::getId));
    }

}
