package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTaskDO;
import com.qiji.cps.module.cps.dal.dataobject.mcp.CpsMcpAccessLogDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.enums.CpxPromotionMethodEnum;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpxTaskMcpToolFunctionTest {

    @InjectMocks
    private CpxListTasksToolFunction listTasksToolFunction;

    @Mock
    private CpxTaskService taskService;

    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    @DisplayName("cpx_list_tasks - CPS 任务优先返回并写入 MCP 审计")
    void listTasks_returnsCpsFirstAndWritesAudit() {
        when(taskService.listPublishedTasks("家电", null, 5)).thenReturn(List.of(
                CpxTaskDO.builder()
                        .id(1L)
                        .taskName("CPS 家电成交返利")
                        .platformCode("jd")
                        .promotionMethod(CpxPromotionMethodEnum.CPS.name())
                        .rewardDesc("成交返利")
                        .priority(1)
                        .build(),
                CpxTaskDO.builder()
                        .id(2L)
                        .taskName("下载激活任务")
                        .platformCode("mock")
                        .promotionMethod(CpxPromotionMethodEnum.CPA.name())
                        .rewardDesc("激活奖励")
                        .priority(20)
                        .build()));

        CpxListTasksToolFunction.Request request = new CpxListTasksToolFunction.Request();
        request.setKeyword("家电");
        request.setLimit(5);
        var response = listTasksToolFunction.apply(request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(2, response.getTasks().size());
        assertEquals(CpxPromotionMethodEnum.CPS.name(), response.getTasks().get(0).getPromotionMethod());
        ArgumentCaptor<CpsMcpAccessLogDO> logCaptor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals("cpx_list_tasks", logCaptor.getValue().getToolName());
        assertEquals(1, logCaptor.getValue().getStatus());
        verify(taskService).listPublishedTasks("家电", null, 5);
    }
}
