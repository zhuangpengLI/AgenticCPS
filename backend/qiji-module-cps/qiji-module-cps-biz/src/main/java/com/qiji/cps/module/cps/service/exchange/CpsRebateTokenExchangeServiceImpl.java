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
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAccountMapper;
import com.qiji.cps.module.cps.enums.CpsFreezeStatusEnum;
import com.qiji.cps.module.cps.enums.CpsRebateExchangeStatusEnum;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeOrderRespDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangePreviewReqDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangePreviewRespDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeStatusUpdateReqDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeSubmitReqDTO;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
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
    private CpsRebateAccountMapper rebateAccountMapper;
    @Resource
    private CpsFreezeRecordMapper freezeRecordMapper;
    @Resource
    private CpsRebateTokenExchangeOrderMapper exchangeOrderMapper;
    @Resource
    private CpsAitokenExchangeClient aitokenExchangeClient;
    @Resource
    private CpsAitokenExchangeProperties properties;

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
        CpsFreezeRecordDO existing = freezeRecordMapper.selectByBusinessAndIdempotencyKey(
                request.getBusinessType(), request.getIdempotencyKey());
        if (existing != null) {
            return mapFreeze(existing);
        }

        CpsRebateAccountDO account = rebateSettleService.getOrInitAccount(request.getUserId());
        if (account.getStatus() != null && account.getStatus() != 1) {
            throw exception(REBATE_ACCOUNT_IS_FROZEN);
        }
        int rows = rebateAccountMapper.freezeBalance(request.getUserId(), request.getAmount());
        if (rows == 0) {
            throw exception(REBATE_ACCOUNT_BALANCE_NOT_ENOUGH);
        }

        CpsFreezeRecordDO record = CpsFreezeRecordDO.builder()
                .memberId(request.getUserId())
                .businessType(request.getBusinessType())
                .businessId(request.getBusinessId())
                .idempotencyKey(request.getIdempotencyKey())
                .freezeAmount(request.getAmount())
                .status(CpsFreezeStatusEnum.FROZEN.getStatus())
                .unfreezeTime(LocalDateTime.now().plusDays(30))
                .build();
        freezeRecordMapper.insert(record);
        return mapFreeze(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreeze(String freezeId, String reason) {
        CpsFreezeRecordDO record = getFrozenRecord(freezeId);
        int rows = rebateAccountMapper.unfreezeBalance(record.getMemberId(), record.getFreezeAmount());
        if (rows == 0) {
            throw exception(REBATE_ACCOUNT_BALANCE_NOT_ENOUGH);
        }
        CpsFreezeRecordDO update = new CpsFreezeRecordDO();
        update.setId(record.getId());
        update.setStatus(CpsFreezeStatusEnum.UNFREEZED.getStatus());
        update.setActualUnfreezeTime(LocalDateTime.now());
        freezeRecordMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDeduct(String freezeId, String exchangeOrderId) {
        CpsFreezeRecordDO record = getFrozenRecord(freezeId);
        int rows = rebateAccountMapper.deductFrozenBalance(record.getMemberId(), record.getFreezeAmount());
        if (rows == 0) {
            throw exception(REBATE_ACCOUNT_BALANCE_NOT_ENOUGH);
        }
        CpsFreezeRecordDO update = new CpsFreezeRecordDO();
        update.setId(record.getId());
        update.setStatus(CpsFreezeStatusEnum.DEDUCTED.getStatus());
        update.setActualUnfreezeTime(LocalDateTime.now());
        freezeRecordMapper.updateById(update);
    }

    @Override
    public CpsAitokenExchangePreviewRespDTO preview(Long memberId, BigDecimal amount) {
        validateAmount(amount);
        CpsRebateAccountDO account = rebateSettleService.getOrInitAccount(memberId);
        if (account.getAvailableBalance().compareTo(amount) < 0) {
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
    @Transactional(rollbackFor = Exception.class)
    public CpsRebateTokenExchangeOrderDO submit(Long memberId, BigDecimal amount, String idempotencyKey) {
        validateAmount(amount);
        if (!StringUtils.hasText(idempotencyKey)) {
            throw exception(REBATE_EXCHANGE_IDEMPOTENCY_KEY_REQUIRED);
        }
        CpsRebateTokenExchangeOrderDO existing = exchangeOrderMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing;
        }

        Long tenantId = TenantContextHolder.getTenantId();
        CpsAitokenExchangePreviewRespDTO preview = preview(memberId, amount);
        String exchangeOrderNo = generateExchangeOrderNo();
        CpsRebateTokenExchangeOrderDO order = CpsRebateTokenExchangeOrderDO.builder()
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
        exchangeOrderMapper.insert(order);

        try {
            OpenApiCpsRebateFreezeReqVO freezeReq = new OpenApiCpsRebateFreezeReqVO();
            freezeReq.setUserId(memberId);
            freezeReq.setAmount(amount);
            freezeReq.setBusinessType(BUSINESS_TYPE_TOKEN_EXCHANGE);
            freezeReq.setBusinessId(exchangeOrderNo);
            freezeReq.setIdempotencyKey(idempotencyKey);
            OpenApiCpsRebateFreezeRespVO freezeResp = freeze(freezeReq);
            updateOrderStatus(order, CpsRebateExchangeStatusEnum.FROZEN, null, freezeResp.getFreezeId(), null);

            CpsAitokenExchangeSubmitReqDTO submitReq = new CpsAitokenExchangeSubmitReqDTO();
            submitReq.setUserId(memberId);
            submitReq.setTenantId(String.valueOf(tenantId));
            submitReq.setSourceSystem(properties.getSourceSystem());
            submitReq.setSourceOrderId(exchangeOrderNo);
            submitReq.setSourceAsset(properties.getSourceAsset());
            submitReq.setSourceAmount(amount);
            submitReq.setTargetAsset(properties.getTargetAsset());
            submitReq.setTargetTokens(preview.getActualTokens());
            submitReq.setIdempotencyKey(idempotencyKey);
            CpsAitokenExchangeOrderRespDTO aitokenOrder = aitokenExchangeClient.submit(submitReq, tenantId);

            if (isAitokenCredited(aitokenOrder)) {
                updateOrderStatus(order, CpsRebateExchangeStatusEnum.CREDITED, null,
                        freezeResp.getFreezeId(), aitokenOrder.getExchangeOrderId());
                try {
                    confirmDeduct(freezeResp.getFreezeId(), exchangeOrderNo);
                    aitokenExchangeClient.confirmSourceDeduct(aitokenOrder.getExchangeOrderId(),
                            buildStatusUpdateRequest(exchangeOrderNo, idempotencyKey, "CPS返利已确认扣减"), tenantId);
                    updateOrderStatus(order, CpsRebateExchangeStatusEnum.SUCCESS, null,
                            freezeResp.getFreezeId(), aitokenOrder.getExchangeOrderId());
                } catch (Exception deductException) {
                    log.error("[submit] CPS确认扣减失败，尝试请求aitoken回滚, exchangeOrderNo={}", exchangeOrderNo, deductException);
                    try {
                        aitokenExchangeClient.rollback(aitokenOrder.getExchangeOrderId(),
                                buildStatusUpdateRequest(exchangeOrderNo, idempotencyKey, deductException.getMessage()), tenantId);
                    } catch (Exception rollbackException) {
                        log.error("[submit] aitoken回滚请求失败, exchangeOrderNo={}, aitokenOrderId={}",
                                exchangeOrderNo, aitokenOrder.getExchangeOrderId(), rollbackException);
                    }
                    updateOrderStatus(order, CpsRebateExchangeStatusEnum.ROLLBACK_REQUIRED,
                            deductException.getMessage(), freezeResp.getFreezeId(), aitokenOrder.getExchangeOrderId());
                }
            } else if (isAitokenFailed(aitokenOrder)) {
                String failureReason = aitokenOrder == null ? "aitoken返回为空" : aitokenOrder.getFailureReason();
                unfreeze(freezeResp.getFreezeId(), failureReason);
                updateOrderStatus(order, CpsRebateExchangeStatusEnum.FAILED, failureReason,
                        freezeResp.getFreezeId(), aitokenOrder == null ? null : aitokenOrder.getExchangeOrderId());
            } else {
                String reason = aitokenOrder == null ? "aitoken返回为空" : "aitoken状态处理中：" + aitokenOrder.getStatus();
                updateOrderStatus(order, CpsRebateExchangeStatusEnum.PROCESSING, reason,
                        freezeResp.getFreezeId(), aitokenOrder == null ? null : aitokenOrder.getExchangeOrderId());
            }
        } catch (Exception e) {
            log.error("[submit] 返利兑换Token链路异常, exchangeOrderNo={}", exchangeOrderNo, e);
            updateOrderStatus(order, CpsRebateExchangeStatusEnum.PROCESSING, e.getMessage(), null, null);
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

    private CpsFreezeRecordDO getFrozenRecord(String freezeId) {
        CpsFreezeRecordDO record = freezeRecordMapper.selectById(Long.valueOf(freezeId));
        if (record == null) {
            throw exception(FREEZE_RECORD_NOT_EXISTS);
        }
        if (!CpsFreezeStatusEnum.FROZEN.getStatus().equals(record.getStatus())) {
            throw exception(FREEZE_RECORD_STATUS_INVALID);
        }
        return record;
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

    private void updateOrderStatus(CpsRebateTokenExchangeOrderDO order, CpsRebateExchangeStatusEnum status,
                                   String failureReason, String freezeId, String aitokenExchangeOrderId) {
        CpsRebateTokenExchangeOrderDO update = new CpsRebateTokenExchangeOrderDO();
        update.setId(order.getId());
        update.setStatus(status.getStatus());
        update.setFailureReason(failureReason);
        update.setAitokenExchangeOrderId(aitokenExchangeOrderId);
        if (StringUtils.hasText(freezeId)) {
            update.setFreezeRecordId(Long.valueOf(freezeId));
        }
        if (CpsRebateExchangeStatusEnum.SUCCESS == status || CpsRebateExchangeStatusEnum.FAILED == status) {
            update.setCompletedAt(LocalDateTime.now());
        }
        exchangeOrderMapper.updateById(update);
    }

    private boolean isAitokenCredited(CpsAitokenExchangeOrderRespDTO aitokenOrder) {
        return aitokenOrder != null
                && ("credited".equalsIgnoreCase(aitokenOrder.getStatus())
                || "approved".equalsIgnoreCase(aitokenOrder.getStatus())
                || "confirmed".equalsIgnoreCase(aitokenOrder.getStatus()));
    }

    private boolean isAitokenFailed(CpsAitokenExchangeOrderRespDTO aitokenOrder) {
        return aitokenOrder == null || "failed".equalsIgnoreCase(aitokenOrder.getStatus())
                || "rejected".equalsIgnoreCase(aitokenOrder.getStatus())
                || "cancelled".equalsIgnoreCase(aitokenOrder.getStatus());
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
