package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncLogDO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncLogMapper;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
import com.qiji.cps.module.member.api.user.MemberUserApi;
import com.qiji.cps.module.member.api.user.dto.MemberUserRespDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
class CpsOrderServiceImplTest {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @InjectMocks
    private CpsOrderServiceImpl orderService;

    @Mock
    private CpsOrderMapper orderMapper;
    @Mock
    private CpsOrderSyncLogMapper syncLogMapper;
    @Mock
    private CpsPlatformClientFactory platformClientFactory;
    @Mock
    private CpsRebateSettleService rebateSettleService;
    @Mock
    private CpsPlatformClient platformClient;
    @Mock
    private CpsTransferRecordMapper transferRecordMapper;
    @Mock
    private MemberUserApi memberUserApi;
    @Mock
    private CpsAdzoneMapper adzoneMapper;

    @Test
    @DisplayName("deleteOrder - 删除订单前校验订单存在")
    void deleteOrder_validatesExistsThenDeletes() {
        when(orderMapper.selectById(7L)).thenReturn(CpsOrderDO.builder().id(7L).build());

        orderService.deleteOrder(7L);

        verify(orderMapper).deleteById(7L);
    }

    @Test
    @DisplayName("deleteOrderList - 批量删除订单时逐个校验存在")
    void deleteOrderList_validatesAndDeletesEachOrder() {
        when(orderMapper.selectById(7L)).thenReturn(CpsOrderDO.builder().id(7L).build());
        when(orderMapper.selectById(8L)).thenReturn(CpsOrderDO.builder().id(8L).build());

        orderService.deleteOrderList(List.of(7L, 8L));

        verify(orderMapper).deleteById(7L);
        verify(orderMapper).deleteById(8L);
    }

