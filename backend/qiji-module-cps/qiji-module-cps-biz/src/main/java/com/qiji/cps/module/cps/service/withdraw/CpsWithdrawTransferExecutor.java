package com.qiji.cps.module.cps.service.withdraw;

import com.qiji.cps.framework.common.enums.UserTypeEnum;
import com.qiji.cps.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
import com.qiji.cps.module.pay.api.transfer.PayTransferApi;
import com.qiji.cps.module.pay.api.transfer.dto.PayTransferCreateReqDTO;
import com.qiji.cps.module.pay.api.transfer.dto.PayTransferCreateRespDTO;
import com.qiji.cps.module.pay.api.transfer.dto.PayTransferRespDTO;
import com.qiji.cps.module.pay.enums.transfer.PayTransferStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CpsWithdrawTransferExecutor {

    private static final String PAY_APP_KEY = "cps";

    @Resource private CpsWithdrawStepExecutor stepExecutor;
    @Resource private PayTransferApi payTransferApi;

    public void startTransfer(Long withdrawId) {
        CpsWithdrawDO withdraw = stepExecutor.claimTransfer(withdrawId);
        if (withdraw == null) return;
        try {
            PayTransferRespDTO transfer = null;
            if (withdraw.getPayTransferId() == null) {
                transfer = payTransferApi.getTransfer(PAY_APP_KEY, String.valueOf(withdraw.getId()));
                if (transfer != null) {
                    validateTransfer(withdraw, transfer, false);
                    withdraw = stepExecutor.attachPayTransfer(withdrawId, transfer.getId());
                } else {
                    PayTransferCreateRespDTO created = payTransferApi.createTransfer(buildRequest(withdraw));
                    if (created == null || created.getId() == null) throw new IllegalStateException("Pay 转账单创建结果为空");
                    withdraw = stepExecutor.attachPayTransfer(withdrawId, created.getId());
                }
            }
            if (transfer == null) transfer = payTransferApi.getTransfer(withdraw.getPayTransferId());
            if (transfer == null) throw new IllegalStateException("Pay 转账单不存在");
            validateTransfer(withdraw, transfer, true);
            if (PayTransferStatusEnum.isSuccess(transfer.getStatus())) {
                stepExecutor.completeSuccess(withdrawId, transfer.getId());
            } else if (PayTransferStatusEnum.isClosed(transfer.getStatus())) {
                stepExecutor.completeFailure(withdrawId, transfer.getId(),
                        transfer.getChannelErrorMsg() == null ? "closed" : transfer.getChannelErrorMsg());
            } else {
                stepExecutor.scheduleRetry(withdrawId, "Pay 转账仍在处理中");
            }
        } catch (Exception ex) {
            stepExecutor.scheduleRetry(withdrawId, ex.getMessage());
        }
    }

    private PayTransferCreateReqDTO buildRequest(CpsWithdrawDO withdraw) {
        PayTransferCreateReqDTO request = new PayTransferCreateReqDTO()
                .setAppKey(PAY_APP_KEY).setChannelCode(withdraw.getTransferChannelCode())
                .setUserIp("127.0.0.1").setUserId(withdraw.getMemberId())
                .setUserType(UserTypeEnum.MEMBER.getValue()).setMerchantTransferId(String.valueOf(withdraw.getId()))
                .setSubject("CPS返利提现").setPrice(Math.toIntExact(withdraw.getAmountCent()))
                .setUserAccount(withdraw.getWithdrawAccount()).setUserName(withdraw.getWithdrawAccountName());
        if ("wechat".equals(withdraw.getWithdrawType())) {
            request.setChannelExtras(PayTransferCreateReqDTO.buildWeiXinChannelExtra1000("CPS返利", "返利提现"));
        } else if ("alipay".equals(withdraw.getWithdrawType())) {
            request.setChannelExtras(PayTransferCreateReqDTO.buildAlipayChannelExtra("CPS返利提现"));
        }
        return request;
    }

    private void validateTransfer(CpsWithdrawDO withdraw, PayTransferRespDTO transfer, boolean requireLocalBinding) {
        if (transfer.getId() == null
                || requireLocalBinding && !Objects.equals(withdraw.getPayTransferId(), transfer.getId())
                || transfer.getPrice() == null || transfer.getPrice().longValue() != withdraw.getAmountCent()
                || !String.valueOf(withdraw.getId()).equals(transfer.getMerchantTransferId())
                || !withdraw.getTransferChannelCode().equals(transfer.getChannelCode())
                || !Objects.equals(withdraw.getMemberId(), transfer.getUserId())
                || !Objects.equals(UserTypeEnum.MEMBER.getValue(), transfer.getUserType())
                || !Objects.equals(withdraw.getWithdrawAccount(), transfer.getUserAccount())
                || !Objects.equals(withdraw.getWithdrawAccountName(), transfer.getUserName())) {
            throw new IllegalStateException("Pay 转账单与提现申请不匹配");
        }
    }
}
