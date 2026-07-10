package com.qiji.cps.module.ai.controller.app.chat;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.ai.controller.app.chat.vo.role.AppAiChatRoleSimpleRespVO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.qiji.cps.module.ai.service.model.AiChatRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "会员端 - AI 聊天角色")
@RestController
@RequestMapping("/ai/chat/role")
public class AppAiChatRoleController {

    @Resource
    private AiChatRoleService chatRoleService;

    @GetMapping("/simple-list")
    @Operation(summary = "获取会员可用 AI 聊天角色")
    public CommonResult<List<AppAiChatRoleSimpleRespVO>> getRoleSimpleList() {
        List<AiChatRoleDO> roles = chatRoleService.getMemberEnabledChatRoleList();
        return success(BeanUtils.toBean(roles, AppAiChatRoleSimpleRespVO.class));
    }
}
