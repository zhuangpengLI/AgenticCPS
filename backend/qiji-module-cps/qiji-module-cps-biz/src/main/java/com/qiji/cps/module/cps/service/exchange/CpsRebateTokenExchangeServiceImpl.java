package com.qiji.cps.module.cps.service.exchange;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.cps.config.CpsAitokenExchangeProperties;
import com.qiji.cps.module.cps.controller.openapi.rebate.vo.OpenApiCpsRebateBalanceRespVO;
import com.qiji.cps.module.cps.controller.openapi.rebate.vo.OpenApiCpsRebateFreezeReqVO;
import com.qiji.cps.module.cps.controller.openapi.rebate.vo.OpenApiCpsRebateFreezeRespVO;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import com.qiji.cps.module.cps.dal.mysql.exchange.CpsRebateTokenExchangeOrderMapper;
import com.qiji.cps.module.cps.enums.CpsFreezeStatusEnum;
import com.qiji.cps.module.cps.enums.CpsRebateExchangeStatusEnum;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeOrderRespDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangePreviewReqDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangePreviewRespDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeStatusUpdateReqDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeSubmitReqDTO;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsAssetOperatorContext;
import com.qiji.cps.module.cps.service.rebate.asset.CpsMoneyConverter;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.*;

@Slf4j
@Service
@Validated
public class CpsRebateTokenExchangeServiceImpl implements CpsRebateTokenExchangeService {

    private static final String BUSINESS_TYPE_TOKEN_EXCHANGE = "TOKEN_EXCHANGE";
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    @Resource
    private CpsRebateSettleService rebateSettleService;
    @Resource
    private CpsRebateTokenExchangeOrderMapper exchangeOrderMapper;
    @Resource
    private CpsAitokenExchangeClient aitokenExchangeClient;
    @Resource
    private CpsAitokenExchangeProperties properties;
    @Resource
    private CpsRebateAssetService rebateAssetService;
    @Resource
    private CpsMoneyConverter moneyConverter;
    @Resource
    private CpsRebateTokenExchangeStepExecutor stepExecutor;

