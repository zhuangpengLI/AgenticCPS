package cn.iocoder.yudao.module.cps.service.rebate;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.cps.dal.dataobject.order.CpsOrderDO;
import cn.iocoder.yudao.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import cn.iocoder.yudao.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import cn.iocoder.yudao.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import cn.iocoder.yudao.module.cps.dal.mysql.order.CpsOrderMapper;
import cn.iocoder.yudao.module.cps.dal.mysql.rebate.CpsRebateAccountMapper;
import cn.iocoder.yudao.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import cn.iocoder.yudao.module.cps.enums.CpsOrderStatusEnum;
import cn.iocoder.yudao.module.cps.enums.CpsRebateStatusEnum;
import cn.iocoder.yudao.module.cps.enums.CpsRebateTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * CPS 返利结算 Service 实现类
 *
 * <p>核心流程：
 * <ol>
 *   <li>扫描已收货/已结算且未入账的订单</li>
 *   <li>按返利配置优先级计算应得返利金额</li>
 *   <li>写入返利记录（{@code yudao_cps_rebate_record}）</li>
 *   <li>更新订单状态为"已到账"，记录 rebate_time</li>
 *   <li>乐观锁更新返利账户余额（{@code yudao_cps_rebate_account}）</li>
 * </ol>
 * </p>
 *
 * @author CPS System
 */
@Slf4j
@Service
@Validated
public class CpsRebateSettleServiceImpl implements CpsRebateSettleService {

    /**
     * 待结算订单状态：已收货 或 已结算（平台已结算给联盟）
     */
    private static final List<String> PENDING_SETTLE_STATUSES = Arrays.asList(
            CpsOrderStatusEnum.RECEIVED.getStatus(),
            CpsOrderStatusEnum.SETTLED.getStatus()
    );

    @Resource
    private CpsOrderMapper orderMapper;

    @Resource
    private CpsRebateRecordMapper rebateRecordMapper;

    @Resource
    private CpsRebateAccountMapper rebateAccountMapper;

    @Resource
    private CpsRebateConfigService rebateConfigService;

    // ==================== 核心结算逻辑 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean settleOrder(CpsOrderDO order) {
        if (order.getMemberId() == null) {
            log.debug("[settleOrder] 订单无会员归因，跳过: orderId={}", order.getId());
            return false;
        }

        // 幂等检查：已有返利记录则跳过
        CpsRebateRecordDO existRecord = rebateRecordMapper.selectByOrderIdAndType(
                order.getId(), CpsRebateTypeEnum.REBATE.getType());
        if (existRecord != null) {
            log.debug("[settleOrder] 订单已结算过，跳过: orderId={}, recordId={}", order.getId(), existRecord.getId());
            return false;
        }

        // 1. 计算返利金额
        BigDecimal rebateAmount = calculateRebateAmount(order);
        if (rebateAmount == null || rebateAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[settleOrder] 返利金额计算为0，跳过结算: orderId={}", order.getId());
            return false;
        }

        // 2. 获取匹配的返利配置（用于记录比例）
        CpsRebateConfigDO config = rebateConfigService.matchRebateConfig(null, order.getPlatformCode());
        BigDecimal rebateRate = (config != null) ? config.getRebateRate() : BigDecimal.ZERO;

        // 3. 写入返利记录
        CpsRebateRecordDO record = CpsRebateRecordDO.builder()
                .memberId(order.getMemberId())
                .orderId(order.getId())
                .platformCode(order.getPlatformCode())
                .platformOrderId(order.getPlatformOrderId())
                .itemId(order.getItemId())
                .itemTitle(order.getItemTitle())
                .orderAmount(order.getFinalPrice() != null ? order.getFinalPrice() : BigDecimal.ZERO)
                .commissionAmount(order.getCommissionAmount() != null ? order.getCommissionAmount() : BigDecimal.ZERO)
                .rebateRate(rebateRate)
                .rebateAmount(rebateAmount)
                .rebateType(CpsRebateTypeEnum.REBATE.getType())
                .rebateStatus(CpsRebateStatusEnum.RECEIVED.getStatus())
                .remark("订单确认收货自动结算")
                .build();
        rebateRecordMapper.insert(record);

