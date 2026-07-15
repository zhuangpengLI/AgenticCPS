package com.qiji.cps.module.cps.service.exchange;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.dal.mysql.exchange.CpsRebateTokenExchangeOrderMapper;
import com.qiji.cps.module.cps.enums.CpsRebateExchangeStatusEnum;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeOrderRespDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeStatusUpdateReqDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeSubmitReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CpsRebateTokenExchangeCompensationServiceImpl
        implements CpsRebateTokenExchangeCompensationService {

    @Resource
    private CpsRebateTokenExchangeOrderMapper orderMapper;
    @Resource
    private CpsRebateTokenExchangeStepExecutor stepExecutor;
    @Resource
    private CpsAitokenExchangeClient aitokenExchangeClient;

    @Override
    public List<Long> getDueOrderIds(int limit) {
        return orderMapper.selectDueCompensationIds(LocalDateTime.now(), limit);
    }

    @Override
    public void compensate(Long orderId) {
        CpsRebateTokenExchangeOrderDO order = orderMapper.selectById(orderId);
        if (order == null) return;
        order = stepExecutor.claimCompensation(order);
        if (order == null) return;
        try {
            if (CpsRebateExchangeStatusEnum.PROCESSING.getStatus().equals(order.getStatus())) {
                compensateProcessing(order);
            } else if (CpsRebateExchangeStatusEnum.CREDITED.getStatus().equals(order.getStatus())) {
                completeCredited(order);
            } else if (CpsRebateExchangeStatusEnum.ROLLBACK_REQUIRED.getStatus().equals(order.getStatus())) {
                compensateRollback(order);
            }
        } catch (Exception ex) {
            log.warn("[compensate][exchange order retry scheduled, orderId={}]", orderId, ex);
            stepExecutor.scheduleRetry(orderId, safeMessage(ex));
        }
    }

    private void compensateProcessing(CpsRebateTokenExchangeOrderDO order) {
        if (order.getFreezeRecordId() == null) {
            order = stepExecutor.freezeOrder(order.getId());
        }
        if (!StringUtils.hasText(order.getAitokenExchangeOrderId())) {
            CpsAitokenExchangeOrderRespDTO remote = aitokenExchangeClient.submit(
                    submitRequest(order), TenantContextHolder.getTenantId());
            handleRemoteState(order, remote);
            return;
        }
        CpsAitokenExchangeOrderRespDTO remote = aitokenExchangeClient.getOrder(
                order.getAitokenExchangeOrderId(), TenantContextHolder.getTenantId());
        handleRemoteState(order, remote);
    }

    private void handleRemoteState(CpsRebateTokenExchangeOrderDO order,
                                   CpsAitokenExchangeOrderRespDTO remote) {
        String remoteOrderId = StringUtils.hasText(order.getAitokenExchangeOrderId())
                ? order.getAitokenExchangeOrderId()
                : (remote == null ? null : remote.getExchangeOrderId());
        if (isCredited(remote)) {
            stepExecutor.markCredited(order.getId(), remoteOrderId);
            order.setAitokenExchangeOrderId(remoteOrderId);
            completeCredited(order);
        } else if (isExplicitFailure(remote)) {
            stepExecutor.unfreezeAndFail(order.getId(), failureReason(remote));
        } else {
            String reason = "aitoken status pending: " + status(remote);
            stepExecutor.markProcessing(order.getId(), reason, remoteOrderId);
            stepExecutor.scheduleRetry(order.getId(), reason);
        }
    }

    private CpsAitokenExchangeSubmitReqDTO submitRequest(CpsRebateTokenExchangeOrderDO order) {
        CpsAitokenExchangeSubmitReqDTO request = new CpsAitokenExchangeSubmitReqDTO();
        request.setUserId(order.getMemberId());
        request.setTenantId(String.valueOf(TenantContextHolder.getTenantId()));
        request.setSourceSystem(order.getSourceSystem());
        request.setSourceOrderId(order.getExchangeOrderNo());
        request.setSourceAsset(order.getSourceAsset());
        request.setSourceAmount(order.getSourceAmount());
        request.setTargetAsset(order.getTargetAsset());
        request.setTargetTokens(order.getTargetTokens());
        request.setIdempotencyKey(order.getIdempotencyKey());
        return request;
    }

    private void completeCredited(CpsRebateTokenExchangeOrderDO order) {
        try {
            stepExecutor.confirmLocalDeduct(order.getId());
        } catch (Exception ex) {
            stepExecutor.markRollbackRequired(order.getId(), safeMessage(ex));
            compensateRollback(order);
            return;
        }
        aitokenExchangeClient.confirmSourceDeduct(order.getAitokenExchangeOrderId(),
                statusUpdate(order, "CPS rebate deducted"), TenantContextHolder.getTenantId());
        stepExecutor.markSuccess(order.getId());
    }

    private void compensateRollback(CpsRebateTokenExchangeOrderDO order) {
        if (!StringUtils.hasText(order.getAitokenExchangeOrderId())) {
            stepExecutor.scheduleRetry(order.getId(), "rollback requires aitoken exchange order id");
            return;
        }
        CpsAitokenExchangeOrderRespDTO remote = aitokenExchangeClient.rollback(
                order.getAitokenExchangeOrderId(), statusUpdate(order, "CPS deduct failed"),
                TenantContextHolder.getTenantId());
        if (remote != null && "rolled_back".equalsIgnoreCase(remote.getStatus())) {
            stepExecutor.unfreezeAndFail(order.getId(), "aitoken rolled back");
        } else {
            stepExecutor.scheduleRetry(order.getId(), "aitoken rollback pending: " + status(remote));
        }
    }

    private CpsAitokenExchangeStatusUpdateReqDTO statusUpdate(CpsRebateTokenExchangeOrderDO order, String reason) {
        CpsAitokenExchangeStatusUpdateReqDTO request = new CpsAitokenExchangeStatusUpdateReqDTO();
        request.setSourceOrderId(order.getExchangeOrderNo());
        request.setIdempotencyKey(order.getIdempotencyKey());
        request.setReason(reason);
        return request;
    }

    private boolean isCredited(CpsAitokenExchangeOrderRespDTO remote) {
        String status = status(remote);
        return "credited".equalsIgnoreCase(status) || "approved".equalsIgnoreCase(status)
                || "confirmed".equalsIgnoreCase(status);
    }

    private boolean isExplicitFailure(CpsAitokenExchangeOrderRespDTO remote) {
        String status = status(remote);
        return "failed".equalsIgnoreCase(status) || "rejected".equalsIgnoreCase(status)
                || "cancelled".equalsIgnoreCase(status) || "rolled_back".equalsIgnoreCase(status);
    }

    private String failureReason(CpsAitokenExchangeOrderRespDTO remote) {
        return remote != null && StringUtils.hasText(remote.getFailureReason())
                ? remote.getFailureReason() : "aitoken exchange failed: " + status(remote);
    }

    private String status(CpsAitokenExchangeOrderRespDTO remote) {
        return remote == null ? "unknown" : String.valueOf(remote.getStatus());
    }

    private String safeMessage(Exception ex) {
        return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName();
    }
}
