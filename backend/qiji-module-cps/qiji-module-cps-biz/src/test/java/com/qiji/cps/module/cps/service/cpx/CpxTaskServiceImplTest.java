package com.qiji.cps.module.cps.service.cpx;

import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxArticleSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxDashboardRespVO;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxPlatformProfileSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxTaskSaveReqVO;
import com.qiji.cps.module.cps.controller.app.cpx.vo.AppCpxTrackingLinkCreateReqVO;
import com.qiji.cps.module.cps.controller.openapi.cpx.vo.CpxEventCreateReqVO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxArticleDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxConversionDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxEventDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxPlatformProfileDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxSettlementRecordDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTaskDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTrackingLinkDO;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxArticleMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxConversionMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxEventMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxPlatformProfileMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxSettlementRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxTaskMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxTrackingLinkMapper;
import com.qiji.cps.module.cps.enums.CpxPromotionMethodEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpxTaskServiceImplTest {

    @InjectMocks
    private CpxTaskServiceImpl taskService;

    @Mock
    private CpxTaskMapper taskMapper;
    @Mock
    private CpxTrackingLinkMapper trackingLinkMapper;
    @Mock
    private CpxEventMapper eventMapper;
    @Mock
    private CpxConversionMapper conversionMapper;
    @Mock
    private CpxArticleMapper articleMapper;
    @Mock
    private CpxPlatformProfileMapper platformProfileMapper;
    @Mock
    private CpxSettlementRecordMapper settlementRecordMapper;

    @Test
    @DisplayName("createTask - 非 CPS 点击类任务默认关闭会员奖励并保留 CPS 优先级语义")
    void createTask_disablesClickRewardByDefaultAndPersistsPromotionMethod() {
        CpxTaskSaveReqVO request = new CpxTaskSaveReqVO();
        request.setTaskName("下载激活任务");
        request.setPlatformCode("mock");
        request.setPromotionMethod(CpxPromotionMethodEnum.OCPC.name());
        request.setRewardAmount(120);
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(7));

        when(taskMapper.insert(any(CpxTaskDO.class))).thenAnswer(invocation -> {
            CpxTaskDO task = invocation.getArgument(0);
            task.setId(10L);
            return 1;
        });

        Long taskId = taskService.createTask(request);

        assertEquals(10L, taskId);
        ArgumentCaptor<CpxTaskDO> captor = ArgumentCaptor.forClass(CpxTaskDO.class);
        verify(taskMapper).insert(captor.capture());
        assertEquals(CpxPromotionMethodEnum.OCPC.name(), captor.getValue().getPromotionMethod());
        assertFalse(captor.getValue().getMemberRewardEnabled());
        assertTrue(captor.getValue().getTaskNo().startsWith("CPX"));
        assertNotNull(captor.getValue().getPriority());
    }

    @Test
    @DisplayName("listAdminTasks - 后台任务列表包含草稿且 CPS 任务优先")
    void listAdminTasks_includesDraftTasksAndKeepsCpsFirst() {
        when(taskMapper.selectAdminList(null, null)).thenReturn(List.of(
                CpxTaskDO.builder()
                        .id(2L)
                        .taskName("CPA 拉新")
                        .promotionMethod(CpxPromotionMethodEnum.CPA.name())
                        .priority(1)
                        .status(CpxTaskConstants.STATUS_DRAFT)
                        .build(),
                CpxTaskDO.builder()
                        .id(1L)
                        .taskName("CPS 商品")
                        .promotionMethod(CpxPromotionMethodEnum.CPS.name())
                        .priority(99)
                        .status(CpxTaskConstants.STATUS_ONLINE)
                        .build()
        ));

        List<CpxTaskDO> tasks = taskService.listAdminTasks(null, null, null);

        assertEquals(2, tasks.size());
        assertEquals(CpxPromotionMethodEnum.CPS.name(), tasks.get(0).getPromotionMethod());
        assertEquals(CpxPromotionMethodEnum.CPA.name(), tasks.get(1).getPromotionMethod());
    }

    @Test
    @DisplayName("generateTrackingLink - 用可信会员身份生成 CPX trackingId")
    void generateTrackingLink_persistsMemberScopedTracking() {
        when(taskMapper.selectById(10L)).thenReturn(CpxTaskDO.builder()
                .id(10L)
                .taskName("CPS 商品主推")
                .platformCode("taobao")
                .promotionMethod(CpxPromotionMethodEnum.CPS.name())
                .landingUrl("https://example.com/landing")
                .status(CpxTaskConstants.STATUS_ONLINE)
                .build());
        when(trackingLinkMapper.insert(any(CpxTrackingLinkDO.class))).thenAnswer(invocation -> {
            CpxTrackingLinkDO link = invocation.getArgument(0);
            link.setId(20L);
            return 1;
        });

        AppCpxTrackingLinkCreateReqVO request = new AppCpxTrackingLinkCreateReqVO();
        request.setTaskId(10L);
        request.setAdzoneId("zone-1");
        request.setChannelCode("iot");

        CpxTrackingLinkDO link = taskService.generateTrackingLink(request, 100L);

        assertEquals(20L, link.getId());
        assertEquals(100L, link.getMemberId());
        assertTrue(link.getTrackingId().startsWith("CPX"));
        assertTrue(link.getTrackingUrl().contains("tracking_id=" + link.getTrackingId()));
        verify(trackingLinkMapper).insert(any(CpxTrackingLinkDO.class));
    }

    @Test
    @DisplayName("recordEvent - 按幂等键重复上报时复用已有事件")
    void recordEvent_reusesExistingEventByIdempotencyKey() {
        CpxEventDO existing = CpxEventDO.builder()
                .id(30L)
                .idempotencyKey("tenant-1:mock:CPC:click-1")
                .sourceEventId("click-1")
                .eventType(CpxTaskConstants.EVENT_CLICK)
                .build();
        when(eventMapper.selectByIdempotencyKey("tenant-1:mock:CPC:click-1")).thenReturn(existing);

        CpxEventCreateReqVO request = new CpxEventCreateReqVO();
        request.setTaskId(10L);
        request.setPlatformCode("mock");
        request.setPromotionMethod(CpxPromotionMethodEnum.CPC.name());
        request.setEventType(CpxTaskConstants.EVENT_CLICK);
        request.setSourceEventId("click-1");
        request.setIdempotencyKey("tenant-1:mock:CPC:click-1");

        CpxEventDO event = taskService.recordEvent(request);

        assertEquals(30L, event.getId());
        verify(eventMapper, never()).insert(any(CpxEventDO.class));
    }

    @Test
    @DisplayName("updateArticle - 后台资讯更新会规范化 oCPA 枚举")
    void updateArticle_normalizesPromotionMethodBeforePersisting() {
        CpxArticleSaveReqVO request = new CpxArticleSaveReqVO();
        request.setId(40L);
        request.setTitle("oCPA 投放攻略");
        request.setCategory("广告优化");
        request.setPromotionMethod("oCPA");
        request.setContent("content");

        taskService.updateArticle(request);

        ArgumentCaptor<CpxArticleDO> captor = ArgumentCaptor.forClass(CpxArticleDO.class);
        verify(articleMapper).updateById(captor.capture());
        assertEquals(40L, captor.getValue().getId());
        assertEquals(CpxPromotionMethodEnum.OCPA.name(), captor.getValue().getPromotionMethod());
    }

    @Test
    @DisplayName("updatePlatformProfile - 后台平台档案更新写入平台能力配置")
    void updatePlatformProfile_persistsIntegrationMetadata() {
        CpxPlatformProfileSaveReqVO request = new CpxPlatformProfileSaveReqVO();
        request.setId(50L);
        request.setPlatformCode("mock_union");
        request.setPlatformName("Mock Union");
        request.setSupportedMethods("CPS,CPA,CPL,CPM,CPC,OCPA,OCPC");
        request.setCallbackUrl("https://mock.example.com/callback");
        request.setStatus(CpxTaskConstants.STATUS_ONLINE);

        taskService.updatePlatformProfile(request);

        ArgumentCaptor<CpxPlatformProfileDO> captor = ArgumentCaptor.forClass(CpxPlatformProfileDO.class);
        verify(platformProfileMapper).updateById(captor.capture());
        assertEquals(50L, captor.getValue().getId());
        assertEquals("mock_union", captor.getValue().getPlatformCode());
        assertEquals("CPS,CPA,CPL,CPM,CPC,OCPA,OCPC", captor.getValue().getSupportedMethods());
    }

    @Test
    @DisplayName("getDashboardSummary - 汇总 CPS 主导的 CPX 漏斗和结算金额")
    void getDashboardSummary_aggregatesFunnelAndSettlement() {
        when(taskMapper.selectAdminList(null, null)).thenReturn(List.of(
                CpxTaskDO.builder().id(1L).promotionMethod(CpxPromotionMethodEnum.CPS.name())
                        .status(CpxTaskConstants.STATUS_ONLINE).build(),
                CpxTaskDO.builder().id(2L).promotionMethod(CpxPromotionMethodEnum.CPM.name())
                        .status(CpxTaskConstants.STATUS_ONLINE).build(),
                CpxTaskDO.builder().id(3L).promotionMethod(CpxPromotionMethodEnum.CPC.name())
                        .status(CpxTaskConstants.STATUS_DRAFT).build()
        ));
        when(eventMapper.selectList()).thenReturn(List.of(
                CpxEventDO.builder().eventType(CpxTaskConstants.EVENT_IMPRESSION).validFlag(true).build(),
                CpxEventDO.builder().eventType(CpxTaskConstants.EVENT_IMPRESSION).validFlag(false).build(),
                CpxEventDO.builder().eventType(CpxTaskConstants.EVENT_CLICK).validFlag(true).build(),
                CpxEventDO.builder().eventType(CpxTaskConstants.EVENT_LEAD).validFlag(true).build(),
                CpxEventDO.builder().eventType(CpxTaskConstants.EVENT_ACTION).validFlag(true).build()
        ));
        when(conversionMapper.selectList()).thenReturn(List.of(
                CpxConversionDO.builder().promotionMethod(CpxPromotionMethodEnum.CPA.name()).build(),
                CpxConversionDO.builder().promotionMethod(CpxPromotionMethodEnum.OCPA.name()).build()
        ));
        when(settlementRecordMapper.selectList()).thenReturn(List.of(
                CpxSettlementRecordDO.builder().amount(1000).rewardAmount(300).build(),
                CpxSettlementRecordDO.builder().amount(2500).rewardAmount(null).build()
        ));

        CpxDashboardRespVO summary = taskService.getDashboardSummary();

        assertEquals(3, summary.getTaskCount());
        assertEquals(2, summary.getOnlineTaskCount());
        assertEquals(1L, summary.getTaskCountByMethod().get(CpxPromotionMethodEnum.CPS.name()));
        assertEquals(1, summary.getImpressionCount());
        assertEquals(1, summary.getClickCount());
        assertEquals(1, summary.getLeadCount());
        assertEquals(1, summary.getActionCount());
        assertEquals(2, summary.getConversionCount());
        assertEquals(2, summary.getSettlementCount());
        assertEquals(3500, summary.getSettlementAmount());
        assertEquals(300, summary.getRewardAmount());
    }
}
