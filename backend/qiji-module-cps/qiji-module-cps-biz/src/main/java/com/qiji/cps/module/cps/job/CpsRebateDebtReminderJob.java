package com.qiji.cps.module.cps.job;

import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateDebtDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateDebtMapper;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetPolicyService;
import com.qiji.cps.module.member.api.user.MemberUserApi;
import com.qiji.cps.module.member.api.user.dto.MemberUserRespDTO;
import com.qiji.cps.module.system.api.notify.NotifyMessageSendApi;
import com.qiji.cps.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import com.qiji.cps.module.system.api.sms.SmsSendApi;
import com.qiji.cps.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会员返利欠款提醒。通知失败只进入短周期重试，不影响任何资金事务。
 */
@Slf4j
@Component
public class CpsRebateDebtReminderJob implements JobHandler {

    private static final String NOTIFY_TEMPLATE = "cps-rebate-debt-reminder";
    private static final String SMS_TEMPLATE = "cps-rebate-large-debt-reminder";

    @Resource private CpsRebateDebtMapper debtMapper;
    @Resource private NotifyMessageSendApi notifyMessageSendApi;
    @Resource private SmsSendApi smsSendApi;
    @Resource private MemberUserApi memberUserApi;
    @Resource private CpsRebateAssetPolicyService policyService;

    @Override
    @TenantJob
    public String execute(String param) {
        LocalDateTime now = LocalDateTime.now();
        CpsRebateAssetPolicyDO policy = policyService.getPolicy();
        int success = 0;
        int failed = 0;
        for (CpsRebateDebtDO debt : debtMapper.selectDueReminderList(now, 200)) {
            if (reminderExpired(debt, now, policy)) {
                updateSchedule(debt, null, null, null);
                continue;
            }
            try {
                Map<String, Object> params = Map.of(
                        "debtId", debt.getId(),
                        "amount", BigDecimal.valueOf(debt.getOutstandingDebtCent(), 2).toPlainString(),
                        "platformOrderId", debt.getPlatformOrderId() == null ? "-" : debt.getPlatformOrderId());
                NotifySendSingleToUserReqDTO notify = new NotifySendSingleToUserReqDTO();
                notify.setUserId(debt.getMemberId());
                notify.setTemplateCode(NOTIFY_TEMPLATE);
                notify.setTemplateParams(params);
                notifyMessageSendApi.sendSingleMessageToMember(notify);

                LocalDateTime smsTime = debt.getLastSmsTime();
                if (debt.getOutstandingDebtCent() >= policy.getLargeDebtThresholdCent()
                        && smsDue(debt, now, policy)) {
                    MemberUserRespDTO member = memberUserApi.getUser(debt.getMemberId());
                    if (member != null && member.getMobile() != null && !member.getMobile().isBlank()) {
                        SmsSendSingleToUserReqDTO sms = new SmsSendSingleToUserReqDTO();
                        sms.setUserId(debt.getMemberId());
                        sms.setMobile(member.getMobile());
                        sms.setTemplateCode(SMS_TEMPLATE);
                        sms.setTemplateParams(params);
                        smsSendApi.sendSingleSmsToMember(sms);
                        smsTime = now;
                    }
                }
                updateSchedule(debt, now, smsTime, now.plusDays(policy.getReminderIntervalDays()));
                success++;
            } catch (Exception ex) {
                // 模板缺失、通道异常均不得影响资金；次日重试并保留失败日志。
                log.warn("[execute][欠款提醒失败 debtId={}, memberId={}]", debt.getId(), debt.getMemberId(), ex);
                updateSchedule(debt, debt.getLastReminderTime(), debt.getLastSmsTime(), now.plusDays(1));
                failed++;
            }
        }
        return "欠款提醒完成: success=" + success + ", failed=" + failed;
    }

    private boolean reminderExpired(CpsRebateDebtDO debt, LocalDateTime now,
                                    CpsRebateAssetPolicyDO policy) {
        if (debt.getCreateTime() == null) return false;
        int days = debt.getOutstandingDebtCent() >= policy.getLargeDebtThresholdCent()
                ? policy.getLargeReminderDays() : policy.getNormalReminderDays();
        return debt.getCreateTime().plusDays(days).isBefore(now);
    }

    private boolean smsDue(CpsRebateDebtDO debt, LocalDateTime now, CpsRebateAssetPolicyDO policy) {
        return debt.getLastSmsTime() == null
                || !debt.getLastSmsTime().plusDays(policy.getSmsIntervalDays()).isAfter(now);
    }

    private void updateSchedule(CpsRebateDebtDO debt, LocalDateTime reminderTime,
                                LocalDateTime smsTime, LocalDateTime nextTime) {
        debtMapper.updateById(CpsRebateDebtDO.builder().id(debt.getId())
                .lastReminderTime(reminderTime).lastSmsTime(smsTime).nextReminderTime(nextTime).build());
    }
}
