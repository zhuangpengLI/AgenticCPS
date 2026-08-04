package com.qiji.cps.module.ai.service.chat.tool;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiToolDO;
import com.qiji.cps.module.ai.service.model.AiChatRoleService;
import com.qiji.cps.module.ai.service.model.AiToolService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.ai.enums.ErrorCodeConstants.CHAT_TOOL_INTENT_NOT_ALLOWED;

@Service
public class AiChatToolActionService {

    @Resource
    private AiChatRoleService chatRoleService;
    @Resource
    private AiToolService toolService;
    @Autowired(required = false)
    private List<AiChatToolActionProvider> providers = Collections.emptyList();

    public List<AiChatToolAction> getAvailableActions(AiChatConversationDO conversation) {
        Set<String> roleToolNames = getRoleToolNames(conversation);
        if (CollUtil.isEmpty(roleToolNames)) {
            return Collections.emptyList();
        }
        return allActions().stream().filter(action -> roleToolNames.contains(action.getToolName())).toList();
    }

    public AiChatToolAction requireAllowedAction(AiChatConversationDO conversation, String intent) {
        if (StrUtil.isBlank(intent)) {
            return null;
        }
        return getAvailableActions(conversation).stream()
                .filter(action -> intent.equals(action.getIntent()))
                .findFirst().orElseThrow(() -> exception(CHAT_TOOL_INTENT_NOT_ALLOWED, intent));
    }

    public Map<String, AiChatToolAction> getAvailableActionsByToolName(AiChatConversationDO conversation) {
        return getAvailableActions(conversation).stream().collect(Collectors.toMap(AiChatToolAction::getToolName,
                Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private List<AiChatToolAction> allActions() {
        return providers.stream().flatMap(provider -> provider.getToolActions().stream()).toList();
    }

    private Set<String> getRoleToolNames(AiChatConversationDO conversation) {
        if (conversation == null || conversation.getRoleId() == null) {
            return Collections.emptySet();
        }
        AiChatRoleDO role = chatRoleService.getChatRole(conversation.getRoleId());
        if (role == null || CollUtil.isEmpty(role.getToolIds())) {
            return Collections.emptySet();
        }
        return toolService.getToolList(role.getToolIds()).stream().map(AiToolDO::getName).collect(Collectors.toSet());
    }

}
