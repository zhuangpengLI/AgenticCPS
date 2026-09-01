package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderPageResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderAttributionLogDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderStatusEventDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncLogDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderAttributionLogMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderStatusEventMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncLogMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import com.qiji.cps.module.cps.enums.CpsRebateTypeEnum;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
import com.qiji.cps.module.cps.service.rebate.CpsRebateConfigService;
import com.qiji.cps.module.member.api.user.MemberUserApi;
import com.qiji.cps.module.member.api.user.dto.MemberUserRespDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.function.Supplier;

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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CpsOrderServiceImplTest {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @InjectMocks
    private CpsOrderServiceImpl orderService;

    @Mock
    private CpsOrderMapper orderMapper;
    @Mock
    private CpsOrderAttributionLogMapper attributionLogMapper;
    @Mock
    private CpsOrderStatusEventMapper statusEventMapper;
    @Mock
    private CpsRebateRecordMapper rebateRecordMapper;
    @Mock
    private CpsOrderSyncLogMapper syncLogMapper;
    @Mock
    private CpsPlatformClientFactory platformClientFactory;
    @Mock
    private CpsRebateSettleService rebateSettleService;
    @Mock
    private CpsRebateConfigService rebateConfigService;
    @Mock
    private CpsPlatformClient platformClient;
    @Mock
    private CpsApiVendorClient vendorClient;
    @Mock
    private CpsTransferRecordMapper transferRecordMapper;
    @Mock
    private MemberUserApi memberUserApi;
    @Mock
    private CpsAdzoneMapper adzoneMapper;

    @BeforeEach
    void allowSuccessfulVersionedOrderUpdates() {
        lenient().when(orderMapper.updateByIdAndStatusVersion(any(CpsOrderDO.class), any(Integer.class)))
                .thenReturn(1);
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 新订单应追加不可变状态事件与原始状态摘要")
    void saveOrUpdateOrder_appendsStatusEventForNewOrder() {
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-EVENT-NEW")).thenReturn(null);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformCode("taobao")
                .platformOrderId("TB-EVENT-NEW")
                .platformStatus(3)
                .refundTag(0)
                .rawPayload("{\"tk_status\":3,\"order_scene\":1}")
                .syncBatchNo("batch-20260714-001")
                .build();

        assertEquals(1, orderService.saveOrUpdateOrder(dto));

        verify(statusEventMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderStatusEventDO>argThat(event ->
                "taobao".equals(event.getPlatformCode())
                        && "TB-EVENT-NEW".equals(event.getPlatformOrderId())
                        && "3".equals(event.getRawStatus())
                        && CpsOrderStatusEnum.SETTLED.getStatus().equals(event.getMappedStatus())
                        && event.getPreviousStatus() == null
                        && Integer.valueOf(0).equals(event.getStatusVersion())
                        && !event.getDowngradeRejected()
                        && "batch-20260714-001".equals(event.getSourceBatchNo())
                        && event.getRawStatusSummary().contains("tk_status")));
        verify(orderMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                order.getRawPlatformStatusSummary() != null
                        && order.getRawPlatformStatusSummary().contains("platformStatus=3")));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 拒绝平台状态降级时必须追加拒绝事件")
    void saveOrUpdateOrder_appendsRejectedDowngradeStatusEvent() {
        CpsOrderDO existing = CpsOrderDO.builder()
                .id(32L)
                .platformCode("taobao")
                .platformOrderId("TB-DOWNGRADE-1")
                .orderStatus(CpsOrderStatusEnum.SETTLED.getStatus())
                .statusVersion(4)
                .build();
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-DOWNGRADE-1")).thenReturn(existing);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformCode("taobao")
                .platformOrderId("TB-DOWNGRADE-1")
                .platformStatus(1)
                .syncBatchNo("batch-20260714-002")
                .build();

        assertEquals(0, orderService.saveOrUpdateOrder(dto));

        verify(orderMapper, never()).updateByIdAndStatusVersion(any(CpsOrderDO.class), any(Integer.class));
        verify(statusEventMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderStatusEventDO>argThat(event ->
                Long.valueOf(32L).equals(event.getOrderId())
                        && "1".equals(event.getRawStatus())
                        && CpsOrderStatusEnum.PAID.getStatus().equals(event.getMappedStatus())
                        && CpsOrderStatusEnum.SETTLED.getStatus().equals(event.getCurrentStatus())
                        && CpsOrderStatusEnum.SETTLED.getStatus().equals(event.getPreviousStatus())
                        && Integer.valueOf(4).equals(event.getStatusVersion())
                        && event.getDowngradeRejected()
                        && event.getRejectReason().contains("拒绝降级")));
    }

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
                .statusVersion(7)
                .commissionAmount(new BigDecimal("12.00"))
                .rebateTime(LocalDateTime.now().minusDays(1))
                .build();
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-1")).thenReturn(existing);
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
        verify(orderMapper).updateByIdAndStatusVersion(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                order.getId().equals(1L)
                        && CpsOrderStatusEnum.REFUNDED.getStatus().equals(order.getOrderStatus())
                        && order.getRefundTime() != null), eq(7));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 版本冲突时拒绝让并发结算覆盖退款状态")
    void saveOrUpdateOrder_throwsWhenStatusVersionChangedConcurrently() {
        CpsOrderDO existing = CpsOrderDO.builder()
                .id(9L)
                .platformCode("taobao")
                .platformOrderId("TB-RACE-1")
                .orderStatus(CpsOrderStatusEnum.SETTLED.getStatus())
                .statusVersion(3)
                .build();
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-RACE-1")).thenReturn(existing);
        when(rebateRecordMapper.selectByOrderIdAndType(9L, CpsRebateTypeEnum.REBATE.getType()))
                .thenReturn(com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO.builder()
                        .id(91L).orderId(9L).rebateType(CpsRebateTypeEnum.REBATE.getType()).build());
        when(rebateSettleService.reverseRebate(9L)).thenReturn(true);
        when(orderMapper.updateByIdAndStatusVersion(any(CpsOrderDO.class), eq(3))).thenReturn(0);

        CpsOrderDTO refund = CpsOrderDTO.builder()
                .platformCode("taobao")
                .platformOrderId("TB-RACE-1")
                .refundTag(1)
                .build();

        assertThrows(IllegalStateException.class, () -> orderService.saveOrUpdateOrder(refund));

        verify(rebateSettleService).reverseRebate(9L);
        verify(orderMapper).updateByIdAndStatusVersion(argThat(order ->
                CpsOrderStatusEnum.REFUNDED.getStatus().equals(order.getOrderStatus())), eq(3));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - V2冻结返利尚未到账时收到退款也必须触发冲正")
    void saveOrUpdateOrder_reverseRebateWhenRefundedAfterV2Freeze() {
        CpsOrderDO existing = CpsOrderDO.builder()
                .id(4L)
                .platformOrderId("TB-4")
                .platformCode("taobao")
                .orderStatus(CpsOrderStatusEnum.SETTLED.getStatus())
                .commissionAmount(new BigDecimal("12.00"))
                .build();
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-4")).thenReturn(existing);
        when(rebateRecordMapper.selectByOrderIdAndType(4L, CpsRebateTypeEnum.REBATE.getType()))
                .thenReturn(com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO.builder()
                        .id(41L).orderId(4L).rebateType(CpsRebateTypeEnum.REBATE.getType())
                        .rebateStatus("pending").build());
        when(rebateSettleService.reverseRebate(4L)).thenReturn(true);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-4")
                .platformCode("taobao")
                .commissionAmount(new BigDecimal("12.00"))
                .refundTag(1)
                .build();

        assertEquals(2, orderService.saveOrUpdateOrder(dto));

        verify(rebateSettleService).reverseRebate(4L);
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
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-2")).thenReturn(existing);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-2")
                .platformCode("taobao")
                .platformStatus(3)
                .commissionAmount(new BigDecimal("15.00"))
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(2, result);
        verify(rebateSettleService, never()).reverseRebate(2L);
        verify(orderMapper).updateByIdAndStatusVersion(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                order.getId().equals(2L)
                        && CpsOrderStatusEnum.REBATE_RECEIVED.getStatus().equals(order.getOrderStatus())
                        && new BigDecimal("15.00").compareTo(order.getCommissionAmount()) == 0), eq(0));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 数字 externalId 未经可信绑定不得作为会员ID归因")
    void saveOrUpdateOrder_rejectsRawNumericExternalIdAttribution() {
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
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-3")).thenReturn(existing);

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
        verify(orderMapper).updateByIdAndStatusVersion(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                order.getId().equals(3L)
                        && new BigDecimal("399.00").compareTo(order.getFinalPrice()) == 0
                        && new BigDecimal("17.96").compareTo(order.getCommissionAmount()) == 0
                        && BigDecimal.ZERO.compareTo(order.getEstimateRebate()) == 0
                        && order.getMemberId() == null
                        && order.getAttributionSource() == null
                        && "1002".equals(order.getExternalInfo())
                        && "ITEM-3".equals(order.getItemId())), eq(0));
        verify(attributionLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderAttributionLogDO>argThat(log -> "AUTO".equals(log.getAction())
                && "UNATTRIBUTED".equals(log.getResult())
                && log.getAttributedMemberId() == null));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 平台未返回券额时应由原价和券后价推导优惠金额")
    void saveOrUpdateOrder_derivesCouponAmountFromOriginalAndFinalPrice() {
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-COUPON-1")).thenReturn(null);

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
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-FALLBACK-1")).thenReturn(null);
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
        when(orderMapper.selectByPlatformOrderId("taobao", "3311726376544025983")).thenReturn(null);
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
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-FALLBACK-2")).thenReturn(null);
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
        verify(attributionLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderAttributionLogDO>argThat(log -> "CONFLICT".equals(log.getResult())
                && "AUTO".equals(log.getAction())));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 淘宝会员运营ID specialId 应优先绑定本地会员")
    void saveOrUpdateOrder_attributesByTaobaoSpecialId() {
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-SPECIAL-1")).thenReturn(null);
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
        verify(attributionLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderAttributionLogDO>argThat(log -> "BOUND".equals(log.getResult())
                && "specialId".equals(log.getBindingType())
                && Long.valueOf(1001L).equals(log.getAttributedMemberId())));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 平台返回用户专属推广位时应确定性绑定会员")
    void saveOrUpdateOrder_attributesByMemberAdzone() {
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-ADZONE-1")).thenReturn(null);
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
    @DisplayName("saveOrUpdateOrder - 闪购 channelCode 仅在唯一有效 sid 映射时绑定会员")
    void saveOrUpdateOrder_attributesElemeByUniqueTrustedSid() {
        when(orderMapper.selectByPlatformOrderId("eleme", "ELM-SID-1")).thenReturn(null);
        when(transferRecordMapper.selectValidAttributionTokenCandidates(
                eq("haodanku"), eq("eleme"), eq("SID"), eq("Abc_123456789"), any()))
                .thenReturn(List.of(CpsTransferRecordDO.builder()
                        .id(20L)
                        .memberId(1002L)
                        .vendorCode("haodanku")
                        .platformCode("eleme")
                        .attributionType("SID")
                        .attributionToken("Abc_123456789")
                        .status(1)
                        .expireTime(LocalDateTime.now().plusDays(1))
                        .build()));
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(1002L);
        member.setNickname("闪购会员");
        when(memberUserApi.getUser(1002L)).thenReturn(member);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .vendorCode("haodanku")
                .platformCode("eleme")
                .platformOrderId("ELM-SID-1")
                .externalId("Abc_123456789")
                .platformStatus(1)
                .build();

        assertEquals(1, orderService.saveOrUpdateOrder(dto));

        verify(orderMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order -> Long.valueOf(1002L).equals(order.getMemberId())
                && "sid".equals(order.getAttributionSource())));
        verify(transferRecordMapper).updatePlatformOrderId(20L, "ELM-SID-1");
    }

    @Test
    @DisplayName("saveOrUpdateOrder - Jutuike order binds member by trusted SID")
    void saveOrUpdateOrder_attributesJutuikeOrderByTrustedSid() {
        when(orderMapper.selectByPlatformOrderId("meituan", "JTK-SID-1")).thenReturn(null);
        when(transferRecordMapper.selectValidAttributionTokenCandidates(
                eq("jutuike"), eq("meituan"), eq("SID"), eq("Jtk_123456789AB"), any()))
                .thenReturn(List.of(CpsTransferRecordDO.builder()
                        .id(30L).memberId(2002L).vendorCode("jutuike").platformCode("meituan")
                        .attributionType("SID").attributionToken("Jtk_123456789AB")
                        .status(1).expireTime(LocalDateTime.now().plusDays(1)).build()));
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(2002L);
        member.setNickname("Jutuike Member");
        when(memberUserApi.getUser(2002L)).thenReturn(member);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .vendorCode("jutuike").platformCode("meituan")
                .platformOrderId("JTK-SID-1").externalId("Jtk_123456789AB")
                .platformStatus(1).build();

        assertEquals(1, orderService.saveOrUpdateOrder(dto));
        verify(orderMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                Long.valueOf(2002L).equals(order.getMemberId()) && "sid".equals(order.getAttributionSource())));
        verify(transferRecordMapper).updatePlatformOrderId(30L, "JTK-SID-1");
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 闪购 sid 存在多个候选时拒绝猜测归因")
    void saveOrUpdateOrder_rejectsAmbiguousElemeSid() {
        when(orderMapper.selectByPlatformOrderId("eleme", "ELM-SID-2")).thenReturn(null);
        when(transferRecordMapper.selectValidAttributionTokenCandidates(
                eq("haodanku"), eq("eleme"), eq("SID"), eq("DuplicateSid12"), any()))
                .thenReturn(List.of(
                        CpsTransferRecordDO.builder().id(21L).memberId(1002L).build(),
                        CpsTransferRecordDO.builder().id(22L).memberId(1003L).build()));

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .vendorCode("haodanku")
                .platformCode("eleme")
                .platformOrderId("ELM-SID-2")
                .externalId("DuplicateSid12")
                .platformStatus(1)
                .build();

        assertEquals(1, orderService.saveOrUpdateOrder(dto));

        verify(orderMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order -> order.getMemberId() == null));
        verify(transferRecordMapper, never()).updatePlatformOrderId(any(), any());
        verify(attributionLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderAttributionLogDO>argThat(log -> "CONFLICT".equals(log.getResult())
                && log.getAttributedMemberId() == null));
    }

    @Test
    @DisplayName("bindSpecialIdToMember - 手动绑定 specialId 应写入会员专属推广位并更新订单归因")
    void bindSpecialIdToMember_createsMemberAdzoneAndUpdatesOrderAttribution() {
        when(orderMapper.selectById(7L)).thenReturn(CpsOrderDO.builder()
                .id(7L)
                .platformCode("taobao")
                .platformOrderId("TB-BIND-1")
                .adzoneId("mm_111_222_333")
                .specialId("SPECIAL-1001")
                .build());
        when(adzoneMapper.selectBySpecialId("taobao", "SPECIAL-1001")).thenReturn(null);
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(1001L);
        member.setNickname("绑定会员");
        when(memberUserApi.getUser(1001L)).thenReturn(member);

        orderService.bindSpecialIdToMember(7L, 1001L);

        verify(adzoneMapper).insert(org.mockito.ArgumentMatchers.<CpsAdzoneDO>argThat(adzone ->
                "taobao".equals(adzone.getPlatformCode())
                        && "mm_111_222_333".equals(adzone.getAdzoneId())
                        && "member".equals(adzone.getAdzoneType())
                        && "member".equals(adzone.getRelationType())
                        && Long.valueOf(1001L).equals(adzone.getRelationId())
                        && "SPECIAL-1001".equals(adzone.getExternalSpecialId())
                        && Integer.valueOf(1).equals(adzone.getStatus())));
        verify(orderMapper).updateById(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                Long.valueOf(7L).equals(order.getId())
                        && Long.valueOf(1001L).equals(order.getMemberId())
                        && "绑定会员".equals(order.getMemberNickname())
                        && "specialId".equals(order.getAttributionSource())));
        verify(attributionLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderAttributionLogDO>argThat(log -> "MANUAL".equals(log.getAction())
                && "BOUND".equals(log.getResult())
                && Long.valueOf(1001L).equals(log.getCandidateMemberId())));
    }

    @Test
    @DisplayName("bindSpecialIdToMember - 已产生返利资产的订单不得直接改绑并记录拒绝日志")
    void bindSpecialIdToMember_rejectsRebindAfterRebateAssetActivity() {
        when(orderMapper.selectById(8L)).thenReturn(CpsOrderDO.builder()
                .id(8L)
                .platformCode("taobao")
                .platformOrderId("TB-BIND-2")
                .memberId(1001L)
                .adzoneId("mm_111_222_333")
                .specialId("SPECIAL-1001")
                .rebateTime(LocalDateTime.now())
                .build());

        assertThrows(ServiceException.class, () -> orderService.bindSpecialIdToMember(8L, 1002L));

        verify(adzoneMapper, never()).insert(any(CpsAdzoneDO.class));
        verify(orderMapper, never()).updateById(any(CpsOrderDO.class));
        verify(attributionLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderAttributionLogDO>argThat(log -> "REBIND".equals(log.getAction())
                && "REJECTED".equals(log.getResult())
                && Long.valueOf(1001L).equals(log.getAttributedMemberId())
                && Long.valueOf(1002L).equals(log.getCandidateMemberId())));
    }

    @Test
    @DisplayName("bindSpecialIdToMember - 会员不存在时不应写入绑定关系")
    void bindSpecialIdToMember_rejectsMissingMemberBeforeWritingAttribution() {
        when(orderMapper.selectById(7L)).thenReturn(CpsOrderDO.builder()
                .id(7L)
                .platformCode("taobao")
                .platformOrderId("TB-BIND-1")
                .adzoneId("mm_111_222_333")
                .specialId("SPECIAL-1001")
                .build());
        when(memberUserApi.getUser(1001L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> orderService.bindSpecialIdToMember(7L, 1001L));

        verify(adzoneMapper, never()).selectBySpecialId(any(), any());
        verify(adzoneMapper, never()).insert(org.mockito.ArgumentMatchers.<CpsAdzoneDO>any());
        verify(orderMapper, never()).updateById(org.mockito.ArgumentMatchers.<CpsOrderDO>any());
    }

    @Test
    @DisplayName("bindSpecialIdToMember - 相同幂等键重复提交不应重复改写归因")
    void bindSpecialIdToMember_skipsDuplicateIdempotencyKey() {
        when(attributionLogMapper.selectByIdempotencyKey("manual-bind-dup"))
                .thenReturn(CpsOrderAttributionLogDO.builder()
                        .id(99L)
                        .idempotencyKey("manual-bind-dup")
                        .result("BOUND")
                        .build());

        orderService.bindSpecialIdToMember(new CpsOrderManualBindCommand(
                7L, 1001L, 9001L, "manual-bind-dup", "重复提交"));

        verify(orderMapper, never()).selectById(any());
        verify(adzoneMapper, never()).insert(any(CpsAdzoneDO.class));
        verify(adzoneMapper, never()).updateById(any(CpsAdzoneDO.class));
        verify(orderMapper, never()).updateById(any(CpsOrderDO.class));
        verify(attributionLogMapper, never()).insert(any(CpsOrderAttributionLogDO.class));
    }

    @Test
    @DisplayName("bindSpecialIdToMember - specialId 已绑定其他会员时阻断并写入复核日志")
    void bindSpecialIdToMember_rejectsConflictingSpecialIdOwnerForReview() {
        when(orderMapper.selectById(9L)).thenReturn(CpsOrderDO.builder()
                .id(9L)
                .platformCode("taobao")
                .platformOrderId("TB-BIND-CONFLICT")
                .adzoneId("mm_111_222_333")
                .specialId("SPECIAL-CONFLICT")
                .build());
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(1001L);
        member.setNickname("目标会员");
        when(memberUserApi.getUser(1001L)).thenReturn(member);
        when(adzoneMapper.selectBySpecialId("taobao", "SPECIAL-CONFLICT")).thenReturn(CpsAdzoneDO.builder()
                .id(11L)
                .platformCode("taobao")
                .relationType("member")
                .relationId(2002L)
                .externalSpecialId("SPECIAL-CONFLICT")
                .build());

        assertThrows(ServiceException.class, () -> orderService.bindSpecialIdToMember(new CpsOrderManualBindCommand(
                9L, 1001L, 9001L, "manual-bind-conflict", "人工发现疑似漏归因")));

        verify(adzoneMapper, never()).insert(any(CpsAdzoneDO.class));
        verify(adzoneMapper, never()).updateById(any(CpsAdzoneDO.class));
        verify(orderMapper, never()).updateById(any(CpsOrderDO.class));
        verify(attributionLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderAttributionLogDO>argThat(log ->
                "CONFLICT".equals(log.getResult())
                        && "PENDING_REVIEW".equals(log.getReviewStatus())
                        && Long.valueOf(1001L).equals(log.getCandidateMemberId())
                        && Long.valueOf(2002L).equals(log.getAttributedMemberId())
                        && Long.valueOf(9001L).equals(log.getReviewOperatorId())
                        && "manual-bind-conflict".equals(log.getIdempotencyKey())));
    }

    @Test
    @DisplayName("bindSpecialIdToMember - 正常人工绑定应写入幂等键、复核结论和操作人")
    void bindSpecialIdToMember_recordsApprovedManualReviewAudit() {
        when(orderMapper.selectById(10L)).thenReturn(CpsOrderDO.builder()
                .id(10L)
                .platformCode("taobao")
                .platformOrderId("TB-BIND-AUDIT")
                .adzoneId("mm_111_222_333")
                .specialId("SPECIAL-APPROVED")
                .build());
        when(adzoneMapper.selectBySpecialId("taobao", "SPECIAL-APPROVED")).thenReturn(null);
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(1001L);
        member.setNickname("绑定会员");
        when(memberUserApi.getUser(1001L)).thenReturn(member);

        orderService.bindSpecialIdToMember(new CpsOrderManualBindCommand(
                10L, 1001L, 9001L, "manual-bind-approved", "平台截图与会员申诉单一致"));

        ArgumentCaptor<CpsOrderAttributionLogDO> captor = ArgumentCaptor.forClass(CpsOrderAttributionLogDO.class);
        verify(attributionLogMapper).insert(captor.capture());
        CpsOrderAttributionLogDO log = captor.getValue();
        assertEquals("BOUND", log.getResult());
        assertEquals("APPROVED", log.getReviewStatus());
        assertEquals("manual-bind-approved", log.getIdempotencyKey());
        assertEquals("平台截图与会员申诉单一致", log.getReviewAuditNote());
        assertEquals(Long.valueOf(9001L), log.getReviewOperatorId());
        assertEquals("9001", log.getOperatorId());
    }

    @Test
    @DisplayName("bindSpecialIdToMember - 绑定成功后应立即按会员规则重算预计返利")
    void bindSpecialIdToMember_recalculatesEstimateRebateImmediately() {
        when(orderMapper.selectById(11L)).thenReturn(CpsOrderDO.builder()
                .id(11L)
                .platformCode("taobao")
                .platformOrderId("TB-BIND-REBATE")
                .adzoneId("116291900443")
                .specialId("3362084501")
                .commissionAmount(new BigDecimal("0.21"))
                .estimateRebate(BigDecimal.ZERO)
                .build());
        when(adzoneMapper.selectBySpecialId("taobao", "3362084501")).thenReturn(null);
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(1001L);
        member.setNickname("绑定会员");
        member.setLevelId(2L);
        when(memberUserApi.getUser(1001L)).thenReturn(member);
        when(rebateConfigService.matchRebateConfig(1001L, 2L, "taobao"))
                .thenReturn(CpsRebateConfigDO.builder()
                        .rebateRate(new BigDecimal("90.0000"))
                        .minRebateAmount(BigDecimal.ZERO)
                        .maxRebateAmount(BigDecimal.ZERO)
                        .build());

        orderService.bindSpecialIdToMember(new CpsOrderManualBindCommand(
                11L, 1001L, 9001L, "manual-bind-rebate", "补录会员归因"));

        verify(orderMapper).updateById(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                Long.valueOf(1001L).equals(order.getMemberId())
                        && "specialId".equals(order.getAttributionSource())
                        && new BigDecimal("0.19").compareTo(order.getEstimateRebate()) == 0));
    }

    @Test
    @DisplayName("manualSync - 状态同步应按更新时间查询平台订单")
    void manualSync_usesUpdateTimeQueryTypeForStatusSync() {
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.queryOrderPage(any(CpsOrderQueryRequest.class)))
                .thenReturn(CpsOrderPageResult.page(List.of(CpsOrderDTO.builder()
                        .platformCode("taobao")
                        .platformOrderId("TB-STATUS-1")
                        .platformStatus(3)
                        .commissionAmount(new BigDecimal("8.00"))
                        .build()), 2, false))
                .thenReturn(CpsOrderPageResult.page(List.of(), 2, false))
                .thenReturn(CpsOrderPageResult.page(List.of(), 2, false));
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-STATUS-1")).thenReturn(null);

        String result = orderService.manualSync("taobao", 2, 4);

        ArgumentCaptor<CpsOrderQueryRequest> captor = ArgumentCaptor.forClass(CpsOrderQueryRequest.class);
        verify(platformClient, times(3)).queryOrderPage(captor.capture());
        assertEquals(List.of(1, 2, 3), captor.getAllValues().stream().map(CpsOrderQueryRequest::getOrderScene).toList());
        captor.getAllValues().forEach(req ->
                assertEquals(4, req.getQueryType()));
        verify(platformClient, atLeastOnce()).queryOrderPage(argThat(req ->
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
    @DisplayName("manualSync - selected vendor routes order status")
    void manualSync_routesSelectedVendorAndOrderStatus() {
        when(platformClientFactory.getVendorClient("dataoke", "taobao")).thenReturn(vendorClient);
        when(platformClientFactory.getVendorConfig("dataoke", "taobao"))
                .thenReturn(CpsVendorConfig.builder().vendorCode("dataoke").platformCode("taobao").build());
        when(platformClientFactory.withVendorCode(eq("dataoke"), any())).thenAnswer(invocation ->
                ((Supplier<?>) invocation.getArgument(1)).get());
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.queryOrderPage(any(CpsOrderQueryRequest.class)))
                .thenReturn(CpsOrderPageResult.page(List.of(), 2, false));

        LocalDateTime startTime = LocalDateTime.of(2026, 8, 1, 8, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 8, 1, 10, 0, 0);
        orderService.manualSync("taobao", "dataoke", 2, 4, 3, startTime, endTime);

        verify(platformClientFactory).withVendorCode(eq("dataoke"), any());
        verify(platformClient, times(3)).queryOrderPage(argThat(request ->
                Integer.valueOf(4).equals(request.getQueryType())
                        && Integer.valueOf(3).equals(request.getOrderStatus())));
    }

    @Test
    @DisplayName("manualSync - explicit time range uses the requested window")
    void manualSync_usesExplicitTimeRange() {
        LocalDateTime startTime = LocalDateTime.of(2026, 8, 1, 8, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 8, 1, 10, 0, 0);
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.queryOrderPage(any(CpsOrderQueryRequest.class)))
                .thenReturn(CpsOrderPageResult.page(List.of(), 2, false));

        orderService.manualSync("taobao", 2, 4, startTime, endTime);

        verify(platformClient, times(3)).queryOrderPage(argThat(request ->
                "2026-08-01 08:00:00".equals(request.getStartTime())
                        && "2026-08-01 10:00:00".equals(request.getEndTime())
                        && Integer.valueOf(4).equals(request.getQueryType())));
        verify(syncLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderSyncLogDO>argThat(log ->
                startTime.equals(log.getQueryStartTime()) && endTime.equals(log.getQueryEndTime())));
    }

    @Test
    @DisplayName("manualSync - 淘宝长时间窗口应拆成不超过3小时的小窗口")
    void manualSync_splitsTaobaoLongRangeIntoThreeHourWindows() {
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        java.util.concurrent.atomic.AtomicInteger pageCall = new java.util.concurrent.atomic.AtomicInteger();
        when(platformClient.queryOrderPage(any(CpsOrderQueryRequest.class))).thenAnswer(invocation -> {
            if (pageCall.getAndIncrement() == 1) {
                return CpsOrderPageResult.page(List.of(CpsOrderDTO.builder()
                        .platformCode("taobao")
                        .platformOrderId("3311726376544025983")
                        .platformStatus(1)
                        .commissionAmount(new BigDecimal("6.68"))
                        .build()), 2, false);
            }
            return CpsOrderPageResult.page(List.of(), 2, false);
        });
        when(orderMapper.selectByPlatformOrderId("taobao", "3311726376544025983")).thenReturn(null);

        String result = orderService.manualSync("taobao", 24, 4);

        ArgumentCaptor<CpsOrderQueryRequest> captor = ArgumentCaptor.forClass(CpsOrderQueryRequest.class);
        verify(platformClient, times(24)).queryOrderPage(captor.capture());
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