    @Test
    @DisplayName("deleteOrder - 订单不存在时抛出业务异常")
    void deleteOrder_throwsWhenMissing() {
        when(orderMapper.selectById(7L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> orderService.deleteOrder(7L));

        verify(orderMapper, never()).deleteById(7L);
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 已到账订单收到退款状态时触发返利扣回")
    void saveOrUpdateOrder_reverseRebateWhenRefundedAfterCredited() {
        CpsOrderDO existing = CpsOrderDO.builder()
                .id(1L)
                .platformOrderId("TB-1")
                .platformCode("taobao")
                .orderStatus(CpsOrderStatusEnum.REBATE_RECEIVED.getStatus())
                .commissionAmount(new BigDecimal("12.00"))
                .rebateTime(LocalDateTime.now().minusDays(1))
                .build();
        when(orderMapper.selectByPlatformOrderId("TB-1")).thenReturn(existing);
        when(rebateSettleService.reverseRebate(1L)).thenReturn(true);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-1")
                .platformCode("taobao")
                .commissionAmount(new BigDecimal("12.00"))
                .refundTag(1)
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(2, result);
        verify(rebateSettleService).reverseRebate(1L);
        verify(orderMapper).updateById(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                order.getId().equals(1L)
                        && CpsOrderStatusEnum.REFUNDED.getStatus().equals(order.getOrderStatus())
                        && order.getRefundTime() != null));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 已到账订单遇到较早平台状态时不回滚")
    void saveOrUpdateOrder_doesNotRollbackCreditedOrderStatus() {
        CpsOrderDO existing = CpsOrderDO.builder()
                .id(2L)
                .platformOrderId("TB-2")
                .platformCode("taobao")
                .orderStatus(CpsOrderStatusEnum.REBATE_RECEIVED.getStatus())
                .commissionAmount(new BigDecimal("12.00"))
                .rebateTime(LocalDateTime.now().minusHours(2))
                .build();
        when(orderMapper.selectByPlatformOrderId("TB-2")).thenReturn(existing);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-2")
                .platformCode("taobao")
                .platformStatus(3)
                .commissionAmount(new BigDecimal("15.00"))
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(2, result);
        verify(rebateSettleService, never()).reverseRebate(2L);
        verify(orderMapper).updateById(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                order.getId().equals(2L)
                        && CpsOrderStatusEnum.REBATE_RECEIVED.getStatus().equals(order.getOrderStatus())
                        && new BigDecimal("15.00").compareTo(order.getCommissionAmount()) == 0));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 重新同步应修正已有订单金额快照和会员归因")
    void saveOrUpdateOrder_refreshesExistingOrderSnapshotAndAttribution() {
        CpsOrderDO existing = CpsOrderDO.builder()
                .id(3L)
                .platformOrderId("TB-3")
                .platformCode("taobao")
                .orderStatus(CpsOrderStatusEnum.PAID.getStatus())
                .itemPrice(new BigDecimal("999.00"))
                .finalPrice(BigDecimal.ZERO)
                .commissionRate(new BigDecimal("4.5"))
                .commissionAmount(BigDecimal.ZERO)
                .estimateRebate(BigDecimal.ZERO)
                .build();
        when(orderMapper.selectByPlatformOrderId("TB-3")).thenReturn(existing);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-3")
                .platformCode("taobao")
                .parentOrderId("TB-PARENT-3")
                .itemId("ITEM-3")
                .itemTitle("旗舰婴儿推车")
                .itemPrice(new BigDecimal("999.00"))
                .finalPrice(new BigDecimal("399.00"))
                .commissionRate(new BigDecimal("4.5"))
                .commissionAmount(new BigDecimal("17.96"))
                .platformStatus(1)
                .adzoneId("mm_111_222_333")
                .externalId("1002")
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(2, result);
        verify(orderMapper).updateById(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                order.getId().equals(3L)
                        && new BigDecimal("399.00").compareTo(order.getFinalPrice()) == 0
                        && new BigDecimal("17.96").compareTo(order.getCommissionAmount()) == 0
                        && new BigDecimal("14.37").compareTo(order.getEstimateRebate()) == 0
                        && Long.valueOf(1002L).equals(order.getMemberId())
                        && "1002".equals(order.getExternalInfo())
                        && "ITEM-3".equals(order.getItemId())));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 平台未返回券额时应由原价和券后价推导优惠金额")
    void saveOrUpdateOrder_derivesCouponAmountFromOriginalAndFinalPrice() {
        when(orderMapper.selectByPlatformOrderId("TB-COUPON-1")).thenReturn(null);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-COUPON-1")
                .platformCode("taobao")
                .itemPrice(new BigDecimal("689.00"))
                .finalPrice(new BigDecimal("89.00"))
                .couponAmount(BigDecimal.ZERO)
                .commissionAmount(new BigDecimal("6.68"))
                .platformStatus(1)
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(1, result);
        verify(orderMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                new BigDecimal("689.00").compareTo(order.getItemPrice()) == 0
                        && new BigDecimal("89.00").compareTo(order.getFinalPrice()) == 0
                        && new BigDecimal("600.00").compareTo(order.getCouponAmount()) == 0));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - externalId 为空时唯一转链记录应兜底归因并回写订单号")
    void saveOrUpdateOrder_attributesByUniqueTransferRecordWhenExternalIdMissing() {
        when(orderMapper.selectByPlatformOrderId("TB-FALLBACK-1")).thenReturn(null);
        when(transferRecordMapper.selectAttributionCandidates(eq("taobao"), eq("ITEM-1"),
                eq("mm_111_222_333"), any(), any())).thenReturn(List.of(CpsTransferRecordDO.builder()
                .id(10L)
                .memberId(1001L)
                .platformCode("taobao")
                .itemId("ITEM-1")
                .adzoneId("mm_111_222_333")
                .build()));
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(1001L);
        member.setNickname("我是喵团员");
        when(memberUserApi.getUser(1001L)).thenReturn(member);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-FALLBACK-1")
                .platformCode("taobao")
                .itemId("ITEM-1")
                .adzoneId("mm_111_222_333")
                .orderTime("2026-07-06 20:59:00")
                .platformStatus(1)
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(1, result);
        verify(orderMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                Long.valueOf(1001L).equals(order.getMemberId())
                        && "我是喵团员".equals(order.getMemberNickname())
                        && "TB-FALLBACK-1".equals(order.getPlatformOrderId())
                        && "ITEM-1".equals(order.getItemId())));
        verify(transferRecordMapper).updatePlatformOrderId(10L, "TB-FALLBACK-1");
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 淘宝订单推广位为数字ID时应降级用唯一转链记录归因")
    void saveOrUpdateOrder_attributesByUniqueTransferRecordWhenTaobaoAdzoneIdDiffersFromPid() {
        when(orderMapper.selectByPlatformOrderId("3311726376544025983")).thenReturn(null);
        when(transferRecordMapper.selectAttributionCandidates(eq("taobao"), eq("ITEM-1"),
                eq("333"), any(), any())).thenReturn(List.of());
        when(transferRecordMapper.selectAttributionCandidates(eq("taobao"), eq("ITEM-1"),
                eq(null), any(), any())).thenReturn(List.of(CpsTransferRecordDO.builder()
                .id(10L)
                .memberId(1001L)
                .platformCode("taobao")
                .itemId("ITEM-1")
                .adzoneId("mm_111_222_333")
                .build()));
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(1001L);
        member.setNickname("我是喵团员");
        when(memberUserApi.getUser(1001L)).thenReturn(member);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("3311726376544025983")
                .platformCode("taobao")
                .itemId("ITEM-1")
                .adzoneId("333")
                .externalId("沉碧7秒")
                .orderTime("2026-07-07 09:39:45")
                .platformStatus(1)
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(1, result);
        verify(orderMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                Long.valueOf(1001L).equals(order.getMemberId())
                        && "我是喵团员".equals(order.getMemberNickname())
                        && "3311726376544025983".equals(order.getPlatformOrderId())));
        verify(transferRecordMapper).updatePlatformOrderId(10L, "3311726376544025983");
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 多条转链候选不做兜底归因避免误绑")
    void saveOrUpdateOrder_doesNotAttributeWhenTransferCandidatesAreAmbiguous() {
        when(orderMapper.selectByPlatformOrderId("TB-FALLBACK-2")).thenReturn(null);
        when(transferRecordMapper.selectAttributionCandidates(eq("taobao"), eq("ITEM-1"),
                eq("mm_111_222_333"), any(), any())).thenReturn(List.of(
                CpsTransferRecordDO.builder().id(10L).memberId(1001L).build(),
                CpsTransferRecordDO.builder().id(11L).memberId(1002L).build()));

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-FALLBACK-2")
                .platformCode("taobao")
                .itemId("ITEM-1")
                .adzoneId("mm_111_222_333")
                .orderTime("2026-07-06 20:59:00")
                .platformStatus(1)
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(1, result);
        verify(orderMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                order.getMemberId() == null));
        verify(transferRecordMapper, never()).updatePlatformOrderId(any(), any());
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 淘宝会员运营ID specialId 应优先绑定本地会员")
    void saveOrUpdateOrder_attributesByTaobaoSpecialId() {
        when(orderMapper.selectByPlatformOrderId("TB-SPECIAL-1")).thenReturn(null);
        when(adzoneMapper.selectActiveMemberAdzoneBySpecialId("taobao", "SPECIAL-1001"))
                .thenReturn(CpsAdzoneDO.builder()
                        .platformCode("taobao")
                        .adzoneId("mm_member_pid")
                        .relationType("member")
                        .relationId(1001L)
                        .externalSpecialId("SPECIAL-1001")
                        .status(1)
                        .build());
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(1001L);
        member.setNickname("我是喵团员");
        when(memberUserApi.getUser(1001L)).thenReturn(member);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-SPECIAL-1")
                .platformCode("taobao")
                .itemId("ITEM-1")
                .adzoneId("mm_member_pid")
                .specialId("SPECIAL-1001")
                .externalId("not-a-member-id")
                .orderScene(3)
                .orderTime("2026-07-07 14:46:25")
                .platformStatus(1)
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(1, result);
        verify(orderMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                Long.valueOf(1001L).equals(order.getMemberId())
                        && "我是喵团员".equals(order.getMemberNickname())
                        && "SPECIAL-1001".equals(order.getSpecialId())
                        && Integer.valueOf(3).equals(order.getOrderScene())
                        && "specialId".equals(order.getAttributionSource())));
        verify(adzoneMapper, never()).selectActiveMemberAdzone(eq("taobao"), any(String.class));
        verify(transferRecordMapper, never()).selectAttributionCandidates(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 平台返回用户专属推广位时应确定性绑定会员")
    void saveOrUpdateOrder_attributesByMemberAdzone() {
        when(orderMapper.selectByPlatformOrderId("TB-ADZONE-1")).thenReturn(null);
        when(adzoneMapper.selectActiveMemberAdzone("taobao", "mm_111_222_333"))
                .thenReturn(CpsAdzoneDO.builder()
                        .platformCode("taobao")
                        .adzoneId("mm_111_222_333")
                        .relationType("member")
                        .relationId(1001L)
                        .status(1)
                        .build());
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(1001L);
        member.setNickname("我是喵团员");
        when(memberUserApi.getUser(1001L)).thenReturn(member);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-ADZONE-1")
                .platformCode("taobao")
                .itemId("ITEM-1")
                .adzoneId("mm_111_222_333")
                .externalId("not-a-member-id")
                .orderTime("2026-07-07 14:46:25")
                .platformStatus(1)
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(1, result);
        verify(orderMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                Long.valueOf(1001L).equals(order.getMemberId())
                        && "我是喵团员".equals(order.getMemberNickname())
                        && "mm_111_222_333".equals(order.getAdzoneId())));
        verify(transferRecordMapper, never()).selectAttributionCandidates(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("manualSync - 状态同步应按更新时间查询平台订单")
    void manualSync_usesUpdateTimeQueryTypeForStatusSync() {
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.queryOrders(any(CpsOrderQueryRequest.class)))
                .thenReturn(List.of(CpsOrderDTO.builder()
                        .platformCode("taobao")
                        .platformOrderId("TB-STATUS-1")
                        .platformStatus(3)
                        .commissionAmount(new BigDecimal("8.00"))
                        .build()))
                .thenReturn(List.of())
                .thenReturn(List.of());
        when(orderMapper.selectByPlatformOrderId("TB-STATUS-1")).thenReturn(null);

        String result = orderService.manualSync("taobao", 2, 4);

        ArgumentCaptor<CpsOrderQueryRequest> captor = ArgumentCaptor.forClass(CpsOrderQueryRequest.class);
        verify(platformClient, times(3)).queryOrders(captor.capture());
        assertEquals(List.of(1, 2, 3), captor.getAllValues().stream().map(CpsOrderQueryRequest::getOrderScene).toList());
        captor.getAllValues().forEach(req ->
                assertEquals(4, req.getQueryType()));
        verify(platformClient, atLeastOnce()).queryOrders(argThat(req ->
                Integer.valueOf(4).equals(req.getQueryType())
                        && Integer.valueOf(50).equals(req.getPageSize())
                        && req.getStartTime() != null
                        && req.getEndTime() != null));
        verify(syncLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderSyncLogDO>argThat(log ->
                Integer.valueOf(4).equals(log.getQueryType())
                        && Integer.valueOf(1).equals(log.getSyncStatus())
                        && Integer.valueOf(1).equals(log.getTotalCount())));
        assertEquals("平台[taobao] 手动同步完成: 共1条，新增1，更新0，跳过0", result);
    }

    @Test
    @DisplayName("manualSync - 淘宝长时间窗口应拆成不超过3小时的小窗口")
    void manualSync_splitsTaobaoLongRangeIntoThreeHourWindows() {
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.queryOrders(any(CpsOrderQueryRequest.class)))
                .thenReturn(List.of())
                .thenReturn(List.of(CpsOrderDTO.builder()
                        .platformCode("taobao")
                        .platformOrderId("3311726376544025983")
                        .platformStatus(1)
                        .commissionAmount(new BigDecimal("6.68"))
                        .build()))
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of());
        when(orderMapper.selectByPlatformOrderId("3311726376544025983")).thenReturn(null);

        String result = orderService.manualSync("taobao", 24, 4);

        ArgumentCaptor<CpsOrderQueryRequest> captor = ArgumentCaptor.forClass(CpsOrderQueryRequest.class);
        verify(platformClient, times(24)).queryOrders(captor.capture());
        captor.getAllValues().forEach(req -> {
            LocalDateTime start = LocalDateTime.parse(req.getStartTime(), DTF);
            LocalDateTime end = LocalDateTime.parse(req.getEndTime(), DTF);
            assertTrue(!end.isBefore(start));
            assertTrue(Duration.between(start, end).compareTo(Duration.ofHours(3)) <= 0);
            assertEquals(4, req.getQueryType());
        });
        assertEquals(Set.of(1, 2, 3), captor.getAllValues().stream()
                .map(CpsOrderQueryRequest::getOrderScene)
                .collect(java.util.stream.Collectors.toSet()));
        verify(syncLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderSyncLogDO>argThat(log ->
                Integer.valueOf(1).equals(log.getTotalCount())
                        && Integer.valueOf(1).equals(log.getNewCount())));
        assertEquals("平台[taobao] 手动同步完成: 共1条，新增1，更新0，跳过0", result);
    }
}