        // 4. 更新订单状态 → 已到账 + 记录 rebateTime
        CpsOrderDO updateOrder = CpsOrderDO.builder()
                .id(order.getId())
                .orderStatus(CpsOrderStatusEnum.REBATE_RECEIVED.getStatus())
                .realRebate(rebateAmount)
                .rebateTime(LocalDateTime.now())
                .build();
        orderMapper.updateById(updateOrder);

        // 5. 乐观锁更新返利账户余额
        updateRebateAccount(order.getMemberId(), rebateAmount);

        log.info("[settleOrder] 返利入账成功: orderId={}, memberId={}, rebateAmount={}",
                order.getId(), order.getMemberId(), rebateAmount);
        return true;
    }

    @Override
    public int[] batchSettle(int batchSize) {
        List<CpsOrderDO> orders = orderMapper.selectPendingSettleOrders(PENDING_SETTLE_STATUSES, batchSize);
        int successCount = 0, skipCount = 0, failCount = 0;

        for (CpsOrderDO order : orders) {
            try {
                boolean settled = settleOrder(order);
                if (settled) successCount++;
                else skipCount++;
            } catch (Exception e) {
                log.error("[batchSettle] 订单结算失败: orderId={}", order.getId(), e);
                failCount++;
            }
        }

        log.info("[batchSettle] 批量结算完成: 成功={}, 跳过={}, 失败={}", successCount, skipCount, failCount);
        return new int[]{successCount, skipCount, failCount};
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reverseRebate(Long orderId) {
        // 查找原始返利记录
        CpsRebateRecordDO origRecord = rebateRecordMapper.selectByOrderIdAndType(
                orderId, CpsRebateTypeEnum.REBATE.getType());
        if (origRecord == null || !CpsRebateStatusEnum.RECEIVED.getStatus().equals(origRecord.getRebateStatus())) {
            log.info("[reverseRebate] 无需扣回，订单未入账返利: orderId={}", orderId);
            return false;
        }

        // 写入扣回记录
        CpsRebateRecordDO reverseRecord = CpsRebateRecordDO.builder()
                .memberId(origRecord.getMemberId())
                .orderId(orderId)
                .platformCode(origRecord.getPlatformCode())
                .platformOrderId(origRecord.getPlatformOrderId())
                .itemId(origRecord.getItemId())
                .itemTitle(origRecord.getItemTitle())
                .orderAmount(origRecord.getOrderAmount())
                .commissionAmount(origRecord.getCommissionAmount())
                .rebateRate(origRecord.getRebateRate())
                .rebateAmount(origRecord.getRebateAmount().negate()) // 负值表示扣回
                .rebateType(CpsRebateTypeEnum.REFUND.getType())
                .rebateStatus(CpsRebateStatusEnum.REFUNDED.getStatus())
                .precedingRebateId(origRecord.getId())
                .remark("订单退款自动扣回返利")
                .build();
        rebateRecordMapper.insert(reverseRecord);

        // 原记录标记为已扣回
        CpsRebateRecordDO updateOrig = new CpsRebateRecordDO();
        updateOrig.setId(origRecord.getId());
        updateOrig.setRebateStatus(CpsRebateStatusEnum.REFUNDED.getStatus());
        rebateRecordMapper.updateById(updateOrig);

        // 扣减账户余额（available_balance - rebateAmount）
        deductRebateAccount(origRecord.getMemberId(), origRecord.getRebateAmount());

        log.info("[reverseRebate] 返利扣回成功: orderId={}, memberId={}, amount={}",
                orderId, origRecord.getMemberId(), origRecord.getRebateAmount());
        return true;
    }

    @Override
    public CpsRebateAccountDO getOrInitAccount(Long memberId) {
        CpsRebateAccountDO account = rebateAccountMapper.selectByMemberId(memberId);
        if (account == null) {
            account = CpsRebateAccountDO.builder()
                    .memberId(memberId)
                    .totalRebate(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .frozenBalance(BigDecimal.ZERO)
                    .withdrawnAmount(BigDecimal.ZERO)
                    .status(CommonStatusEnum.ENABLE.getStatus())
                    .version(0)
                    .build();
            rebateAccountMapper.insert(account);
            log.info("[getOrInitAccount] 初始化返利账户: memberId={}", memberId);
        }
        return account;
    }

    // ==================== 私有方法 ====================

    /**
     * 计算返利金额
     *
     * <p>返利 = 佣金金额 × 返利比例（按配置匹配优先级决定）</p>
     *
     * <p>注：若 commissionAmount 为空，则使用 finalPrice × 默认比例作为兜底计算。</p>
     */
    private BigDecimal calculateRebateAmount(CpsOrderDO order) {
        CpsRebateConfigDO config = rebateConfigService.matchRebateConfig(null, order.getPlatformCode());

        BigDecimal commission = order.getCommissionAmount();
        if (commission == null || commission.compareTo(BigDecimal.ZERO) <= 0) {
            // 佣金为0，无法结算
            return BigDecimal.ZERO;
        }

        BigDecimal rebateRate;
        if (config != null && config.getRebateRate() != null) {
            // 使用配置的返利比例（百分比，如 80.00 表示80%）
            rebateRate = config.getRebateRate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        } else {
            // 兜底：默认80%返利给用户
            rebateRate = new BigDecimal("0.80");
        }

        BigDecimal rebateAmount = commission.multiply(rebateRate).setScale(2, RoundingMode.HALF_UP);

        // 应用上下限
        if (config != null) {
            if (config.getMaxRebateAmount() != null && config.getMaxRebateAmount().compareTo(BigDecimal.ZERO) > 0) {
                rebateAmount = rebateAmount.min(config.getMaxRebateAmount());
            }
            if (config.getMinRebateAmount() != null && config.getMinRebateAmount().compareTo(BigDecimal.ZERO) > 0) {
                rebateAmount = rebateAmount.max(config.getMinRebateAmount());
            }
        }

        return rebateAmount;
    }

    /**
     * 乐观锁更新返利账户（入账）
     *
     * <p>失败时最多重试 3 次，防止并发冲突导致丢失。</p>
     */
    private void updateRebateAccount(Long memberId, BigDecimal rebateAmount) {
        for (int retry = 0; retry < 3; retry++) {
            CpsRebateAccountDO account = getOrInitAccount(memberId);
            CpsRebateAccountDO update = CpsRebateAccountDO.builder()
                    .id(account.getId())
                    .totalRebate(account.getTotalRebate().add(rebateAmount))
                    .availableBalance(account.getAvailableBalance().add(rebateAmount))
                    .version(account.getVersion())
                    .build();
            int rows = rebateAccountMapper.updateById(update);
            if (rows > 0) {
                return;
            }
            log.warn("[updateRebateAccount] 乐观锁冲突，重试: memberId={}, retry={}", memberId, retry + 1);
        }
        throw new RuntimeException("更新返利账户失败（乐观锁冲突），memberId=" + memberId);
    }

    /**
     * 乐观锁更新返利账户（扣回）
     */
    private void deductRebateAccount(Long memberId, BigDecimal rebateAmount) {
        for (int retry = 0; retry < 3; retry++) {
            CpsRebateAccountDO account = rebateAccountMapper.selectByMemberId(memberId);
            if (account == null) {
                log.warn("[deductRebateAccount] 账户不存在，跳过扣减: memberId={}", memberId);
                return;
            }
            BigDecimal newBalance = account.getAvailableBalance().subtract(rebateAmount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                newBalance = BigDecimal.ZERO; // 余额不足时归零
            }
            CpsRebateAccountDO update = CpsRebateAccountDO.builder()
                    .id(account.getId())
                    .availableBalance(newBalance)
                    .totalRebate(account.getTotalRebate().subtract(rebateAmount).max(BigDecimal.ZERO))
                    .version(account.getVersion())
                    .build();
            int rows = rebateAccountMapper.updateById(update);
            if (rows > 0) {
                return;
            }
            log.warn("[deductRebateAccount] 乐观锁冲突，重试: memberId={}, retry={}", memberId, retry + 1);
        }
        throw new RuntimeException("扣减返利账户失败（乐观锁冲突），memberId=" + memberId);
    }

}
