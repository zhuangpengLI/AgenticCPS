package com.qiji.cps.module.cps.job;

import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateDebtDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateDebtMapper;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetPolicyService;
import com.qiji.cps.module.member.api.user.MemberUserApi;
import com.qiji.cps.module.system.api.notify.NotifyMessageSendApi;
import com.qiji.cps.module.system.api.sms.SmsSendApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateDebtReminderJobTest {

    private CpsRebateDebtReminderJob job;
    @Mock private CpsRebateDebtMapper debtMapper;
    @Mock private NotifyMessageSendApi notifyMessageSendApi;
    @Mock private SmsSendApi smsSendApi;
    @Mock private MemberUserApi memberUserApi;
    @Mock private CpsRebateAssetPolicyService policyService;

    @BeforeEach
    void setUp() {
        job = new CpsRebateDebtReminderJob();
        ReflectionTestUtils.setField(job, "debtMapper", debtMapper);
        ReflectionTestUtils.setField(job, "notifyMessageSendApi", notifyMessageSendApi);
        ReflectionTestUtils.setField(job, "smsSendApi", smsSendApi);
        ReflectionTestUtils.setField(job, "memberUserApi", memberUserApi);
        ReflectionTestUtils.setField(job, "policyService", policyService);
        when(policyService.getPolicy()).thenReturn(CpsRebateAssetPolicyDO.builder()
                .largeDebtThresholdCent(50_000L).reminderIntervalDays(3)
                .normalReminderDays(10).largeReminderDays(60).smsIntervalDays(20).build());
    }

    @Test
    void notificationFailureSchedulesRetryAndDoesNotEscapeJob() {
        CpsRebateDebtDO debt = debt(1L, 3_000L, LocalDateTime.now().minusDays(2));
        when(debtMapper.selectDueReminderList(any(), org.mockito.ArgumentMatchers.eq(200)))
                .thenReturn(List.of(debt));
        doThrow(new IllegalStateException("template missing"))
                .when(notifyMessageSendApi).sendSingleMessageToMember(any());

        String result = job.execute("");

        assertTrue(result.contains("failed=1"));
        ArgumentCaptor<CpsRebateDebtDO> update = ArgumentCaptor.forClass(CpsRebateDebtDO.class);
        verify(debtMapper).updateById(update.capture());
        assertTrue(update.getValue().getNextReminderTime().isAfter(LocalDateTime.now()));
    }

    @Test
    void tenantPolicyControlsNormalReminderExpiry() {
        CpsRebateDebtDO debt = debt(2L, 3_000L, LocalDateTime.now().minusDays(11));
        when(debtMapper.selectDueReminderList(any(), org.mockito.ArgumentMatchers.eq(200)))
                .thenReturn(List.of(debt));

        job.execute("");

        ArgumentCaptor<CpsRebateDebtDO> update = ArgumentCaptor.forClass(CpsRebateDebtDO.class);
        verify(debtMapper).updateById(update.capture());
        assertEquals(null, update.getValue().getNextReminderTime());
    }

    private CpsRebateDebtDO debt(Long id, long amountCent, LocalDateTime createTime) {
        CpsRebateDebtDO debt = CpsRebateDebtDO.builder().id(id).memberId(1001L)
                .outstandingDebtCent(amountCent).nextReminderTime(LocalDateTime.now().minusMinutes(1)).build();
        debt.setCreateTime(createTime);
        return debt;
    }
}
