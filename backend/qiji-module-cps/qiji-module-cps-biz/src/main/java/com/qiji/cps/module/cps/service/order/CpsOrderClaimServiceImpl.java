package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderAttributionLogDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderAttributionLogMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.enums.CpsRebateTypeEnum;
import com.qiji.cps.module.member.api.user.MemberUserApi;
import com.qiji.cps.module.member.api.user.dto.MemberUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ORDER_CLAIM_INVALID;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ORDER_CLAIM_NOT_EXISTS;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ORDER_CLAIM_STATUS_INVALID;

@Slf4j
@Service
@Validated
public class CpsOrderClaimServiceImpl implements CpsOrderClaimService {

    private static final String PENDING_REVIEW = "PENDING_REVIEW";

    @Resource
    private CpsOrderMapper orderMapper;
    @Resource
    private CpsOrderAttributionLogMapper attributionLogMapper;
    @Resource
    private CpsTransferRecordMapper transferRecordMapper;
    @Resource
    private CpsAdzoneMapper adzoneMapper;
    @Resource
    private CpsRebateRecordMapper rebateRecordMapper;
    @Resource
    private CpsFreezeRecordMapper freezeRecordMapper;
    @Resource
    private MemberUserApi memberUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsOrderClaimResult claim(CpsOrderClaimCommand command) {
        validateClaim(command);
        CpsOrderAttributionLogDO idempotent = attributionLogMapper.selectByIdempotencyKey(command.idempotencyKey());
        if (idempotent != null) {
            if (!Objects.equals(idempotent.getCandidateMemberId(), command.memberId())) {
                throw exception(ORDER_CLAIM_INVALID, "幂等键已被其他请求占用");
            }
            return toResult(idempotent);
        }
        CpsOrderAttributionLogDO previous = attributionLogMapper.selectLatestClaimByMemberAndOrder(
                command.memberId(), command.platformCode(), command.platformOrderId());
        if (previous != null && (PENDING_REVIEW.equals(previous.getReviewStatus())
                || "PENDING_SYNC".equals(previous.getReviewStatus()))) {
            return toResult(previous);
        }

        MemberUserRespDTO member = requireMember(command.memberId());
        CpsOrderDO located = orderMapper.selectByPlatformOrderId(command.platformCode(), command.platformOrderId());
        if (located == null) {
            CpsOrderAttributionLogDO claim = newClaimLog(command, null, "UNATTRIBUTED", "PENDING_SYNC",
                    "本地尚未同步到该订单，已记录并等待订单同步");
            attributionLogMapper.insert(claim);
            return result(claim, "PENDING_SYNC", "订单尚未同步，系统将在订单入库后继续处理");
        }

        CpsOrderDO order = orderMapper.selectForUpdateById(located.getId());
        if (order == null) {
            throw exception(ORDER_CLAIM_NOT_EXISTS);
        }
        if (Objects.equals(order.getMemberId(), command.memberId())) {
            CpsOrderAttributionLogDO claim = newClaimLog(command, order, "BOUND", "APPROVED", null);
            claim.setAttributedMemberId(command.memberId());
            attributionLogMapper.insert(claim);
            return result(claim, "APPROVED", "订单已归属于当前账号");
        }
        if (order.getMemberId() != null) {
            CpsOrderAttributionLogDO claim = newClaimLog(command, order, "CONFLICT", "CONFLICT",
                    "订单已有可信归属，不能重复申领");
            attributionLogMapper.insert(claim);
            return result(claim, "CONFLICT", "订单已有归属或存在申领冲突，请提交客服复核");
        }

        Evidence evidence = resolveEvidence(order);
        if (evidence != null && !Objects.equals(evidence.memberId(), command.memberId())) {
            CpsOrderAttributionLogDO claim = newClaimLog(command, order, "CONFLICT", "CONFLICT",
                    "订单可信归因证据与当前账号不一致");
            claim.setAttributionSource(evidence.source());
            claim.setBindingType(evidence.source());
            claim.setBindingId(evidence.bindingId());
            attributionLogMapper.insert(claim);
            return result(claim, "CONFLICT", "订单归因证据与当前账号不一致，请提交客服复核");
        }
        if (evidence == null) {
            CpsOrderAttributionLogDO claim = newClaimLog(command, order, "UNATTRIBUTED", PENDING_REVIEW,
                    "订单号只能定位订单，缺少可信会员归因证据");
            attributionLogMapper.insert(claim);
            return result(claim, PENDING_REVIEW, "已提交人工审核，审核通过前不会产生返利资产");
        }
        if (hasAssetActivity(order)) {
            CpsOrderAttributionLogDO claim = newClaimLog(command, order, "REJECTED", "ASSET_LOCKED",
                    "订单已有返利资产活动，禁止直接绑定");
            attributionLogMapper.insert(claim);
            return result(claim, "ASSET_LOCKED", "订单已有资金记录，需要人工冲正后处理");
        }

        CpsOrderAttributionLogDO claim = newClaimLog(command, order, "BOUND", "APPROVED", null);
        claim.setAttributedMemberId(command.memberId());
        claim.setAttributionSource(evidence.source());
        claim.setBindingType(evidence.source());
        claim.setBindingId(evidence.bindingId());
        attributionLogMapper.insert(claim);
        int updated = orderMapper.bindMemberIfUnattributed(order.getId(), command.memberId(), member.getNickname(),
                evidence.source());
        if (updated != 1) {
            appendDecision(claim, "CONFLICT", "CONFLICT", "并发申领导致订单归属已变化", null);
            return result(claim, "CONFLICT", "订单归属已发生变化，请刷新后查看");
        }
        if (evidence.transferRecordId() != null) {
            transferRecordMapper.updatePlatformOrderId(evidence.transferRecordId(), order.getPlatformOrderId());
        }
        appendDecision(claim, "CLAIM_AUTO_APPROVED", "APPROVED", null, command.memberId());
        return result(claim, "APPROVED", "订单归因证据验证通过，已绑定当前账号");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsOrderClaimResult review(CpsOrderClaimReviewCommand command) {
        if (command == null || command.claimId() == null || command.operatorId() == null || isBlank(command.auditNote())) {
            throw exception(ORDER_CLAIM_INVALID, "申领ID、审核人和审核说明不能为空");
        }
        CpsOrderAttributionLogDO claim = attributionLogMapper.selectForUpdateById(command.claimId());
        if (claim == null) {
            throw exception(ORDER_CLAIM_NOT_EXISTS);
        }
        if (!"CLAIM".equals(claim.getAction()) || !PENDING_REVIEW.equals(claim.getReviewStatus())) {
            throw exception(ORDER_CLAIM_STATUS_INVALID);
        }
        if (!command.approved()) {
            updateReview(claim, "REJECTED", command.auditNote(), command.operatorId());
            appendDecision(claim, "REJECTED", "REJECTED", command.auditNote(), null);
            return result(claim, "REJECTED", "申领已拒绝");
        }

        CpsOrderDO order = orderMapper.selectForUpdateById(claim.getOrderId());
        if (order == null) {
            throw exception(ORDER_CLAIM_NOT_EXISTS);
        }
        if (order.getMemberId() != null && !Objects.equals(order.getMemberId(), claim.getCandidateMemberId())) {
            updateReview(claim, "CONFLICT", command.auditNote(), command.operatorId());
            appendDecision(claim, "CONFLICT", "CONFLICT", "订单已由其他流程完成归因", null);
            return result(claim, "CONFLICT", "订单归属已发生变化，审核未生效");
        }
        if (hasAssetActivity(order)) {
            String reason = "订单已有返利资产活动，禁止直接绑定";
            updateReview(claim, "REJECTED", reason, command.operatorId());
            appendDecision(claim, "REJECTED", "REJECTED", reason, null);
            return result(claim, "ASSET_LOCKED", "订单已有资金记录，不能直接绑定");
        }
        MemberUserRespDTO member = requireMember(claim.getCandidateMemberId());
        if (order.getMemberId() == null && orderMapper.bindMemberIfUnattributed(order.getId(),
                claim.getCandidateMemberId(), member.getNickname(), "manualClaim") != 1) {
            updateReview(claim, "CONFLICT", command.auditNote(), command.operatorId());
            appendDecision(claim, "CONFLICT", "CONFLICT", "并发审核导致订单归属已变化", null);
            return result(claim, "CONFLICT", "订单归属已发生变化，审核未生效");
        }
        updateReview(claim, "APPROVED", command.auditNote(), command.operatorId());
        appendDecision(claim, "APPROVED", "APPROVED", null, claim.getCandidateMemberId());
        return result(claim, "APPROVED", "申领审核通过，订单已绑定会员");
    }

    @Override
    public List<CpsOrderClaimResult> getMemberClaims(Long memberId, int limit) {
        if (memberId == null) {
            throw exception(ORDER_CLAIM_INVALID, "会员不能为空");
        }
        List<CpsOrderAttributionLogDO> claims = attributionLogMapper.selectClaimsByMemberId(memberId, limit);
        if (claims == null) {
            return Collections.emptyList();
        }
        return claims.stream().map(this::toResult).toList();
    }

    private Evidence resolveEvidence(CpsOrderDO order) {
        try {
            if (!isBlank(order.getSpecialId())) {
                CpsAdzoneDO adzone = adzoneMapper.selectActiveMemberAdzoneBySpecialId(
                        order.getPlatformCode(), order.getSpecialId());
                if (adzone != null && adzone.getRelationId() != null) {
                    return new Evidence(adzone.getRelationId(), "specialId", order.getSpecialId(), null);
                }
            }
            if (!isBlank(order.getRelationId())) {
                CpsAdzoneDO adzone = adzoneMapper.selectActiveMemberAdzoneByExternalRelationId(
                        order.getPlatformCode(), order.getRelationId());
                if (adzone != null && adzone.getRelationId() != null) {
                    return new Evidence(adzone.getRelationId(), "relationId", order.getRelationId(), null);
                }
            }
            if ("eleme".equalsIgnoreCase(order.getPlatformCode()) && !isBlank(order.getExternalInfo())) {
                List<CpsTransferRecordDO> records = transferRecordMapper.selectValidAttributionTokenCandidates(
                        "haodanku", "eleme", "SID", order.getExternalInfo(), LocalDateTime.now());
                if (records != null && records.size() == 1 && records.get(0).getMemberId() != null) {
                    CpsTransferRecordDO record = records.get(0);
                    return new Evidence(record.getMemberId(), "sid", order.getExternalInfo(), record.getId());
                }
            }
            if (!isBlank(order.getAdzoneId())) {
                CpsAdzoneDO adzone = adzoneMapper.selectActiveMemberAdzone(
                        order.getPlatformCode(), order.getAdzoneId());
                if (adzone != null && adzone.getRelationId() != null) {
                    return new Evidence(adzone.getRelationId(), "adzone", order.getAdzoneId(), null);
                }
            }
        } catch (Exception ex) {
            log.warn("[resolveEvidence] 订单申领可信证据查询失败: orderId={}", order.getId(), ex);
        }
        return null;
    }

    private boolean hasAssetActivity(CpsOrderDO order) {
        return order.getRebateTime() != null
                || positive(order.getRealRebate())
                || !isBlank(order.getRebateFreezeStatus())
                || rebateRecordMapper.selectByOrderIdAndType(order.getId(), CpsRebateTypeEnum.REBATE.getType()) != null
                || freezeRecordMapper.selectByBusinessId("ORDER_REBATE", String.valueOf(order.getId())) != null;
    }

    private CpsOrderAttributionLogDO newClaimLog(CpsOrderClaimCommand command, CpsOrderDO order,
                                                  String result, String reviewStatus, String reason) {
        return CpsOrderAttributionLogDO.builder()
                .orderId(order == null ? null : order.getId())
                .platformCode(command.platformCode())
                .platformOrderId(command.platformOrderId())
                .candidateMemberId(command.memberId())
                .attributedMemberId(order == null ? null : order.getMemberId())
                .action("CLAIM")
                .result(result)
                .rejectReason(reason)
                .operatorType("MEMBER")
                .operatorId(String.valueOf(command.memberId()))
                .idempotencyKey(command.idempotencyKey())
                .reviewStatus(reviewStatus)
                .build();
    }

    private void appendDecision(CpsOrderAttributionLogDO claim, String action, String reviewStatus,
                                String reason, Long attributedMemberId) {
        attributionLogMapper.insert(CpsOrderAttributionLogDO.builder()
                .orderId(claim.getOrderId())
                .platformCode(claim.getPlatformCode())
                .platformOrderId(claim.getPlatformOrderId())
                .candidateMemberId(claim.getCandidateMemberId())
                .attributedMemberId(attributedMemberId)
                .attributionSource(claim.getAttributionSource())
                .bindingType(claim.getBindingType())
                .bindingId(claim.getBindingId())
                .action(action)
                .result("APPROVED".equals(reviewStatus) ? "BOUND" : reviewStatus)
                .rejectReason(reason)
                .operatorType(action.startsWith("CLAIM_AUTO") ? "SYSTEM" : "ADMIN")
                .operatorId(action.startsWith("CLAIM_AUTO") ? "order-claim" : Objects.toString(claim.getReviewOperatorId(), null))
                .reviewStatus(reviewStatus)
                .reviewAuditNote(reason)
                .reviewOperatorId(claim.getReviewOperatorId())
                .reviewTime(LocalDateTime.now())
                .build());
    }

    private void updateReview(CpsOrderAttributionLogDO claim, String status, String note, Long operatorId) {
        attributionLogMapper.updateClaimReview(claim.getId(), status, note, operatorId);
        claim.setReviewStatus(status);
        claim.setReviewAuditNote(note);
        claim.setReviewOperatorId(operatorId);
    }

    private CpsOrderClaimResult toResult(CpsOrderAttributionLogDO claim) {
        String status = !isBlank(claim.getReviewStatus()) ? claim.getReviewStatus() : claim.getResult();
        String message = switch (status) {
            case "APPROVED" -> "订单已绑定当前账号";
            case "PENDING_REVIEW" -> "申领正在等待人工审核";
            case "PENDING_SYNC" -> "订单尚未同步，正在等待入库";
            case "CONFLICT" -> "订单已有归属或存在申领冲突";
            case "REJECTED" -> "订单申领未通过";
            default -> "订单申领状态已更新";
        };
        return result(claim, status, message);
    }

    private CpsOrderClaimResult result(CpsOrderAttributionLogDO claim, String status, String message) {
        return new CpsOrderClaimResult(claim.getId(), claim.getOrderId(), claim.getPlatformCode(),
                claim.getPlatformOrderId(), status, message);
    }

    private MemberUserRespDTO requireMember(Long memberId) {
        MemberUserRespDTO member = memberId == null ? null : memberUserApi.getUser(memberId);
        if (member == null) {
            throw exception(ORDER_CLAIM_INVALID, "会员不存在");
        }
        return member;
    }

    private void validateClaim(CpsOrderClaimCommand command) {
        if (command == null || command.memberId() == null || isBlank(command.platformCode())
                || isBlank(command.platformOrderId()) || isBlank(command.idempotencyKey())) {
            throw exception(ORDER_CLAIM_INVALID, "会员、平台、订单号和幂等键不能为空");
        }
        if (command.platformOrderId().length() > 128 || command.idempotencyKey().length() > 128) {
            throw exception(ORDER_CLAIM_INVALID, "订单号或幂等键长度超限");
        }
    }

    private boolean positive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Evidence(Long memberId, String source, String bindingId, Long transferRecordId) {
    }
}
