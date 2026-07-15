package com.qiji.cps.module.cps.service.withdraw;

import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
import com.qiji.cps.module.cps.dal.mysql.withdraw.CpsWithdrawMapper;
import com.qiji.cps.module.cps.enums.CpsWithdrawStatusEnum;
import com.qiji.cps.module.cps.service.rebate.asset.CpsAssetOperatorContext;
import com.qiji.cps.module.cps.service.rebate.asset.CpsMoneyConverter;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.WITHDRAW_NOT_EXISTS;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.WITHDRAW_STATUS_INVALID;

@Component
public class CpsWithdrawStepExecutor {

    static final String TRANSFER_WAITING = "WAITING";
    static final String TRANSFER_PROCESSING = "PROCESSING";
    static final String TRANSFER_SUCCESS = "SUCCESS";
    static final String TRANSFER_FAILED = "FAILED";

    @Resource private CpsWithdrawMapper withdrawMapper;
    @Resource private CpsRebateAssetService assetService;
    @Resource private CpsMoneyConverter moneyConverter;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CpsWithdrawDO createAndFreeze(CpsWithdrawCreateCommand command) {
        CpsWithdrawDO replay = withdrawMapper.selectByIdempotencyKey(command.idempotencyKey());
        if (replay != null) return validateReplay(replay, command);
        String withdrawNo = "CPSW" + UUID.randomUUID().toString().replace("-", "");
        CpsWithdrawDO withdraw = CpsWithdrawDO.builder()
                .memberId(command.memberId()).withdrawNo(withdrawNo)
                .withdrawType(command.withdrawType()).withdrawAccount(command.withdrawAccount())
                .withdrawAccountName(command.withdrawAccountName()).amountCent(command.amountCent())
                .amount(moneyConverter.centToYuan(command.amountCent())).feeAmount(BigDecimal.ZERO)
                .actualAmount(moneyConverter.centToYuan(command.amountCent()))
                .status(CpsWithdrawStatusEnum.CREATED.getStatus()).transferStatus(TRANSFER_WAITING)
                .transferChannelCode(channelCode(command.withdrawType()))
                .idempotencyKey(command.idempotencyKey()).statusVersion(0).retryCount(0).build();
        withdrawMapper.insert(withdraw);
        CpsFreezeRecordDO freeze = assetService.freezeAvailableForWithdrawal(command.memberId(), command.amountCent(),
                withdrawNo, command.idempotencyKey(), CpsAssetOperatorContext.member(command.memberId(),
                        command.idempotencyKey(), "会员申请提现"));
        CpsWithdrawDO update = CpsWithdrawDO.builder().id(withdraw.getId()).freezeRecordId(freeze.getId()).build();
        if (withdrawMapper.updateById(update) != 1) throw new IllegalStateException("提现冻结关联失败");
        withdraw.setFreezeRecordId(freeze.getId());
        return withdraw;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CpsWithdrawDO markReviewing(Long id, String reviewNote, Long auditUserId) {
        CpsWithdrawDO withdraw = requireWithdraw(id);
        if (CpsWithdrawStatusEnum.REVIEWING.getStatus().equals(withdraw.getStatus())
                || isTerminal(withdraw.getStatus())) return withdraw;
        if (!CpsWithdrawStatusEnum.CREATED.getStatus().equals(withdraw.getStatus())) {
            throw exception(WITHDRAW_STATUS_INVALID);
        }
        CpsWithdrawDO update = CpsWithdrawDO.builder().id(id)
                .status(CpsWithdrawStatusEnum.REVIEWING.getStatus()).auditUserId(auditUserId)
                .auditTime(LocalDateTime.now()).reviewNote(reviewNote).build();
        if (withdrawMapper.updateByIdAndStatusVersion(update, withdraw.getStatusVersion(),
                List.of(CpsWithdrawStatusEnum.CREATED.getStatus())) != 1) {
            return requireWithdraw(id);
        }
        return requireWithdraw(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CpsWithdrawDO rejectAndUnfreeze(Long id, String reason, String idempotencyKey) {
        CpsWithdrawDO withdraw = requireWithdraw(id);
        if (CpsWithdrawStatusEnum.REJECTED.getStatus().equals(withdraw.getStatus())) return withdraw;
        if (!CpsWithdrawStatusEnum.CREATED.getStatus().equals(withdraw.getStatus())) {
            throw exception(WITHDRAW_STATUS_INVALID);
        }
        CpsWithdrawDO update = CpsWithdrawDO.builder().id(id)
                .status(CpsWithdrawStatusEnum.REJECTED.getStatus()).auditUserId(SecurityFrameworkUtils.getLoginUserId())
                .auditTime(LocalDateTime.now()).reviewNote(reason).transferStatus(TRANSFER_FAILED).build();
        if (withdrawMapper.updateByIdAndStatusVersion(update, withdraw.getStatusVersion(),
                List.of(CpsWithdrawStatusEnum.CREATED.getStatus())) != 1) throw exception(WITHDRAW_STATUS_INVALID);
        assetService.unfreezeWithdrawalAsset(withdraw.getFreezeRecordId(), idempotencyKey,
                CpsAssetOperatorContext.admin(String.valueOf(SecurityFrameworkUtils.getLoginUserId()),
                        idempotencyKey, reason));
        return requireWithdraw(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CpsWithdrawDO claimTransfer(Long id) {
        CpsWithdrawDO withdraw = requireWithdraw(id);
        if (!CpsWithdrawStatusEnum.REVIEWING.getStatus().equals(withdraw.getStatus())) return null;
        LocalDateTime now = LocalDateTime.now();
        if (withdraw.getNextRetryTime() != null && withdraw.getNextRetryTime().isAfter(now)) return null;
        CpsWithdrawDO update = CpsWithdrawDO.builder().id(id).transferStatus(TRANSFER_PROCESSING)
                .lastAttemptTime(now).nextRetryTime(now.plusMinutes(1)).build();
        if (withdrawMapper.updateByIdAndStatusVersion(update, withdraw.getStatusVersion(),
                List.of(CpsWithdrawStatusEnum.REVIEWING.getStatus())) != 1) return null;
        return requireWithdraw(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CpsWithdrawDO attachPayTransfer(Long id, Long payTransferId) {
        CpsWithdrawDO withdraw = requireWithdraw(id);
        if (withdraw.getPayTransferId() != null) {
            if (!withdraw.getPayTransferId().equals(payTransferId)) {
                throw new IllegalStateException("提现单已绑定其他 Pay 转账单");
            }
            return withdraw;
        }
        CpsWithdrawDO update = CpsWithdrawDO.builder().id(id).payTransferId(payTransferId)
                .transactionNo(String.valueOf(payTransferId)).build();
        if (withdrawMapper.updateByIdAndStatusVersion(update, withdraw.getStatusVersion(),
                List.of(CpsWithdrawStatusEnum.REVIEWING.getStatus())) != 1) {
            CpsWithdrawDO concurrent = requireWithdraw(id);
            if (!Objects.equals(concurrent.getPayTransferId(), payTransferId)) {
                throw new IllegalStateException("提现单并发绑定了其他 Pay 转账单");
            }
            return concurrent;
        }
        return requireWithdraw(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void scheduleRetry(Long id, String failure) {
        CpsWithdrawDO withdraw = withdrawMapper.selectById(id);
        if (withdraw == null || !CpsWithdrawStatusEnum.REVIEWING.getStatus().equals(withdraw.getStatus())) return;
        CpsWithdrawDO update = CpsWithdrawDO.builder().id(id).transferError(limit(failure))
                .retryCount((withdraw.getRetryCount() == null ? 0 : withdraw.getRetryCount()) + 1)
                .nextRetryTime(LocalDateTime.now().plusMinutes(1)).build();
        withdrawMapper.updateByIdAndStatusVersion(update, withdraw.getStatusVersion(),
                List.of(CpsWithdrawStatusEnum.REVIEWING.getStatus()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void completeSuccess(Long id, Long payTransferId) {
        CpsWithdrawDO withdraw = requireWithdraw(id);
        if (CpsWithdrawStatusEnum.SUCCESS.getStatus().equals(withdraw.getStatus())) return;
        validateTransferBinding(withdraw, payTransferId);
        String key = "withdraw-deduct:" + id;
        assetService.confirmWithdrawalDeduct(withdraw.getFreezeRecordId(), key,
                CpsAssetOperatorContext.system(key, "提现转账成功确认扣减"));
        CpsWithdrawDO update = CpsWithdrawDO.builder().id(id).status(CpsWithdrawStatusEnum.SUCCESS.getStatus())
                .transferStatus(TRANSFER_SUCCESS).transferTime(LocalDateTime.now()).nextRetryTime(null)
                .transferError(null).build();
        if (withdrawMapper.updateByIdAndStatusVersion(update, withdraw.getStatusVersion(),
                List.of(CpsWithdrawStatusEnum.REVIEWING.getStatus())) != 1) {
            throw new IllegalStateException("提现成功状态并发更新失败");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void completeFailure(Long id, Long payTransferId, String failure) {
        CpsWithdrawDO withdraw = requireWithdraw(id);
        if (CpsWithdrawStatusEnum.FAILED.getStatus().equals(withdraw.getStatus())) return;
        validateTransferBinding(withdraw, payTransferId);
        String key = "withdraw-unfreeze:" + id;
        assetService.unfreezeWithdrawalAsset(withdraw.getFreezeRecordId(), key,
                CpsAssetOperatorContext.system(key, "提现转账明确关闭，退回冻结资金"));
        CpsWithdrawDO update = CpsWithdrawDO.builder().id(id).status(CpsWithdrawStatusEnum.FAILED.getStatus())
                .transferStatus(TRANSFER_FAILED).transferError(limit(failure)).nextRetryTime(null).build();
        if (withdrawMapper.updateByIdAndStatusVersion(update, withdraw.getStatusVersion(),
                List.of(CpsWithdrawStatusEnum.REVIEWING.getStatus())) != 1) {
            throw new IllegalStateException("提现失败状态并发更新失败");
        }
    }

    private CpsWithdrawDO requireWithdraw(Long id) {
        CpsWithdrawDO withdraw = withdrawMapper.selectById(id);
        if (withdraw == null) throw exception(WITHDRAW_NOT_EXISTS);
        return withdraw;
    }

    private CpsWithdrawDO validateReplay(CpsWithdrawDO withdraw, CpsWithdrawCreateCommand command) {
        if (!command.memberId().equals(withdraw.getMemberId())
                || !Long.valueOf(command.amountCent()).equals(withdraw.getAmountCent())
                || !Objects.equals(command.withdrawType(), withdraw.getWithdrawType())
                || !Objects.equals(command.withdrawAccount(), withdraw.getWithdrawAccount())
                || !Objects.equals(command.withdrawAccountName(), withdraw.getWithdrawAccountName())) {
            throw new IllegalStateException("幂等键已用于其他提现申请");
        }
        return withdraw;
    }

    private void validateTransferBinding(CpsWithdrawDO withdraw, Long payTransferId) {
        if (withdraw.getPayTransferId() == null || !withdraw.getPayTransferId().equals(payTransferId)) {
            throw new IllegalStateException("提现单与 Pay 转账单不匹配");
        }
    }

    private boolean isTerminal(String status) {
        return CpsWithdrawStatusEnum.SUCCESS.getStatus().equals(status)
                || CpsWithdrawStatusEnum.FAILED.getStatus().equals(status)
                || CpsWithdrawStatusEnum.REJECTED.getStatus().equals(status);
    }

    static String channelCode(String withdrawType) {
        return switch (withdrawType) {
            case "alipay" -> "alipay_pc";
            case "wechat" -> "wx_lite";
            default -> throw new IllegalArgumentException("不支持的提现类型: " + withdrawType);
        };
    }

    private String limit(String text) {
        if (text == null) return "unknown";
        return text.length() <= 500 ? text : text.substring(0, 500);
    }
}
