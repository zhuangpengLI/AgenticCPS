package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderAttributionLogDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderAttributionLogMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.member.api.user.MemberUserApi;
import com.qiji.cps.module.member.api.user.dto.MemberUserRespDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsOrderClaimServiceImplTest {

    @InjectMocks
    private CpsOrderClaimServiceImpl service;

    @Mock
    private CpsOrderMapper orderMapper;
    @Mock
    private CpsOrderAttributionLogMapper attributionLogMapper;
    @Mock
    private CpsTransferRecordMapper transferRecordMapper;
    @Mock
    private CpsAdzoneMapper adzoneMapper;
    @Mock
    private CpsRebateRecordMapper rebateRecordMapper;
    @Mock
    private CpsFreezeRecordMapper freezeRecordMapper;
    @Mock
    private MemberUserApi memberUserApi;

    @Test
    void claimOrder_onlyOrderNumberCreatesPendingReviewWithoutBinding() {
        CpsOrderDO order = CpsOrderDO.builder().id(10L).platformCode("taobao")
                .platformOrderId("TB-CLAIM-1").build();
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-CLAIM-1")).thenReturn(order);
        when(orderMapper.selectForUpdateById(10L)).thenReturn(order);
        when(memberUserApi.getUser(1001L)).thenReturn(member(1001L, "申领会员"));

        CpsOrderClaimResult result = service.claim(new CpsOrderClaimCommand(
                1001L, "taobao", "TB-CLAIM-1", null, null, null, "claim-1"));

        assertEquals("PENDING_REVIEW", result.status());
        verify(orderMapper, never()).bindMemberIfUnattributed(any(), any(), any(), any());
        verify(attributionLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderAttributionLogDO>argThat(log ->
                "CLAIM".equals(log.getAction()) && "PENDING_REVIEW".equals(log.getReviewStatus())
                        && Long.valueOf(1001L).equals(log.getCandidateMemberId())));
    }

    @Test
    void claimOrder_uniqueTrustedElemeSidAutoApproves() {
        CpsOrderDO order = CpsOrderDO.builder().id(11L).platformCode("eleme")
                .platformOrderId("ELM-CLAIM-1").externalInfo("Sid_123456789").build();
        when(orderMapper.selectByPlatformOrderId("eleme", "ELM-CLAIM-1")).thenReturn(order);
        when(orderMapper.selectForUpdateById(11L)).thenReturn(order);
        when(transferRecordMapper.selectValidAttributionTokenCandidates(
                eq("haodanku"), eq("eleme"), eq("SID"), eq("Sid_123456789"), any(LocalDateTime.class)))
                .thenReturn(List.of(CpsTransferRecordDO.builder().id(31L).memberId(1001L).build()));
        when(memberUserApi.getUser(1001L)).thenReturn(member(1001L, "闪购会员"));
        when(orderMapper.bindMemberIfUnattributed(11L, 1001L, "闪购会员", "sid")).thenReturn(1);

        CpsOrderClaimResult result = service.claim(new CpsOrderClaimCommand(
                1001L, "eleme", "ELM-CLAIM-1", null, null, null, "claim-2"));

        assertEquals("APPROVED", result.status());
        verify(orderMapper).bindMemberIfUnattributed(11L, 1001L, "闪购会员", "sid");
        verify(attributionLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderAttributionLogDO>argThat(log ->
                "CLAIM_AUTO_APPROVED".equals(log.getAction()) && "APPROVED".equals(log.getReviewStatus())));
    }

    @Test
    void claimOrder_boundToAnotherMemberReturnsConflictWithoutIdentityLeak() {
        CpsOrderDO order = CpsOrderDO.builder().id(12L).platformCode("taobao")
                .platformOrderId("TB-CLAIM-2").memberId(2002L).build();
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-CLAIM-2")).thenReturn(order);
        when(orderMapper.selectForUpdateById(12L)).thenReturn(order);
        when(memberUserApi.getUser(1001L)).thenReturn(member(1001L, "申领会员"));

        CpsOrderClaimResult result = service.claim(new CpsOrderClaimCommand(
                1001L, "taobao", "TB-CLAIM-2", null, null, null, "claim-3"));

        assertEquals("CONFLICT", result.status());
        assertEquals(false, result.message().contains("2002"));
        verify(orderMapper, never()).bindMemberIfUnattributed(any(), any(), any(), any());
    }

    @Test
    void claimOrder_sameMemberIsIdempotentSuccess() {
        CpsOrderDO order = CpsOrderDO.builder().id(13L).platformCode("taobao")
                .platformOrderId("TB-CLAIM-3").memberId(1001L).build();
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-CLAIM-3")).thenReturn(order);
        when(orderMapper.selectForUpdateById(13L)).thenReturn(order);
        when(memberUserApi.getUser(1001L)).thenReturn(member(1001L, "申领会员"));

        CpsOrderClaimResult result = service.claim(new CpsOrderClaimCommand(
                1001L, "taobao", "TB-CLAIM-3", null, null, null, "claim-4"));

        assertEquals("APPROVED", result.status());
        verify(orderMapper, never()).bindMemberIfUnattributed(any(), any(), any(), any());
    }

    @Test
    void reviewPendingClaim_approvesWithConditionalBindAndAppendOnlyDecision() {
        CpsOrderAttributionLogDO claim = CpsOrderAttributionLogDO.builder().id(51L).orderId(14L)
                .platformCode("taobao").platformOrderId("TB-CLAIM-4").candidateMemberId(1001L)
                .action("CLAIM").reviewStatus("PENDING_REVIEW").build();
        CpsOrderDO order = CpsOrderDO.builder().id(14L).platformCode("taobao")
                .platformOrderId("TB-CLAIM-4").build();
        when(attributionLogMapper.selectForUpdateById(51L)).thenReturn(claim);
        when(orderMapper.selectForUpdateById(14L)).thenReturn(order);
        when(memberUserApi.getUser(1001L)).thenReturn(member(1001L, "审核会员"));
        when(orderMapper.bindMemberIfUnattributed(14L, 1001L, "审核会员", "manualClaim")).thenReturn(1);

        CpsOrderClaimResult result = service.review(new CpsOrderClaimReviewCommand(
                51L, true, 9001L, "订单截图与联盟后台一致"));

        assertEquals("APPROVED", result.status());
        verify(attributionLogMapper).updateClaimReview(51L, "APPROVED", "订单截图与联盟后台一致", 9001L);
        verify(attributionLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderAttributionLogDO>argThat(log ->
                "APPROVED".equals(log.getAction()) && "APPROVED".equals(log.getReviewStatus())));
    }

    @Test
    void reviewPendingClaim_rejectsBindingWhenOrderHasAssetActivity() {
        CpsOrderAttributionLogDO claim = CpsOrderAttributionLogDO.builder().id(52L).orderId(15L)
                .platformCode("taobao").platformOrderId("TB-CLAIM-5").candidateMemberId(1001L)
                .action("CLAIM").reviewStatus("PENDING_REVIEW").build();
        CpsOrderDO order = CpsOrderDO.builder().id(15L).platformCode("taobao")
                .platformOrderId("TB-CLAIM-5").realRebate(BigDecimal.ONE).build();
        when(attributionLogMapper.selectForUpdateById(52L)).thenReturn(claim);
        when(orderMapper.selectForUpdateById(15L)).thenReturn(order);

        CpsOrderClaimResult result = service.review(new CpsOrderClaimReviewCommand(
                52L, true, 9001L, "尝试审核"));

        assertEquals("ASSET_LOCKED", result.status());
        verify(orderMapper, never()).bindMemberIfUnattributed(any(), any(), any(), any());
        verify(attributionLogMapper).updateClaimReview(52L, "REJECTED", "订单已有返利资产活动，禁止直接绑定", 9001L);
    }

    @Test
    void claimOrder_rejectsIdempotencyKeyOwnedByAnotherMember() {
        when(attributionLogMapper.selectByIdempotencyKey("shared-key"))
                .thenReturn(CpsOrderAttributionLogDO.builder().candidateMemberId(2002L).build());

        assertThrows(com.qiji.cps.framework.common.exception.ServiceException.class,
                () -> service.claim(new CpsOrderClaimCommand(
                        1001L, "taobao", "TB-CLAIM-6", null, null, null, "shared-key")));

        verify(orderMapper, never()).selectByPlatformOrderId(any(), any());
    }

    private MemberUserRespDTO member(Long id, String nickname) {
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(id);
        member.setNickname(nickname);
        return member;
    }
}
