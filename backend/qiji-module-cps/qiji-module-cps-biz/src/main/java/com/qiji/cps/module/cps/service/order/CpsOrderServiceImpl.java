package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderPageResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderPaginationMode;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderAttributionLogDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderStatusEventDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncLogDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderAttributionLogMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderStatusEventMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncLogMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import com.qiji.cps.module.cps.enums.CpsRebateTypeEnum;
import com.qiji.cps.module.cps.service.order.status.CpsPlatformOrderStatusMapper;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
import com.qiji.cps.module.cps.service.rebate.CpsRebateConfigService;
import com.qiji.cps.module.member.api.user.MemberUserApi;
import com.qiji.cps.module.member.api.user.dto.MemberUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ORDER_ATTRIBUTION_BIND_INVALID;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ORDER_NOT_EXISTS;

/**
 * CPS 订单 Service 实现类
 *
 * @author CPS System
 */
@Slf4j
@Service
@Validated
public class CpsOrderServiceImpl implements CpsOrderService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int ORDER_QUERY_WINDOW_HOURS = 3;
    private static final int ORDER_QUERY_PAGE_SIZE = 50;
    // Dataoke accepts pageNo up to 100.  Stopping at 20 silently dropped
    // orders from busy three-hour windows while still reporting the sync as
    // successful, which made historical compensation look complete but lose
    // data.  Exhaust the documented range and fail loudly if more remains.
    private static final int ORDER_QUERY_MAX_PAGES = 100;

    @Resource
    private CpsOrderMapper orderMapper;

    @Resource
    private CpsOrderAttributionLogMapper attributionLogMapper;

    @Resource
    private CpsOrderStatusEventMapper statusEventMapper;

    @Resource
    private CpsRebateRecordMapper rebateRecordMapper;

    @Resource
    private CpsOrderSyncLogMapper syncLogMapper;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @Resource
    private CpsRebateSettleService rebateSettleService;

    @Resource
    private CpsRebateConfigService rebateConfigService;

    @Resource
    private CpsTransferRecordMapper transferRecordMapper;

    @Resource
    private CpsAdzoneMapper adzoneMapper;

    @Resource
    private MemberUserApi memberUserApi;

    // ==================== 订单查询 ====================

    @Override
    public CpsOrderDO getOrder(Long id) {
        CpsOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw exception(ORDER_NOT_EXISTS);
        }
        enrichOrderMembers(List.of(order));
        return order;
    }

    @Override
    public void deleteOrder(Long id) {
        validateOrderExists(id);
        orderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrderList(List<Long> ids) {
        for (Long id : ids) {
            deleteOrder(id);
        }
    }

    @Override
    public PageResult<CpsOrderDO> getOrderPage(CpsOrderPageReqVO pageReqVO) {
        fillMemberIdsForNicknameSearch(pageReqVO);
        PageResult<CpsOrderDO> pageResult = orderMapper.selectPage(pageReqVO);
        enrichOrderMembers(pageResult.getList());
        return pageResult;
    }

    @Override
    public PageResult<CpsOrderDO> getMemberOrderPage(CpsOrderPageReqVO pageReqVO, Long memberId) {
        pageReqVO.setMemberId(null);
        pageReqVO.setMemberName(null);
        pageReqVO.setMemberIds(null);
        return orderMapper.selectPageByMemberId(pageReqVO, memberId);
    }

    @Override
    public CpsOrderDO getMemberOrder(Long memberId, Long id) {
        CpsOrderDO order = orderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getMemberId(), memberId)) {
            throw exception(ORDER_NOT_EXISTS);
        }
        return order;
    }

    @Override
    public CpsOrderDO getOrderByPlatformOrderId(String platformCode, String platformOrderId) {
        return orderMapper.selectByPlatformOrderId(platformCode, platformOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)
    public void bindSpecialIdToMember(Long orderId, Long memberId) {
        bindSpecialIdToMember(new CpsOrderManualBindCommand(
                orderId, memberId, SecurityFrameworkUtils.getLoginUserId(), null, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)
    public void bindSpecialIdToMember(CpsOrderManualBindCommand command) {
        if (command == null) {
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "绑定请求不能为空");
        }
        String idempotencyKey = command.idempotencyKey();
        if (!isBlank(idempotencyKey) && attributionLogMapper.selectByIdempotencyKey(idempotencyKey) != null) {
            return;
        }
        Long memberId = command.memberId();
        CpsOrderDO order = getOrder(command.orderId());
        if (memberId == null) {
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "会员ID不能为空");
        }
        if (memberId <= 0) {
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "会员ID必须为正数");
        }
        if (isBlank(order.getPlatformCode()) || isBlank(order.getSpecialId())) {
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "订单缺少 special_id，不能手动绑定会员");
        }
        if (isBlank(order.getAdzoneId())) {
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "订单缺少推广位ID，不能建立 special_id 绑定关系");
        }
        if (!Objects.equals(order.getMemberId(), memberId) && hasRebateAssetActivity(order)) {
            appendManualAttributionLog(order, memberId, "REBIND", "REJECTED",
                    "订单已产生返利资产活动，必须通过冲正和重新结算流程改绑",
                    idempotencyKey, "PENDING_COMPENSATION", command.auditNote(), command.operatorId());
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "订单已产生返利资产活动，不能直接改绑会员");
        }

        MemberUserRespDTO member = requireMemberForBind(memberId);
        CpsAdzoneDO existing = adzoneMapper.selectBySpecialId(order.getPlatformCode(), order.getSpecialId());
        if (isConflictingManualBind(existing, memberId)) {
            appendManualAttributionLog(order, memberId, existing.getRelationId(),
                    order.getMemberId() == null || Objects.equals(order.getMemberId(), memberId) ? "MANUAL" : "REBIND",
                    "CONFLICT", "special_id 已绑定到其他会员，需人工复核后走冲正/重算或保留原归因",
                    idempotencyKey, "PENDING_REVIEW", command.auditNote(), command.operatorId());
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "special_id 已绑定到其他会员，需人工复核");
        }
        if (existing == null) {
            adzoneMapper.insert(CpsAdzoneDO.builder()
                    .platformCode(order.getPlatformCode())
                    .adzoneId(order.getAdzoneId())
                    .adzoneName("手动绑定 special_id " + order.getSpecialId())
                    .adzoneType("member")
                    .relationType("member")
                    .relationId(memberId)
                    .externalSpecialId(order.getSpecialId())
                    .isDefault(0)
                    .status(1)
                    .build());
        } else {
            adzoneMapper.updateById(CpsAdzoneDO.builder()
                    .id(existing.getId())
                    .adzoneType("member")
                    .relationType("member")
                    .relationId(memberId)
                    .externalSpecialId(order.getSpecialId())
                    .status(1)
                    .build());
        }

        orderMapper.updateById(CpsOrderDO.builder()
                .id(order.getId())
                .memberId(memberId)
                .memberNickname(member.getNickname())
                .attributionSource("specialId")
                .estimateRebate(calculateEstimateRebate(
                        order.getCommissionAmount(), order.getPlatformCode(), memberId, member))
                .build());
        appendManualAttributionLog(order, memberId,
                order.getMemberId() == null || Objects.equals(order.getMemberId(), memberId) ? "MANUAL" : "REBIND",
                "BOUND", null, idempotencyKey, "APPROVED", command.auditNote(), command.operatorId());
    }

    /**
     * 手动归属未归因订单。此流程不建立 special_id 绑定关系，适用于平台没有返回可用
     * special_id/adzone 的订单；仅允许 member_id 为空的订单，避免覆盖自动归因结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)
    public void manuallyAttributeOrder(CpsOrderManualBindCommand command) {
        if (command == null) {
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "归因请求不能为空");
        }
        String idempotencyKey = command.idempotencyKey();
        if (!isBlank(idempotencyKey) && attributionLogMapper.selectByIdempotencyKey(idempotencyKey) != null) {
            return;
        }
        Long memberId = command.memberId();
        if (memberId == null || memberId <= 0) {
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "会员ID必须为正数");
        }
        CpsOrderDO order = getOrder(command.orderId());
        if (order.getMemberId() != null) {
            if (Objects.equals(order.getMemberId(), memberId)) {
                // 重复提交但幂等键变化时也记录一次人工操作，避免误报为成功改绑。
                appendManualAttributionLog(order, memberId, memberId, "MANUAL", "ALREADY_BOUND",
                        "订单已归属于该会员", idempotencyKey, "APPROVED", command.auditNote(), command.operatorId(),
                        "manual", null);
                return;
            }
            boolean assetAffected = hasRebateAssetActivity(order);
            String reason = assetAffected
                    ? "订单已产生返利资产活动，必须通过冲正和重新结算流程改绑"
                    : "订单已归因，不能通过未归因订单入口直接改绑会员";
            appendManualAttributionLog(order, memberId, order.getMemberId(), "REBIND", "REJECTED",
                    reason, idempotencyKey, assetAffected ? "PENDING_COMPENSATION" : "PENDING_REVIEW",
                    command.auditNote(), command.operatorId(), "manual", null);
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, reason);
        }

        MemberUserRespDTO member = requireMemberForBind(memberId);
        BigDecimal estimateRebate = calculateEstimateRebate(order.getCommissionAmount(),
                order.getPlatformCode(), memberId, member);
        int updated = orderMapper.bindMemberIfUnattributed(order.getId(), memberId, member.getNickname(),
                estimateRebate, "manual");
        if (updated != 1) {
            // 并发自动归因已抢先完成时，禁止覆盖并给调用方明确结果。
            CpsOrderDO current = orderMapper.selectById(order.getId());
            if (current != null && Objects.equals(current.getMemberId(), memberId)) {
                return;
            }
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "订单已被其他归因操作处理");
        }
        appendManualAttributionLog(order, memberId, memberId, "MANUAL", "BOUND", null,
                idempotencyKey, "APPROVED", command.auditNote(), command.operatorId(), "manual", null);
    }

    // ==================== 订单保存/更新 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveOrUpdateOrder(CpsOrderDTO orderDTO) {
        if (orderDTO == null || orderDTO.getPlatformCode() == null || orderDTO.getPlatformOrderId() == null) {
            return 0;
        }
        normalizeOrderAmountSnapshot(orderDTO);

        CpsOrderDO existing = orderMapper.selectByPlatformOrderId(
                orderDTO.getPlatformCode(), orderDTO.getPlatformOrderId());
        boolean restoredDeletedOrder = false;
        if (existing == null) {
            // 订单采用逻辑删除，唯一键仍占用原平台订单号；同步时必须恢复原行，
            // 不能走 insert，否则会触发唯一键冲突并丢失历史审计/返利关联。
            existing = orderMapper.selectDeletedByPlatformOrderId(
                    orderDTO.getPlatformCode(), orderDTO.getPlatformOrderId());
            // 查询方法本身限定 deleted=1，不依赖 JDBC BIT 到 Boolean 的类型转换结果。
            if (existing != null) {
                int restored = orderMapper.restoreDeletedById(existing.getId());
                if (restored == 0) {
                    // 并发同步可能已经先恢复了该订单；重新读取活动行后继续 CAS 更新。
                    CpsOrderDO activeOrder = orderMapper.selectByPlatformOrderId(
                            orderDTO.getPlatformCode(), orderDTO.getPlatformOrderId());
                    if (activeOrder == null) {
                        throw new IllegalStateException("订单恢复失败: " + existing.getId());
                    }
                    existing = activeOrder;
                } else {
                    restoredDeletedOrder = true;
                }
            }
        }
        if (existing == null) {
            // 新订单：插入
            AttributionResult attribution = resolveAttribution(orderDTO);
            CpsOrderDO newOrder = convertToOrderDO(orderDTO, attribution.memberId());
            if (newOrder.getMemberId() == null && attribution.memberId() != null) {
                newOrder.setMemberId(attribution.memberId());
                newOrder.setAttributionSource(attribution.source());
            }
            fillMemberNickname(newOrder);
            newOrder.setSyncTime(LocalDateTime.now());
            newOrder.setRetryCount(0);
            newOrder.setRawPlatformStatusSummary(buildRawStatusSummary(orderDTO));
            orderMapper.insert(newOrder);
            appendStatusEvent(newOrder.getId(), orderDTO, null, newOrder.getOrderStatus(),
                    newOrder.getOrderStatus(), 0, false, null);
            appendAutomaticAttributionLog(newOrder.getId(), orderDTO, attribution);
            closeTransferRecordLoop(attribution, orderDTO.getPlatformOrderId());
            log.debug("[saveOrUpdateOrder] 新增订单: platform={}, orderId={}",
                    orderDTO.getPlatformCode(), orderDTO.getPlatformOrderId());
            return 1;
        } else {
            // 已有订单：判断是否需要更新
            String incomingStatus = mapPlatformStatus(orderDTO);
            String newStatus = resolveNextOrderStatus(existing, orderDTO);
            String downgradeRejectReason = resolveDowngradeRejectReason(existing, incomingStatus, newStatus);
            AttributionResult attribution = existing.getMemberId() == null
                    ? resolveAttribution(orderDTO) : AttributionResult.empty();
            Long estimateMemberId = existing.getMemberId() != null ? existing.getMemberId() : attribution.memberId();
            boolean shouldFillMemberNickname = existing.getMemberId() != null && isBlank(existing.getMemberNickname());
            if (Objects.equals(existing.getOrderStatus(), newStatus)
                    && !hasOrderSnapshotChanged(existing, orderDTO)
                    && attribution.memberId() == null
                    && !shouldFillMemberNickname
                    && !restoredDeletedOrder) {
                if (downgradeRejectReason != null) {
                    appendStatusEvent(existing.getId(), orderDTO, existing.getOrderStatus(), incomingStatus,
                            existing.getOrderStatus(), existing.getStatusVersion(), true, downgradeRejectReason);
                }
                // 状态和订单快照均无变化，跳过
                return 0;
            }
            // 更新字段
            CpsOrderDO updateDO = CpsOrderDO.builder()
                    .id(existing.getId())
                    .parentOrderId(orderDTO.getParentOrderId())
                    .itemId(orderDTO.getItemId())
                    .itemTitle(orderDTO.getItemTitle())
                    .itemPic(orderDTO.getItemPic())
                    .itemPrice(orderDTO.getItemPrice())
                    .finalPrice(orderDTO.getFinalPrice())
                    .couponAmount(orderDTO.getCouponAmount())
                    .orderStatus(newStatus)
                    .commissionRate(orderDTO.getCommissionRate())
                    .commissionAmount(orderDTO.getCommissionAmount())
                    .adzoneId(orderDTO.getAdzoneId())
                    .externalInfo(orderDTO.getExternalId())
                    .specialId(orderDTO.getSpecialId())
                    .relationId(orderDTO.getRelationId())
                    .orderScene(orderDTO.getOrderScene())
                    .syncTime(LocalDateTime.now())
                    .rawPlatformStatusSummary(buildRawStatusSummary(orderDTO))
                    .build();
            if (orderDTO.getCommissionAmount() != null) {
                updateDO.setEstimateRebate(calculateEstimateRebate(orderDTO, estimateMemberId));
            }
            Long attributedMemberId = attribution.memberId();
            if (existing.getMemberId() == null && attributedMemberId != null) {
                updateDO.setMemberId(attributedMemberId);
                updateDO.setMemberNickname(resolveMemberNickname(attributedMemberId));
                updateDO.setAttributionSource(attribution.source());
            } else if (existing.getMemberId() != null && isBlank(existing.getMemberNickname())) {
                updateDO.setMemberNickname(resolveMemberNickname(existing.getMemberId()));
            }
            // 收货时间
            if (orderDTO.getReceiveTime() != null && existing.getConfirmReceiptTime() == null) {
                updateDO.setConfirmReceiptTime(parseDateTime(orderDTO.getReceiveTime()));
            }
            // 结算时间
            if (orderDTO.getSettleTime() != null && existing.getSettleTime() == null) {
                updateDO.setSettleTime(parseDateTime(orderDTO.getSettleTime()));
            }
            // 退款标记
            if (Integer.valueOf(1).equals(orderDTO.getRefundTag())) {
                updateDO.setOrderStatus(CpsOrderStatusEnum.REFUNDED.getStatus());
                updateDO.setRefundTime(LocalDateTime.now());
            }
            if (shouldReverseRebate(existing, updateDO.getOrderStatus())) {
                boolean reversed = rebateSettleService.reverseRebate(existing.getId());
                if (!reversed) {
                    log.warn("[saveOrUpdateOrder] 订单状态已更新为退款/失效，但返利扣回未执行: orderId={}, oldStatus={}, newStatus={}",
                            existing.getId(), existing.getOrderStatus(), updateDO.getOrderStatus());
                }
            }
            int expectedStatusVersion = existing.getStatusVersion() == null ? 0 : existing.getStatusVersion();
            int updated = orderMapper.updateByIdAndStatusVersion(updateDO, expectedStatusVersion);
            if (updated == 0) {
                throw new IllegalStateException("订单状态并发更新失败: " + existing.getId());
            }
            if (!Objects.equals(existing.getOrderStatus(), updateDO.getOrderStatus()) || downgradeRejectReason != null) {
                appendStatusEvent(existing.getId(), orderDTO, existing.getOrderStatus(), incomingStatus,
                        updateDO.getOrderStatus(), expectedStatusVersion + 1, downgradeRejectReason != null,
                        downgradeRejectReason);
            }
            if (existing.getMemberId() == null) {
                appendAutomaticAttributionLog(existing.getId(), orderDTO, attribution);
            }
            if (existing.getMemberId() == null && attributedMemberId != null) {
                closeTransferRecordLoop(attribution, orderDTO.getPlatformOrderId());
            }
            log.debug("[saveOrUpdateOrder] 更新订单: platform={}, orderId={}, status={}",
                    orderDTO.getPlatformCode(), orderDTO.getPlatformOrderId(), newStatus);
            return 2;
        }
    }

    @Override
    public int[] batchSaveOrUpdateOrders(List<CpsOrderDTO> orderDTOs) {
        int newCount = 0, updateCount = 0, skipCount = 0;
        for (CpsOrderDTO dto : orderDTOs) {
            try {
                int result = saveOrUpdateOrder(dto);
                if (result == 1) newCount++;
                else if (result == 2) updateCount++;
                else skipCount++;
            } catch (Exception e) {
                log.error("[batchSaveOrUpdateOrders] 处理订单异常: orderId={}", dto.getPlatformOrderId(), e);
                skipCount++;
            }
        }
        return new int[]{newCount, updateCount, skipCount};
    }

    // ==================== 手动同步 ====================

    @Override
    public String manualSync(String platformCode, Integer hours) {
        return manualSync(platformCode, hours, 1);
    }

    @Override
    public String manualSync(String platformCode, Integer hours, Integer queryType) {
        int effectiveHours = (hours == null || hours <= 0) ? 2 : hours;
        int effectiveQueryType = normalizeQueryType(queryType);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(effectiveHours);
        return manualSync(platformCode, effectiveHours, effectiveQueryType, startTime, endTime);
    }

    @Override
    public String manualSync(String platformCode, Integer hours, Integer queryType,
                             LocalDateTime startTime, LocalDateTime endTime) {
        return manualSync(platformCode, null, hours, queryType, null, startTime, endTime);
    }

    @Override
    public String manualSync(String platformCode, String vendorCode, Integer hours, Integer queryType,
                             Integer orderStatus, LocalDateTime startTime, LocalDateTime endTime) {
        if (vendorCode != null && !vendorCode.isBlank()
                && (platformClientFactory.getVendorClient(vendorCode, platformCode) == null
                || platformClientFactory.getVendorConfig(vendorCode, platformCode) == null)) {
            throw new IllegalArgumentException("供应商未配置或不可用: " + vendorCode + "/" + platformCode);
        }
        return platformClientFactory.withVendorCode(vendorCode,
                () -> doManualSync(platformCode, hours, queryType, orderStatus, startTime, endTime));
    }

    private String doManualSync(String platformCode, Integer hours, Integer queryType, Integer orderStatus,
                                LocalDateTime startTime, LocalDateTime endTime) {
        int effectiveHours = (hours == null || hours <= 0) ? 2 : hours;
        int effectiveQueryType = normalizeQueryType(queryType);
        LocalDateTime effectiveEndTime = endTime == null ? LocalDateTime.now() : endTime;
        LocalDateTime effectiveStartTime = startTime == null
                ? effectiveEndTime.minusHours(effectiveHours) : startTime;
        if (effectiveStartTime.isAfter(effectiveEndTime)) {
            throw new IllegalArgumentException("同步起始时间不能晚于结束时间");
        }

        CpsOrderSyncLogDO syncLog = CpsOrderSyncLogDO.builder()
                .platformCode(platformCode)
                .syncType(2) // 全量补偿
                .queryType(effectiveQueryType)
                .queryStartTime(effectiveStartTime)
                .queryEndTime(effectiveEndTime)
                .syncStartTime(LocalDateTime.now())
                .build();

        long t0 = System.currentTimeMillis();
        int total = 0, newCount = 0, updateCount = 0, skipCount = 0;
        RuntimeException failure = null;
        try {
            CpsPlatformClient client = platformClientFactory.getRequiredClient(platformCode);

            List<CpsOrderDTO> orders = pullOrdersByWindow(platformCode, client, effectiveQueryType, orderStatus,
                    effectiveStartTime, effectiveEndTime);
            total = orders.size();
            int[] stats = batchSaveOrUpdateOrders(orders);
            newCount = stats[0];
            updateCount = stats[1];
            skipCount = stats[2];

            syncLog.setSyncStatus(1); // 成功
        } catch (Exception e) {
            log.error("[manualSync] 平台 {} 手动同步失败", platformCode, e);
            syncLog.setSyncStatus(2); // 失败
            syncLog.setErrorMsg(e.getMessage());
            failure = e instanceof RuntimeException runtimeException ? runtimeException
                    : new IllegalStateException("订单同步失败", e);
        } finally {
            long cost = System.currentTimeMillis() - t0;
            syncLog.setSyncEndTime(LocalDateTime.now());
            syncLog.setCostMs(cost);
            syncLog.setTotalCount(total);
            syncLog.setNewCount(newCount);
            syncLog.setUpdateCount(updateCount);
            syncLog.setSkipCount(skipCount);
            syncLogMapper.insert(syncLog);
        }
        if (failure != null) {
            throw failure;
        }

        return String.format("平台[%s] 手动同步完成: 共%d条，新增%d，更新%d，跳过%d",
                platformCode, total, newCount, updateCount, skipCount);
    }

    private List<CpsOrderDTO> pullOrdersByWindow(String platformCode, CpsPlatformClient client, int queryType,
                                                  Integer orderStatus,
                                                  LocalDateTime startTime, LocalDateTime endTime) {
        List<CpsOrderDTO> allOrders = new ArrayList<>();
        LocalDateTime windowStart = startTime;
        while (windowStart.isBefore(endTime)) {
            LocalDateTime windowEnd = min(windowStart.plusHours(ORDER_QUERY_WINDOW_HOURS), endTime);
            for (Integer orderScene : resolveOrderScenes(platformCode)) {
                allOrders.addAll(pullAllOrderPages(client, queryType, orderStatus, orderScene, windowStart, windowEnd));
            }
            windowStart = windowEnd;
        }
        return allOrders;
    }

    private List<CpsOrderDTO> pullAllOrderPages(CpsPlatformClient client, int queryType, Integer orderStatus,
                                                Integer orderScene,
                                                LocalDateTime startTime, LocalDateTime endTime) {
        List<CpsOrderDTO> allOrders = new ArrayList<>();
        String positionIndex = null;
        int pageNo = 1;
        boolean hasMore = false;
        for (int pageCount = 1; pageCount <= ORDER_QUERY_MAX_PAGES; pageCount++) {
            CpsOrderQueryRequest req = new CpsOrderQueryRequest();
            req.setQueryType(queryType);
            req.setOrderStatus(orderStatus);
            req.setOrderScene(orderScene);
            req.setStartTime(startTime.format(DTF));
            req.setEndTime(endTime.format(DTF));
            req.setPageSize(ORDER_QUERY_PAGE_SIZE);
            req.setPageNo(pageNo);
            if (positionIndex != null) {
                req.setPositionIndex(positionIndex);
            }

            CpsOrderPageResult pageResult = client.queryOrderPage(req);
            List<CpsOrderDTO> pageOrders = pageResult.getItems();
            if (pageOrders.isEmpty()) {
                break;
            }
            allOrders.addAll(pageOrders);

            hasMore = pageResult.isHasMore();
            if (!hasMore) {
                break;
            }
            if (pageResult.getPaginationMode() == CpsOrderPaginationMode.CURSOR) {
                String nextPositionIndex = pageResult.getNextCursor();
                if (nextPositionIndex == null || nextPositionIndex.equals(positionIndex)) {
                    throw new IllegalStateException("订单游标分页返回 hasMore=true 但未提供有效 nextCursor");
                }
                positionIndex = nextPositionIndex;
            } else {
                if (pageResult.getNextPageNo() == null || pageResult.getNextPageNo() <= pageNo) {
                    throw new IllegalStateException("订单页码分页返回 hasMore=true 但未提供有效 nextPageNo");
                }
                pageNo = pageResult.getNextPageNo();
            }
        }
        if (hasMore) {
            throw new IllegalStateException("订单分页超过最大页数 " + ORDER_QUERY_MAX_PAGES
                    + "，请缩小时间窗口后重试");
        }
        return allOrders;
    }

    private List<Integer> resolveOrderScenes(String platformCode) {
        if ("taobao".equalsIgnoreCase(platformCode)) {
            return List.of(1, 2, 3);
        }
        return Collections.singletonList(null);
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    // ==================== 私有辅助方法 ====================

    private int normalizeQueryType(Integer queryType) {
        if (queryType == null || queryType < 1 || queryType > 4) {
            return 1;
        }
        return queryType;
    }

    /**
     * 将平台 DTO 转换为 CpsOrderDO
     */
    private CpsOrderDO convertToOrderDO(CpsOrderDTO dto, Long memberId) {
        CpsOrderDO order = CpsOrderDO.builder()
                .platformCode(dto.getPlatformCode())
                .platformOrderId(dto.getPlatformOrderId())
                .parentOrderId(dto.getParentOrderId())
                .itemId(dto.getItemId())
                .itemTitle(dto.getItemTitle())
                .itemPic(dto.getItemPic())
                .itemPrice(dto.getItemPrice())
                .finalPrice(dto.getFinalPrice())
                .couponAmount(dto.getCouponAmount())
                .commissionRate(dto.getCommissionRate())
                .commissionAmount(dto.getCommissionAmount())
                .estimateRebate(calculateEstimateRebate(dto, memberId))
                .adzoneId(dto.getAdzoneId())
                .externalInfo(dto.getExternalId())
                .specialId(dto.getSpecialId())
                .relationId(dto.getRelationId())
                .orderScene(dto.getOrderScene())
                .orderStatus(mapPlatformStatus(dto))
                .build();

        // 时间字段
        if (dto.getOrderTime() != null) {
            order.setCreateTime(parseDateTime(dto.getOrderTime()));
        }
        if (dto.getReceiveTime() != null) {
            order.setConfirmReceiptTime(parseDateTime(dto.getReceiveTime()));
        }
        if (dto.getSettleTime() != null) {
            order.setSettleTime(parseDateTime(dto.getSettleTime()));
        }

        return order;
    }

    private boolean hasOrderSnapshotChanged(CpsOrderDO existing, CpsOrderDTO dto) {
        return changedIfPresent(existing.getParentOrderId(), dto.getParentOrderId())
                || changedIfPresent(existing.getItemId(), dto.getItemId())
                || changedIfPresent(existing.getItemTitle(), dto.getItemTitle())
                || changedIfPresent(existing.getItemPic(), dto.getItemPic())
                || amountChangedIfPresent(existing.getItemPrice(), dto.getItemPrice())
                || amountChangedIfPresent(existing.getFinalPrice(), dto.getFinalPrice())
                || amountChangedIfPresent(existing.getCouponAmount(), dto.getCouponAmount())
                || amountChangedIfPresent(existing.getCommissionRate(), dto.getCommissionRate())
                || amountChangedIfPresent(existing.getCommissionAmount(), dto.getCommissionAmount())
                || (dto.getCommissionAmount() != null
                        && amountChangedIfPresent(existing.getEstimateRebate(),
                        calculateEstimateRebate(dto, existing.getMemberId())))
                || changedIfPresent(existing.getAdzoneId(), dto.getAdzoneId())
                || changedIfPresent(existing.getExternalInfo(), dto.getExternalId())
                || changedIfPresent(existing.getSpecialId(), dto.getSpecialId())
                || changedIfPresent(existing.getRelationId(), dto.getRelationId())
                || integerChangedIfPresent(existing.getOrderScene(), dto.getOrderScene())
                || (existing.getMemberId() == null && hasTrustedAttributionCandidate(dto));
    }

    private boolean hasTrustedAttributionCandidate(CpsOrderDTO dto) {
        return !isBlank(dto.getSpecialId())
                || !isBlank(dto.getRelationId())
                || !isBlank(dto.getAdzoneId())
                || (!isBlank(dto.getPlatformCode()) && !isBlank(dto.getItemId()) && !isBlank(dto.getOrderTime()));
    }

    private boolean changedIfPresent(String existingValue, String incomingValue) {
        return incomingValue != null && !Objects.equals(existingValue, incomingValue);
    }

    private boolean amountChangedIfPresent(BigDecimal existingValue, BigDecimal incomingValue) {
        if (incomingValue == null) {
            return false;
        }
        if (existingValue == null) {
            return true;
        }
        return existingValue.compareTo(incomingValue) != 0;
    }

    private boolean integerChangedIfPresent(Integer existingValue, Integer incomingValue) {
        return incomingValue != null && !Objects.equals(existingValue, incomingValue);
    }

    private AttributionResult resolveAttribution(CpsOrderDTO dto) {
        Long specialIdMemberId = resolveMemberIdBySpecialId(dto);
        if (specialIdMemberId != null) {
            return AttributionResult.bound(specialIdMemberId, null, "specialId", dto.getSpecialId());
        }
        Long relationIdMemberId = resolveMemberIdByExternalRelationId(dto);
        if (relationIdMemberId != null) {
            return AttributionResult.bound(relationIdMemberId, null, "relationId", dto.getRelationId());
        }
        AttributionResult tokenAttribution = resolveByAttributionToken(dto);
        if (tokenAttribution != null) {
            return tokenAttribution;
        }
        Long adzoneMemberId = resolveMemberIdByAdzone(dto);
        if (adzoneMemberId != null) {
            return AttributionResult.bound(adzoneMemberId, null, "adzone", dto.getAdzoneId());
        }
        if (dto.getPlatformCode() == null || dto.getItemId() == null) {
            return AttributionResult.unattributed("平台或商品标识不完整，无法匹配可信绑定");
        }
        LocalDateTime orderTime = parseDateTime(dto.getOrderTime());
        if (orderTime == null) {
            return AttributionResult.unattributed("订单时间缺失，无法唯一匹配转链记录");
        }
        LocalDateTime startTime = orderTime.minusHours(24);
        LocalDateTime endTime = orderTime.plusMinutes(30);
        List<CpsTransferRecordDO> candidates = transferRecordMapper.selectAttributionCandidates(
                dto.getPlatformCode(), dto.getItemId(), dto.getAdzoneId(), startTime, endTime);
        if ((candidates == null || candidates.isEmpty()) && !isBlank(dto.getAdzoneId())) {
            candidates = transferRecordMapper.selectAttributionCandidates(
                    dto.getPlatformCode(), dto.getItemId(), null, startTime, endTime);
        }
        if (candidates == null || candidates.size() != 1) {
            if (candidates != null && candidates.size() > 1) {
                log.warn("[resolveAttribution] 订单存在多条转链候选，跳过兜底归因: platform={}, orderId={}, itemId={}, adzoneId={}, count={}",
                        dto.getPlatformCode(), dto.getPlatformOrderId(), dto.getItemId(), dto.getAdzoneId(), candidates.size());
            }
            return candidates != null && candidates.size() > 1
                    ? AttributionResult.conflict("存在多条有效转链候选，拒绝猜测归因")
                    : rawExternalIdRejection(dto);
        }
        CpsTransferRecordDO record = candidates.get(0);
        return record.getMemberId() == null
                ? AttributionResult.rejected("唯一转链记录未绑定会员")
                : AttributionResult.bound(record.getMemberId(), record.getId(), "transferRecord",
                        String.valueOf(record.getId()));
    }

    private AttributionResult rawExternalIdRejection(CpsOrderDTO dto) {
        if (!isBlank(dto.getExternalId()) && dto.getExternalId().matches("\\d+")) {
            return AttributionResult.rejected("数字 externalId 未经可信绑定，不得作为会员ID");
        }
        return AttributionResult.unattributed("未找到唯一且有效的可信归因绑定");
    }

    private AttributionResult resolveByAttributionToken(CpsOrderDTO dto) {
        boolean supportedSidVendor = "jutuike".equalsIgnoreCase(dto.getVendorCode())
                || "haodanku".equalsIgnoreCase(dto.getVendorCode())
                && "eleme".equalsIgnoreCase(dto.getPlatformCode());
        if (!supportedSidVendor || isBlank(dto.getPlatformCode()) || isBlank(dto.getExternalId())) {
            return null;
        }
        List<CpsTransferRecordDO> candidates = transferRecordMapper.selectValidAttributionTokenCandidates(
                dto.getVendorCode(), dto.getPlatformCode(), "SID", dto.getExternalId(), LocalDateTime.now());
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() > 1) {
            log.warn("[resolveByAttributionToken] 闪购 SID 存在多条有效候选，拒绝归因: orderId={}, count={}",
                    dto.getPlatformOrderId(), candidates.size());
            return new AttributionResult(null, null, "sid", dto.getExternalId(), "CONFLICT",
                    "同一闪购 SID 存在多条有效转链记录，拒绝猜测归因");
        }
        CpsTransferRecordDO record = candidates.get(0);
        return record.getMemberId() == null
                ? new AttributionResult(null, null, "sid", dto.getExternalId(), "REJECTED",
                "闪购 SID 转链记录未绑定会员")
                : AttributionResult.bound(record.getMemberId(), record.getId(), "sid", dto.getExternalId());
    }

    private Long resolveMemberIdBySpecialId(CpsOrderDTO dto) {
        if (isBlank(dto.getPlatformCode()) || isBlank(dto.getSpecialId())) {
            return null;
        }
        try {
            CpsAdzoneDO adzone = adzoneMapper.selectActiveMemberAdzoneBySpecialId(dto.getPlatformCode(), dto.getSpecialId());
            return adzone == null ? null : adzone.getRelationId();
        } catch (Exception e) {
            log.warn("[resolveMemberIdBySpecialId] 按淘宝会员运营ID归因失败: platform={}, specialId={}",
                    dto.getPlatformCode(), dto.getSpecialId(), e);
            return null;
        }
    }

    private Long resolveMemberIdByExternalRelationId(CpsOrderDTO dto) {
        if (isBlank(dto.getPlatformCode()) || isBlank(dto.getRelationId())) {
            return null;
        }
        try {
            CpsAdzoneDO adzone = adzoneMapper.selectActiveMemberAdzoneByExternalRelationId(
                    dto.getPlatformCode(), dto.getRelationId());
            return adzone == null ? null : adzone.getRelationId();
        } catch (Exception e) {
            log.warn("[resolveMemberIdByExternalRelationId] 按淘宝渠道关系ID归因失败: platform={}, relationId={}",
                    dto.getPlatformCode(), dto.getRelationId(), e);
            return null;
        }
    }

    private Long resolveMemberIdByAdzone(CpsOrderDTO dto) {
        if (isBlank(dto.getPlatformCode()) || isBlank(dto.getAdzoneId())) {
            return null;
        }
        try {
            CpsAdzoneDO adzone = adzoneMapper.selectActiveMemberAdzone(dto.getPlatformCode(), dto.getAdzoneId());
            return adzone == null ? null : adzone.getRelationId();
        } catch (Exception e) {
            log.warn("[resolveMemberIdByAdzone] 按用户专属推广位归因失败: platform={}, adzoneId={}",
                    dto.getPlatformCode(), dto.getAdzoneId(), e);
            return null;
        }
    }

    private void closeTransferRecordLoop(AttributionResult attribution, String platformOrderId) {
        if (attribution.transferRecordId() == null || platformOrderId == null) {
            return;
        }
        transferRecordMapper.updatePlatformOrderId(attribution.transferRecordId(), platformOrderId);
    }

    private boolean hasRebateAssetActivity(CpsOrderDO order) {
        return order.getRebateTime() != null
                || rebateRecordMapper.selectByOrderIdAndType(order.getId(), CpsRebateTypeEnum.REBATE.getType()) != null;
    }

    private boolean isConflictingManualBind(CpsAdzoneDO existing, Long targetMemberId) {
        return existing != null
                && existing.getRelationId() != null
                && !Objects.equals(existing.getRelationId(), targetMemberId);
    }

    private void appendAutomaticAttributionLog(Long orderId, CpsOrderDTO dto, AttributionResult attribution) {
        attributionLogMapper.insert(CpsOrderAttributionLogDO.builder()
                .orderId(orderId)
                .platformCode(dto.getPlatformCode())
                .platformOrderId(dto.getPlatformOrderId())
                .candidateMemberId(attribution.memberId())
                .attributedMemberId(attribution.memberId())
                .attributionSource(attribution.source())
                .bindingType(attribution.source())
                .bindingId(attribution.bindingId())
                .action("AUTO")
                .result(attribution.auditResult())
                .rejectReason(attribution.reason())
                .operatorType("SYSTEM")
                .operatorId("order-sync")
                .build());
    }

    private void appendManualAttributionLog(CpsOrderDO order, Long candidateMemberId, String action,
                                            String result, String rejectReason) {
        appendManualAttributionLog(order, candidateMemberId,
                "BOUND".equals(result) ? candidateMemberId : order.getMemberId(),
                action, result, rejectReason, null, null, null, SecurityFrameworkUtils.getLoginUserId());
    }

    private void appendManualAttributionLog(CpsOrderDO order, Long candidateMemberId, String action,
                                            String result, String rejectReason, String idempotencyKey,
                                            String reviewStatus, String reviewAuditNote, Long reviewOperatorId) {
        appendManualAttributionLog(order, candidateMemberId,
                "BOUND".equals(result) ? candidateMemberId : order.getMemberId(),
                action, result, rejectReason, idempotencyKey, reviewStatus, reviewAuditNote, reviewOperatorId);
    }

    private void appendManualAttributionLog(CpsOrderDO order, Long candidateMemberId, Long attributedMemberId,
                                            String action, String result, String rejectReason, String idempotencyKey,
                                            String reviewStatus, String reviewAuditNote, Long reviewOperatorId) {
        appendManualAttributionLog(order, candidateMemberId, attributedMemberId, action, result, rejectReason,
                idempotencyKey, reviewStatus, reviewAuditNote, reviewOperatorId,
                "specialId", order.getSpecialId());
    }

    private void appendManualAttributionLog(CpsOrderDO order, Long candidateMemberId, Long attributedMemberId,
                                            String action, String result, String rejectReason, String idempotencyKey,
                                            String reviewStatus, String reviewAuditNote, Long reviewOperatorId,
                                            String attributionSource, String bindingId) {
        attributionLogMapper.insert(CpsOrderAttributionLogDO.builder()
                .orderId(order.getId())
                .platformCode(order.getPlatformCode())
                .platformOrderId(order.getPlatformOrderId())
                .candidateMemberId(candidateMemberId)
                .attributedMemberId(attributedMemberId)
                .attributionSource(attributionSource)
                .bindingType(attributionSource)
                .bindingId(bindingId)
                .action(action)
                .result(result)
                .rejectReason(rejectReason)
                .operatorType("ADMIN")
                .operatorId(Objects.toString(reviewOperatorId, null))
                .idempotencyKey(idempotencyKey)
                .reviewStatus(reviewStatus)
                .reviewAuditNote(reviewAuditNote)
                .reviewOperatorId(reviewOperatorId)
                .reviewTime(reviewStatus == null ? null : LocalDateTime.now())
                .build());
    }

    private void appendStatusEvent(Long orderId, CpsOrderDTO dto, String previousStatus, String mappedStatus,
                                   String currentStatus, Integer statusVersion, boolean downgradeRejected,
                                   String rejectReason) {
        statusEventMapper.insert(CpsOrderStatusEventDO.builder()
                .orderId(orderId)
                .platformCode(dto.getPlatformCode())
                .platformOrderId(dto.getPlatformOrderId())
                .sourceType(resolveStatusEventSourceType(dto))
                .sourceBatchNo(resolveStatusEventBatchNo(dto))
                .rawStatus(Objects.toString(dto.getPlatformStatus(), null))
                .rawStatusSummary(buildRawStatusSummary(dto))
                .previousStatus(previousStatus)
                .mappedStatus(mappedStatus)
                .currentStatus(currentStatus)
                .eventTime(LocalDateTime.now())
                .statusVersion(statusVersion == null ? 0 : statusVersion)
                .downgradeRejected(downgradeRejected)
                .rejectReason(truncate(rejectReason, 512))
                .build());
    }

    private String resolveStatusEventSourceType(CpsOrderDTO dto) {
        return isBlank(dto.getSyncBatchNo()) ? "MANUAL_SYNC" : "ORDER_SYNC";
    }

    private String resolveStatusEventBatchNo(CpsOrderDTO dto) {
        return isBlank(dto.getSyncBatchNo()) ? "manual-sync" : truncate(dto.getSyncBatchNo(), 128);
    }

    private String buildRawStatusSummary(CpsOrderDTO dto) {
        StringBuilder summary = new StringBuilder();
        summary.append("platformStatus=").append(dto.getPlatformStatus());
        summary.append(",refundTag=").append(dto.getRefundTag());
        if (dto.getOrderScene() != null) {
            summary.append(",orderScene=").append(dto.getOrderScene());
        }
        if (dto.getRawPayload() != null) {
            summary.append(",rawPayload=").append(dto.getRawPayload());
        }
        return truncate(summary.toString(), 512);
    }

    private String resolveDowngradeRejectReason(CpsOrderDO existing, String incomingStatus, String resolvedStatus) {
        if (existing == null || incomingStatus == null || Objects.equals(incomingStatus, resolvedStatus)) {
            return null;
        }
        if (Objects.equals(existing.getOrderStatus(), resolvedStatus)
                && !isReversalStatus(incomingStatus)
                && statusRank(incomingStatus) < statusRank(existing.getOrderStatus())) {
            return "拒绝降级: " + existing.getOrderStatus() + " -> " + incomingStatus;
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record AttributionResult(Long memberId, Long transferRecordId, String source, String bindingId,
                                     String auditResult, String reason) {
        private static AttributionResult empty() {
            return unattributed("无需重新归因");
        }

        private static AttributionResult bound(Long memberId, Long transferRecordId, String source, String bindingId) {
            return new AttributionResult(memberId, transferRecordId, source, bindingId, "BOUND", null);
        }

        private static AttributionResult rejected(String reason) {
            return new AttributionResult(null, null, null, null, "REJECTED", reason);
        }

        private static AttributionResult conflict(String reason) {
            return new AttributionResult(null, null, null, null, "CONFLICT", reason);
        }

        private static AttributionResult unattributed(String reason) {
            return new AttributionResult(null, null, null, null, "UNATTRIBUTED", reason);
        }
    }

    private void fillMemberIdsForNicknameSearch(CpsOrderPageReqVO pageReqVO) {
        if (isBlank(pageReqVO.getMemberName())) {
            return;
        }
        pageReqVO.setMemberIds(findMemberIdsByNickname(pageReqVO.getMemberName()));
    }

    private void validateOrderExists(Long id) {
        if (orderMapper.selectById(id) == null) {
            throw exception(ORDER_NOT_EXISTS);
        }
    }

    private List<Long> findMemberIdsByNickname(String memberName) {
        try {
            List<MemberUserRespDTO> users = memberUserApi.getUserListByNickname(memberName);
            if (users == null || users.isEmpty()) {
                return Collections.emptyList();
            }
            return users.stream().map(MemberUserRespDTO::getId).filter(Objects::nonNull).toList();
        } catch (Exception e) {
            log.warn("[findMemberIdsByNickname] 按会员名查询会员失败: memberName={}", memberName, e);
            return Collections.emptyList();
        }
    }

    private void fillMemberNickname(CpsOrderDO order) {
        if (order.getMemberId() != null && isBlank(order.getMemberNickname())) {
            order.setMemberNickname(resolveMemberNickname(order.getMemberId()));
        }
    }

    private void enrichOrderMembers(Collection<CpsOrderDO> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        Set<Long> memberIds = orders.stream()
                .filter(order -> order.getMemberId() != null && isBlank(order.getMemberNickname()))
                .map(CpsOrderDO::getMemberId)
                .collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return;
        }
        try {
            Map<Long, MemberUserRespDTO> userMap = memberUserApi.getUserMap(memberIds);
            if (userMap == null || userMap.isEmpty()) {
                return;
            }
            orders.forEach(order -> {
                MemberUserRespDTO user = userMap.get(order.getMemberId());
                if (user != null && !isBlank(user.getNickname())) {
                    order.setMemberNickname(user.getNickname());
                }
            });
        } catch (Exception e) {
            log.warn("[enrichOrderMembers] 补充订单会员昵称失败: memberIds={}", memberIds, e);
        }
    }

    private String resolveMemberNickname(Long memberId) {
        if (memberId == null) {
            return null;
        }
        try {
            MemberUserRespDTO user = memberUserApi.getUser(memberId);
            return user == null ? null : user.getNickname();
        } catch (Exception e) {
            log.warn("[resolveMemberNickname] 获取会员昵称失败: memberId={}", memberId, e);
            return null;
        }
    }

    private MemberUserRespDTO requireMemberForBind(Long memberId) {
        try {
            MemberUserRespDTO user = memberUserApi.getUser(memberId);
            if (user == null) {
                throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "会员不存在");
            }
            return user;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[requireMemberForBind] 校验会员失败: memberId={}", memberId, e);
            throw exception(ORDER_ATTRIBUTION_BIND_INVALID, "会员校验失败");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String mapPlatformStatus(CpsOrderDTO dto) {
        return CpsPlatformOrderStatusMapper.mapStatus(dto);
    }

    private String resolveNextOrderStatus(CpsOrderDO existing, CpsOrderDTO dto) {
        String incomingStatus = mapPlatformStatus(dto);
        String currentStatus = existing.getOrderStatus();
        if (isRollbackProtectedTerminalStatus(currentStatus)) {
            return isReversalStatus(incomingStatus) ? incomingStatus : currentStatus;
        }
        if (isReversalStatus(incomingStatus)) {
            return incomingStatus;
        }
        if (statusRank(incomingStatus) < statusRank(currentStatus)) {
            return currentStatus;
        }
        return incomingStatus;
    }

    private boolean shouldReverseRebate(CpsOrderDO existing, String nextStatus) {
        if (!isReversalStatus(nextStatus) || existing.getId() == null) {
            return false;
        }
        if (isReversalStatus(existing.getOrderStatus())) {
            return false;
        }
        if (existing.getRebateTime() != null
                || CpsOrderStatusEnum.REBATE_RECEIVED.getStatus().equals(existing.getOrderStatus())) {
            return true;
        }
        // V2 在平台结算后先创建冻结返利，此时订单仍是 SETTLED 且 rebateTime 为空。
        // 只要返利主记录已经创建，退款/失效就必须进入统一资产冲正，避免冻结到期后继续解冻。
        return rebateRecordMapper.selectByOrderIdAndType(
                existing.getId(), CpsRebateTypeEnum.REBATE.getType()) != null;
    }

    private boolean isRollbackProtectedTerminalStatus(String status) {
        return CpsOrderStatusEnum.REBATE_RECEIVED.getStatus().equals(status)
                || isReversalStatus(status);
    }

    private boolean isReversalStatus(String status) {
        return CpsOrderStatusEnum.REFUNDED.getStatus().equals(status)
                || CpsOrderStatusEnum.INVALID.getStatus().equals(status);
    }

    private int statusRank(String status) {
        if (status == null) {
            return -1;
        }
        if (CpsOrderStatusEnum.CREATED.getStatus().equals(status)) {
            return 0;
        }
        if (CpsOrderStatusEnum.PAID.getStatus().equals(status)) {
            return 1;
        }
        if (CpsOrderStatusEnum.RECEIVED.getStatus().equals(status)) {
            return 2;
        }
        if (CpsOrderStatusEnum.SETTLED.getStatus().equals(status)) {
            return 3;
        }
        if (CpsOrderStatusEnum.REBATE_RECEIVED.getStatus().equals(status)) {
            return 4;
        }
        if (isReversalStatus(status)) {
            return 5;
        }
        return -1;
    }

    /**
     * 使用与实际结算相同的六级规则引擎估算返利；没有可信会员或规则时不做猜测。
     */
    private BigDecimal calculateEstimateRebate(CpsOrderDTO dto, Long memberId) {
        if (dto.getCommissionAmount() == null || memberId == null) {
            return BigDecimal.ZERO;
        }
        MemberUserRespDTO member = memberUserApi.getUser(memberId);
        return calculateEstimateRebate(
                dto.getCommissionAmount(), dto.getPlatformCode(), memberId, member);
    }

    private BigDecimal calculateEstimateRebate(BigDecimal commissionAmount, String platformCode,
                                                Long memberId, MemberUserRespDTO member) {
        if (commissionAmount == null || memberId == null || member == null) {
            return BigDecimal.ZERO;
        }
        CpsRebateConfigDO config = rebateConfigService.matchRebateConfig(memberId, member.getLevelId(), platformCode);
        if (config == null || config.getRebateRate() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal estimate = commissionAmount.multiply(config.getRebateRate())
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        if (config.getMinRebateAmount() != null && config.getMinRebateAmount().signum() > 0
                && estimate.compareTo(config.getMinRebateAmount()) < 0) {
            estimate = config.getMinRebateAmount();
        }
        if (config.getMaxRebateAmount() != null && config.getMaxRebateAmount().signum() > 0
                && estimate.compareTo(config.getMaxRebateAmount()) > 0) {
            estimate = config.getMaxRebateAmount();
        }
        return estimate.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void normalizeOrderAmountSnapshot(CpsOrderDTO dto) {
        BigDecimal itemPrice = dto.getItemPrice();
        BigDecimal finalPrice = dto.getFinalPrice();
        BigDecimal couponAmount = dto.getCouponAmount();

        if (!isPositive(itemPrice) && isPositive(finalPrice) && isPositive(couponAmount)) {
            itemPrice = finalPrice.add(couponAmount).setScale(2, java.math.RoundingMode.HALF_UP);
            dto.setItemPrice(itemPrice);
        }
        if (!isPositive(finalPrice) && isPositive(itemPrice) && isPositive(couponAmount)) {
            BigDecimal derivedFinalPrice = itemPrice.subtract(couponAmount);
            if (derivedFinalPrice.signum() >= 0) {
                finalPrice = derivedFinalPrice.setScale(2, java.math.RoundingMode.HALF_UP);
                dto.setFinalPrice(finalPrice);
            }
        }
        if (!isPositive(couponAmount) && isPositive(itemPrice) && isPositive(finalPrice)
                && itemPrice.compareTo(finalPrice) > 0) {
            dto.setCouponAmount(itemPrice.subtract(finalPrice).setScale(2, java.math.RoundingMode.HALF_UP));
        }
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "--".equals(dateStr.trim())) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DTF);
        } catch (Exception e) {
            log.warn("[parseDateTime] 时间格式解析失败: {}", dateStr);
            return null;
        }
    }

}