    @Override
    public OpenApiCpsRebateBalanceRespVO getBalance(Long memberId) {
        CpsRebateAccountDO account = rebateSettleService.getOrInitAccount(memberId);
        return OpenApiCpsRebateBalanceRespVO.builder()
                .userId(memberId)
                .tenantId(TenantContextHolder.getTenantId())
                .pending(BigDecimal.ZERO)
                .settled(account.getTotalRebate())
                .available(account.getAvailableBalance())
                .frozen(account.getFrozenBalance())
                .exchanged(BigDecimal.ZERO)
                .withdrawn(account.getWithdrawnAmount())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenApiCpsRebateFreezeRespVO freeze(OpenApiCpsRebateFreezeReqVO request) {
        validateAmount(request.getAmount());
        CpsRebateAccountDO account = rebateSettleService.getOrInitAccount(request.getUserId());
        if (account.getStatus() != null && account.getStatus() != 1) {
            throw exception(REBATE_ACCOUNT_IS_FROZEN);
        }
        CpsFreezeRecordDO record = rebateAssetService.freezeAvailableForExchange(request.getUserId(),
                moneyConverter.yuanToCent(request.getAmount()), request.getBusinessId(), request.getIdempotencyKey(),
                serviceContext(request.getIdempotencyKey(), "Token兑换冻结返利"));
        return mapFreeze(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreeze(String freezeId, String reason) {
        Long recordId = Long.valueOf(freezeId);
        String key = "token-unfreeze:" + freezeId;
        rebateAssetService.unfreezeExchangeAsset(recordId, key,
                serviceContext(key, StringUtils.hasText(reason) ? reason : "Token兑换失败解冻"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDeduct(String freezeId, String exchangeOrderId) {
        Long recordId = Long.valueOf(freezeId);
        String key = "token-deduct:" + freezeId;
        rebateAssetService.confirmExchangeDeduct(recordId, key,
                serviceContext(key, "Token兑换确认扣减:" + exchangeOrderId));
    }

    @Override
    public CpsAitokenExchangePreviewRespDTO preview(Long memberId, BigDecimal amount) {
        validateAmount(amount);
        CpsRebateAccountDO account = rebateSettleService.getOrInitAccount(memberId);
        if ((account.getDebtBalance() != null && account.getDebtBalance().signum() > 0)
                || account.getAvailableBalance().compareTo(amount) < 0) {
            throw exception(REBATE_ACCOUNT_BALANCE_NOT_ENOUGH);
        }
        CpsAitokenExchangePreviewReqDTO request = new CpsAitokenExchangePreviewReqDTO();
        request.setUserId(memberId);
        request.setTenantId(String.valueOf(TenantContextHolder.getTenantId()));
        request.setSourceSystem(properties.getSourceSystem());
        request.setSourceAsset(properties.getSourceAsset());
        request.setSourceAmount(amount);
        request.setTargetAsset(properties.getTargetAsset());
        return aitokenExchangeClient.preview(request, TenantContextHolder.getTenantId());
    }

    @Override
    public CpsRebateTokenExchangeOrderDO submit(Long memberId, BigDecimal amount, String idempotencyKey) {
        validateAmount(amount);
        if (!StringUtils.hasText(idempotencyKey)) {
            throw exception(REBATE_EXCHANGE_IDEMPOTENCY_KEY_REQUIRED);
        }
        CpsRebateTokenExchangeOrderDO existing = exchangeOrderMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            validateIdempotentRequest(existing, memberId, amount);
            return existing;
        }

        Long tenantId = TenantContextHolder.getTenantId();
        CpsAitokenExchangePreviewRespDTO preview = preview(memberId, amount);
        String exchangeOrderNo = generateExchangeOrderNo();
        CpsRebateTokenExchangeOrderDO candidate = CpsRebateTokenExchangeOrderDO.builder()
                .exchangeOrderNo(exchangeOrderNo)
                .memberId(memberId)
                .sourceSystem(properties.getSourceSystem())
                .sourceAsset(properties.getSourceAsset())
                .sourceAmount(amount)
                .targetAsset(properties.getTargetAsset())
                .targetTokens(preview.getActualTokens())
                .exchangeRate(preview.getExchangeRate())
                .status(CpsRebateExchangeStatusEnum.INIT.getStatus())
                .idempotencyKey(idempotencyKey)
                .build();
        CpsRebateTokenExchangeOrderDO order = stepExecutor.createOrder(candidate);
        validateIdempotentRequest(order, memberId, amount);
        if (!exchangeOrderNo.equals(order.getExchangeOrderNo())) {
            return order;
        }

        try {
            order = stepExecutor.freezeOrder(order.getId());

            CpsAitokenExchangeSubmitReqDTO submitReq = new CpsAitokenExchangeSubmitReqDTO();
            submitReq.setUserId(order.getMemberId());
            submitReq.setTenantId(String.valueOf(tenantId));
            submitReq.setSourceSystem(order.getSourceSystem());
            submitReq.setSourceOrderId(order.getExchangeOrderNo());
            submitReq.setSourceAsset(order.getSourceAsset());
            submitReq.setSourceAmount(order.getSourceAmount());
            submitReq.setTargetAsset(order.getTargetAsset());
            submitReq.setTargetTokens(order.getTargetTokens());
            submitReq.setIdempotencyKey(order.getIdempotencyKey());
            CpsAitokenExchangeOrderRespDTO aitokenOrder = aitokenExchangeClient.submit(submitReq, tenantId);

            if (isAitokenCredited(aitokenOrder)) {
                stepExecutor.markCredited(order.getId(), aitokenOrder.getExchangeOrderId());
                boolean localDeducted = false;
                try {
                    stepExecutor.confirmLocalDeduct(order.getId());
                    localDeducted = true;
                } catch (Exception deductException) {
                    log.error("[submit] CPS确认扣减失败，尝试请求aitoken回滚, exchangeOrderNo={}",
                            order.getExchangeOrderNo(), deductException);
                    stepExecutor.markRollbackRequired(order.getId(), deductException.getMessage());
                    try {
                        CpsAitokenExchangeOrderRespDTO rollback = aitokenExchangeClient.rollback(aitokenOrder.getExchangeOrderId(),
                                buildStatusUpdateRequest(order.getExchangeOrderNo(), order.getIdempotencyKey(),
                                        deductException.getMessage()), tenantId);
                        if (rollback != null && "rolled_back".equalsIgnoreCase(rollback.getStatus())) {
                            stepExecutor.unfreezeAndFail(order.getId(), "aitoken rolled back");
                        } else {
                            stepExecutor.scheduleRetry(order.getId(), "aitoken rollback pending");
                        }
                    } catch (Exception rollbackException) {
                        log.error("[submit] aitoken回滚请求失败, exchangeOrderNo={}, aitokenOrderId={}",
                                order.getExchangeOrderNo(), aitokenOrder.getExchangeOrderId(), rollbackException);
                        stepExecutor.scheduleRetry(order.getId(), rollbackException.getMessage());
                    }
                }
                if (localDeducted) {
                    try {
                        aitokenExchangeClient.confirmSourceDeduct(aitokenOrder.getExchangeOrderId(),
                                buildStatusUpdateRequest(order.getExchangeOrderNo(), order.getIdempotencyKey(),
                                        "CPS返利已确认扣减"), tenantId);
                        stepExecutor.markSuccess(order.getId());
                    } catch (Exception confirmException) {
                        log.warn("[submit] CPS已扣减但aitoken确认超时，保持CREDITED等待补偿, exchangeOrderNo={}",
                                order.getExchangeOrderNo(), confirmException);
                        stepExecutor.scheduleRetry(order.getId(), confirmException.getMessage());
                    }
                }
            } else if (isAitokenFailed(aitokenOrder)) {
                String failureReason = aitokenOrder == null ? "aitoken返回为空" : aitokenOrder.getFailureReason();
                stepExecutor.unfreezeAndFail(order.getId(), failureReason);
            } else {
                String reason = aitokenOrder == null ? "aitoken返回为空" : "aitoken状态处理中：" + aitokenOrder.getStatus();
                stepExecutor.markProcessing(order.getId(), reason,
                        aitokenOrder == null ? null : aitokenOrder.getExchangeOrderId());
            }
        } catch (Exception e) {
            log.error("[submit] 返利兑换Token链路异常, exchangeOrderNo={}", order.getExchangeOrderNo(), e);
            stepExecutor.markProcessing(order.getId(), e.getMessage(), order.getAitokenExchangeOrderId());
        }
        return exchangeOrderMapper.selectById(order.getId());
    }

    @Override
    public CpsRebateTokenExchangeOrderDO getExchangeOrder(String exchangeOrderNo) {
        CpsRebateTokenExchangeOrderDO order = exchangeOrderMapper.selectByExchangeOrderNo(exchangeOrderNo);
        if (order == null) {
            throw exception(REBATE_EXCHANGE_NOT_EXISTS);
        }
        return order;
    }

    @Override
    public CpsRebateTokenExchangeOrderDO getExchangeOrder(Long memberId, String exchangeOrderNo) {
        CpsRebateTokenExchangeOrderDO order = exchangeOrderMapper
                .selectByMemberIdAndExchangeOrderNo(memberId, exchangeOrderNo);
        if (order == null) {
            throw exception(REBATE_EXCHANGE_NOT_EXISTS);
        }
        return order;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(REBATE_EXCHANGE_AMOUNT_INVALID);
        }
    }

    private OpenApiCpsRebateFreezeRespVO mapFreeze(CpsFreezeRecordDO record) {
        return OpenApiCpsRebateFreezeRespVO.builder()
                .freezeId(String.valueOf(record.getId()))
                .userId(record.getMemberId())
                .amount(record.getFreezeAmount())
                .businessType(record.getBusinessType())
                .businessId(record.getBusinessId())
                .status(record.getStatus())
                .idempotencyKey(record.getIdempotencyKey())
                .build();
    }

    private void validateIdempotentRequest(CpsRebateTokenExchangeOrderDO persistedOrder,
                                           Long memberId, BigDecimal amount) {
        if (persistedOrder == null || !memberId.equals(persistedOrder.getMemberId())
                || persistedOrder.getSourceAmount() == null
                || amount.compareTo(persistedOrder.getSourceAmount()) != 0) {
            throw new IllegalStateException("幂等键已用于其他会员或金额的兑换请求");
        }
    }

    private CpsAssetOperatorContext serviceContext(String idempotencyKey, String reason) {
        return new CpsAssetOperatorContext("SERVICE", "aitoken-exchange", idempotencyKey, reason);
    }

    private boolean isAitokenCredited(CpsAitokenExchangeOrderRespDTO aitokenOrder) {
        return aitokenOrder != null
                && ("credited".equalsIgnoreCase(aitokenOrder.getStatus())
                || "approved".equalsIgnoreCase(aitokenOrder.getStatus())
                || "confirmed".equalsIgnoreCase(aitokenOrder.getStatus()));
    }

    private boolean isAitokenFailed(CpsAitokenExchangeOrderRespDTO aitokenOrder) {
        return aitokenOrder != null && ("failed".equalsIgnoreCase(aitokenOrder.getStatus())
                || "rejected".equalsIgnoreCase(aitokenOrder.getStatus())
                || "cancelled".equalsIgnoreCase(aitokenOrder.getStatus())
                || "rolled_back".equalsIgnoreCase(aitokenOrder.getStatus()));
    }

    private CpsAitokenExchangeStatusUpdateReqDTO buildStatusUpdateRequest(String exchangeOrderNo,
                                                                          String idempotencyKey,
                                                                          String reason) {
        CpsAitokenExchangeStatusUpdateReqDTO request = new CpsAitokenExchangeStatusUpdateReqDTO();
        request.setSourceOrderId(exchangeOrderNo);
        request.setIdempotencyKey(idempotencyKey);
        request.setReason(reason);
        return request;
    }

    private String generateExchangeOrderNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int seq = SEQUENCE.incrementAndGet() % 10000;
        return "CPSX" + datePart + String.format("%04d", seq);
    }
}
