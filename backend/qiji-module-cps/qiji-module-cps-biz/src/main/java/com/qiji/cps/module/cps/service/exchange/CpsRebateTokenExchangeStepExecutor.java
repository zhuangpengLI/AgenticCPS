package com.qiji.cps.module.cps.service.exchange;

import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.dal.mysql.exchange.CpsRebateTokenExchangeOrderMapper;
import com.qiji.cps.module.cps.enums.CpsRebateExchangeStatusEnum;
import com.qiji.cps.module.cps.service.rebate.asset.CpsAssetOperatorContext;
import com.qiji.cps.module.cps.service.rebate.asset.CpsMoneyConverter;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetService;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Executes one local exchange state transition per independent transaction. */
@Service
public class CpsRebateTokenExchangeStepExecutor {

    @Resource
    private CpsRebateTokenExchangeOrderMapper exchangeOrderMapper;
    @Resource
    private CpsRebateAssetService rebateAssetService;
    @Resource
    private CpsMoneyConverter moneyConverter;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CpsRebateTokenExchangeOrderDO createOrder(CpsRebateTokenExchangeOrderDO order) {
        CpsRebateTokenExchangeOrderDO existing = exchangeOrderMapper
                .selectByIdempotencyKey(order.getIdempotencyKey());
        if (existing != null) return existing;
        if (order.getRetryCount() == null) order.setRetryCount(0);
        if (order.getStatusVersion() == null) order.setStatusVersion(0);
        try {
            exchangeOrderMapper.insert(order);
            return order;
        } catch (DuplicateKeyException ex) {
            existing = exchangeOrderMapper.selectByIdempotencyKey(order.getIdempotencyKey());
            if (existing != null) return existing;
            throw ex;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CpsRebateTokenExchangeOrderDO freezeOrder(Long orderId) {
        CpsRebateTokenExchangeOrderDO order = requireOrder(orderId);
        if (order.getFreezeRecordId() != null) return order;
        requireAllowedStatus(order, List.of(CpsRebateExchangeStatusEnum.INIT.getStatus(),
                CpsRebateExchangeStatusEnum.PROCESSING.getStatus()));
        CpsFreezeRecordDO freeze = rebateAssetService.freezeAvailableForExchange(order.getMemberId(),
                moneyConverter.yuanToCent(order.getSourceAmount()), order.getExchangeOrderNo(),
                order.getIdempotencyKey(), new CpsAssetOperatorContext("SERVICE", "aitoken-exchange",
                        order.getIdempotencyKey(), "Token兑换冻结返利"));
        CpsRebateTokenExchangeOrderDO update = new CpsRebateTokenExchangeOrderDO();
        update.setId(orderId);
        update.setFreezeRecordId(freeze.getId());
        update.setStatus(CpsRebateExchangeStatusEnum.FROZEN.getStatus());
        updateWithCas(update, order, List.of(CpsRebateExchangeStatusEnum.INIT.getStatus(),
                CpsRebateExchangeStatusEnum.PROCESSING.getStatus()));
        return requireOrder(orderId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markCredited(Long orderId, String remoteOrderId) {
        updateStatus(orderId, CpsRebateExchangeStatusEnum.CREDITED, null, remoteOrderId, false,
                List.of(CpsRebateExchangeStatusEnum.FROZEN.getStatus(),
                        CpsRebateExchangeStatusEnum.PROCESSING.getStatus()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void confirmLocalDeduct(Long orderId) {
        CpsRebateTokenExchangeOrderDO order = requireOrder(orderId);
        if (order.getFreezeRecordId() == null) {
            throw new IllegalStateException("exchange order has no freeze record: " + orderId);
        }
        String key = "token-deduct:" + order.getFreezeRecordId();
        rebateAssetService.confirmExchangeDeduct(order.getFreezeRecordId(), key,
                new CpsAssetOperatorContext("SERVICE", "aitoken-exchange", key,
                        "Token兑换补偿确认扣减:" + order.getExchangeOrderNo()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markSuccess(Long orderId) {
        updateStatus(orderId, CpsRebateExchangeStatusEnum.SUCCESS, null, null, true,
                List.of(CpsRebateExchangeStatusEnum.CREDITED.getStatus()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markRollbackRequired(Long orderId, String reason) {
        updateStatus(orderId, CpsRebateExchangeStatusEnum.ROLLBACK_REQUIRED, reason, null, false,
                List.of(CpsRebateExchangeStatusEnum.CREDITED.getStatus()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markProcessing(Long orderId, String reason, String remoteOrderId) {
        updateStatus(orderId, CpsRebateExchangeStatusEnum.PROCESSING, reason, remoteOrderId, false,
                List.of(CpsRebateExchangeStatusEnum.INIT.getStatus(),
                        CpsRebateExchangeStatusEnum.FROZEN.getStatus(),
                        CpsRebateExchangeStatusEnum.PROCESSING.getStatus()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void unfreezeAndFail(Long orderId, String reason) {
        CpsRebateTokenExchangeOrderDO order = requireOrder(orderId);
        CpsRebateTokenExchangeOrderDO update = new CpsRebateTokenExchangeOrderDO();
        update.setId(orderId);
        update.setStatus(CpsRebateExchangeStatusEnum.FAILED.getStatus());
        update.setFailureReason(reason);
        update.setCompletedAt(LocalDateTime.now());
        update.setNextRetryTime(null);
        update.setLastCompensationAt(LocalDateTime.now());
        updateWithCas(update, order, List.of(CpsRebateExchangeStatusEnum.INIT.getStatus(),
                CpsRebateExchangeStatusEnum.FROZEN.getStatus(),
                CpsRebateExchangeStatusEnum.PROCESSING.getStatus(),
                CpsRebateExchangeStatusEnum.ROLLBACK_REQUIRED.getStatus()));
        if (order.getFreezeRecordId() != null) {
            String key = "token-unfreeze:" + order.getFreezeRecordId();
            rebateAssetService.unfreezeExchangeAsset(order.getFreezeRecordId(), key,
                    new CpsAssetOperatorContext("SERVICE", "aitoken-exchange", key,
                            reason == null ? "Token兑换失败解冻" : reason));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void scheduleRetry(Long orderId, String reason) {
        CpsRebateTokenExchangeOrderDO order = requireOrder(orderId);
        List<String> retryableStatuses = List.of(CpsRebateExchangeStatusEnum.PROCESSING.getStatus(),
                CpsRebateExchangeStatusEnum.CREDITED.getStatus(),
                CpsRebateExchangeStatusEnum.ROLLBACK_REQUIRED.getStatus());
        requireAllowedStatus(order, retryableStatuses);
        int retryCount = order.getRetryCount() == null ? 1 : order.getRetryCount() + 1;
        long delaySeconds = Math.min(3600L, 30L << Math.min(retryCount - 1, 7));
        CpsRebateTokenExchangeOrderDO update = new CpsRebateTokenExchangeOrderDO();
        update.setId(orderId);
        update.setFailureReason(reason);
        update.setRetryCount(retryCount);
        update.setLastCompensationAt(LocalDateTime.now());
        update.setNextRetryTime(LocalDateTime.now().plusSeconds(delaySeconds));
        updateWithCas(update, order, retryableStatuses);
    }

    /**
     * 为一次补偿领取短租约。只有扫描到的状态和版本仍然有效时才能领取成功。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CpsRebateTokenExchangeOrderDO claimCompensation(CpsRebateTokenExchangeOrderDO scannedOrder) {
        if (scannedOrder == null || scannedOrder.getId() == null) return null;
        List<String> claimableStatuses = List.of(CpsRebateExchangeStatusEnum.PROCESSING.getStatus(),
                CpsRebateExchangeStatusEnum.CREDITED.getStatus(),
                CpsRebateExchangeStatusEnum.ROLLBACK_REQUIRED.getStatus());
        if (!claimableStatuses.contains(scannedOrder.getStatus())) return null;
        CpsRebateTokenExchangeOrderDO update = new CpsRebateTokenExchangeOrderDO();
        update.setId(scannedOrder.getId());
        update.setStatus(scannedOrder.getStatus());
        update.setLastCompensationAt(LocalDateTime.now());
        update.setNextRetryTime(LocalDateTime.now().plusSeconds(60));
        int affected = exchangeOrderMapper.updateByIdAndStatusVersion(update,
                scannedOrder.getStatusVersion(), List.of(scannedOrder.getStatus()));
        return affected == 1 ? requireOrder(scannedOrder.getId()) : null;
    }

    private void updateStatus(Long orderId, CpsRebateExchangeStatusEnum status, String reason,
                              String remoteOrderId, boolean completed, List<String> allowedSourceStatuses) {
        CpsRebateTokenExchangeOrderDO order = requireOrder(orderId);
        requireAllowedStatus(order, allowedSourceStatuses);
        CpsRebateTokenExchangeOrderDO update = new CpsRebateTokenExchangeOrderDO();
        update.setId(orderId);
        update.setStatus(status.getStatus());
        update.setFailureReason(reason);
        if (remoteOrderId != null && !remoteOrderId.isBlank()) {
            update.setAitokenExchangeOrderId(remoteOrderId);
        }
        update.setNextRetryTime(null);
        update.setLastCompensationAt(LocalDateTime.now());
        if (completed) update.setCompletedAt(LocalDateTime.now());
        updateWithCas(update, order, allowedSourceStatuses);
    }

    private CpsRebateTokenExchangeOrderDO requireOrder(Long orderId) {
        CpsRebateTokenExchangeOrderDO order = exchangeOrderMapper.selectById(orderId);
        if (order == null) throw new IllegalStateException("exchange order not found: " + orderId);
        return order;
    }

    private void requireAllowedStatus(CpsRebateTokenExchangeOrderDO order, List<String> allowedStatuses) {
        if (!allowedStatuses.contains(order.getStatus())) {
            throw new IllegalStateException("illegal exchange state transition from " + order.getStatus());
        }
    }

    private void updateWithCas(CpsRebateTokenExchangeOrderDO update,
                               CpsRebateTokenExchangeOrderDO current,
                               List<String> allowedSourceStatuses) {
        int affected = exchangeOrderMapper.updateByIdAndStatusVersion(
                update, current.getStatusVersion(), allowedSourceStatuses);
        if (affected != 1) {
            throw new IllegalStateException("exchange order state changed concurrently: " + current.getId());
        }
    }
}
