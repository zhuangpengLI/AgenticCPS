package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncLogDO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncLogMapper;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
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
    private static final int ORDER_QUERY_MAX_PAGES = 20;

    @Resource
    private CpsOrderMapper orderMapper;

    @Resource
    private CpsOrderSyncLogMapper syncLogMapper;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @Resource
    private CpsRebateSettleService rebateSettleService;

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
    public CpsOrderDO getOrderByPlatformOrderId(String platformOrderId) {
        return orderMapper.selectByPlatformOrderId(platformOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindSpecialIdToMember(Long orderId, Long memberId) {
        CpsOrderDO order = getOrder(orderId);
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

        MemberUserRespDTO member = requireMemberForBind(memberId);
        CpsAdzoneDO existing = adzoneMapper.selectBySpecialId(order.getPlatformCode(), order.getSpecialId());
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
                .build());
    }

    // ==================== 订单保存/更新 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveOrUpdateOrder(CpsOrderDTO orderDTO) {
        if (orderDTO == null || orderDTO.getPlatformOrderId() == null) {
            return 0;
        }
        normalizeOrderAmountSnapshot(orderDTO);

        CpsOrderDO existing = orderMapper.selectByPlatformOrderId(orderDTO.getPlatformOrderId());
        if (existing == null) {
            // 新订单：插入
            CpsOrderDO newOrder = convertToOrderDO(orderDTO);
            AttributionResult attribution = resolveAttribution(orderDTO);
            if (newOrder.getMemberId() == null && attribution.memberId() != null) {
                newOrder.setMemberId(attribution.memberId());
                newOrder.setAttributionSource(attribution.source());
            }
            fillMemberNickname(newOrder);
            newOrder.setSyncTime(LocalDateTime.now());
            newOrder.setRetryCount(0);
            orderMapper.insert(newOrder);
            closeTransferRecordLoop(attribution, orderDTO.getPlatformOrderId());
            log.debug("[saveOrUpdateOrder] 新增订单: platform={}, orderId={}",
                    orderDTO.getPlatformCode(), orderDTO.getPlatformOrderId());
            return 1;
        } else {
            // 已有订单：判断是否需要更新
            String newStatus = resolveNextOrderStatus(existing, orderDTO);
            AttributionResult attribution = existing.getMemberId() == null
                    ? resolveAttribution(orderDTO) : AttributionResult.empty();
            boolean shouldFillMemberNickname = existing.getMemberId() != null && isBlank(existing.getMemberNickname());
            if (Objects.equals(existing.getOrderStatus(), newStatus)
                    && !hasOrderSnapshotChanged(existing, orderDTO)
                    && attribution.memberId() == null
                    && !shouldFillMemberNickname) {
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
                    .build();
            if (orderDTO.getCommissionAmount() != null) {
                updateDO.setEstimateRebate(calculateEstimateRebate(orderDTO));
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
            orderMapper.updateById(updateDO);
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

        CpsOrderSyncLogDO syncLog = CpsOrderSyncLogDO.builder()
                .platformCode(platformCode)
                .syncType(2) // 全量补偿
                .queryType(effectiveQueryType)
                .queryStartTime(startTime)
                .queryEndTime(endTime)
                .syncStartTime(LocalDateTime.now())
                .build();

        long t0 = System.currentTimeMillis();
        int total = 0, newCount = 0, updateCount = 0, skipCount = 0;
        try {
            CpsPlatformClient client = platformClientFactory.getRequiredClient(platformCode);

            List<CpsOrderDTO> orders = pullOrdersByWindow(platformCode, client, effectiveQueryType, startTime, endTime);
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

        return String.format("平台[%s] 手动同步完成: 共%d条，新增%d，更新%d，跳过%d",
                platformCode, total, newCount, updateCount, skipCount);
    }

    private List<CpsOrderDTO> pullOrdersByWindow(String platformCode, CpsPlatformClient client, int queryType,
                                                  LocalDateTime startTime, LocalDateTime endTime) {
        List<CpsOrderDTO> allOrders = new ArrayList<>();
        LocalDateTime windowStart = startTime;
        while (windowStart.isBefore(endTime)) {
            LocalDateTime windowEnd = min(windowStart.plusHours(ORDER_QUERY_WINDOW_HOURS), endTime);
            for (Integer orderScene : resolveOrderScenes(platformCode)) {
                allOrders.addAll(pullAllOrderPages(client, queryType, orderScene, windowStart, windowEnd));
            }
            windowStart = windowEnd;
        }
        return allOrders;
    }

    private List<CpsOrderDTO> pullAllOrderPages(CpsPlatformClient client, int queryType, Integer orderScene,
                                                LocalDateTime startTime, LocalDateTime endTime) {
        List<CpsOrderDTO> allOrders = new ArrayList<>();
        String positionIndex = null;
        for (int page = 1; page <= ORDER_QUERY_MAX_PAGES; page++) {
            CpsOrderQueryRequest req = new CpsOrderQueryRequest();
            req.setQueryType(queryType);
            req.setOrderScene(orderScene);
            req.setStartTime(startTime.format(DTF));
            req.setEndTime(endTime.format(DTF));
            req.setPageSize(ORDER_QUERY_PAGE_SIZE);
            req.setPageNo(page);
            if (positionIndex != null) {
                req.setPositionIndex(positionIndex);
            }

            List<CpsOrderDTO> pageOrders = client.queryOrders(req);
            if (pageOrders == null || pageOrders.isEmpty()) {
                break;
            }
            allOrders.addAll(pageOrders);

            String nextPositionIndex = pageOrders.get(pageOrders.size() - 1).getNextPositionIndex();
            if (nextPositionIndex == null || nextPositionIndex.equals(positionIndex)) {
                break;
            }
            positionIndex = nextPositionIndex;

            if (pageOrders.size() < ORDER_QUERY_PAGE_SIZE) {
                break;
            }
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
    private CpsOrderDO convertToOrderDO(CpsOrderDTO dto) {
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
                .estimateRebate(calculateEstimateRebate(dto))
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
                        && amountChangedIfPresent(existing.getEstimateRebate(), calculateEstimateRebate(dto)))
                || changedIfPresent(existing.getAdzoneId(), dto.getAdzoneId())
                || changedIfPresent(existing.getExternalInfo(), dto.getExternalId())
                || changedIfPresent(existing.getSpecialId(), dto.getSpecialId())
                || changedIfPresent(existing.getRelationId(), dto.getRelationId())
                || integerChangedIfPresent(existing.getOrderScene(), dto.getOrderScene())
                || (existing.getMemberId() == null && parseMemberId(dto.getExternalId()) != null);
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

    private Long parseMemberId(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(externalId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AttributionResult resolveAttribution(CpsOrderDTO dto) {
        Long specialIdMemberId = resolveMemberIdBySpecialId(dto);
        if (specialIdMemberId != null) {
            return new AttributionResult(specialIdMemberId, null, "specialId");
        }
        Long relationIdMemberId = resolveMemberIdByExternalRelationId(dto);
        if (relationIdMemberId != null) {
            return new AttributionResult(relationIdMemberId, null, "relationId");
        }
        Long externalMemberId = parseMemberId(dto.getExternalId());
        if (externalMemberId != null) {
            return new AttributionResult(externalMemberId, null, "externalId");
        }
        Long adzoneMemberId = resolveMemberIdByAdzone(dto);
        if (adzoneMemberId != null) {
            return new AttributionResult(adzoneMemberId, null, "adzone");
        }
        if (dto.getPlatformCode() == null || dto.getItemId() == null) {
            return AttributionResult.empty();
        }
        LocalDateTime orderTime = parseDateTime(dto.getOrderTime());
        if (orderTime == null) {
            return AttributionResult.empty();
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
            return AttributionResult.empty();
        }
        CpsTransferRecordDO record = candidates.get(0);
        return record.getMemberId() == null ? AttributionResult.empty()
                : new AttributionResult(record.getMemberId(), record.getId(), "transferRecord");
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

    private record AttributionResult(Long memberId, Long transferRecordId, String source) {
        private static AttributionResult empty() {
            return new AttributionResult(null, null, null);
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

    /**
     * 根据平台原始状态映射为系统订单状态
     *
     * <p>各平台状态码不同，适配器的 CpsOrderDTO.platformStatus 已做初步转换，
     * 此处基于 refundTag 和 platformStatus 做最终映射。</p>
     */
    private String mapPlatformStatus(CpsOrderDTO dto) {
        if (Integer.valueOf(1).equals(dto.getRefundTag())) {
            return CpsOrderStatusEnum.REFUNDED.getStatus();
        }
        if (dto.getPlatformStatus() == null) {
            return CpsOrderStatusEnum.CREATED.getStatus();
        }
        // 通用规则：0=已下单，1=已付款，2=已收货/确认，3=已结算，4=已到账，-1=失效
        return switch (dto.getPlatformStatus()) {
            case 0 -> CpsOrderStatusEnum.CREATED.getStatus();
            case 1 -> CpsOrderStatusEnum.PAID.getStatus();
            case 2 -> CpsOrderStatusEnum.RECEIVED.getStatus();
            case 3 -> CpsOrderStatusEnum.SETTLED.getStatus();
            case 4 -> CpsOrderStatusEnum.REBATE_RECEIVED.getStatus();
            case -1 -> CpsOrderStatusEnum.INVALID.getStatus();
            default -> CpsOrderStatusEnum.CREATED.getStatus();
        };
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
        return existing.getRebateTime() != null
                || CpsOrderStatusEnum.REBATE_RECEIVED.getStatus().equals(existing.getOrderStatus());
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
     * 估算返利金额（佣金 × 返利比例，此处简单取佣金的 80% 作为预估）
     *
     * <p>实际返利比例由 CpsRebateConfigService 解析，此处仅做预估存储。</p>
     */
    private BigDecimal calculateEstimateRebate(CpsOrderDTO dto) {
        if (dto.getCommissionAmount() == null) {
            return BigDecimal.ZERO;
        }
        // 默认 80% 佣金作为返利预估（实际由返利配置决定）
        return dto.getCommissionAmount().multiply(new BigDecimal("0.8")).setScale(2, java.math.RoundingMode.HALF_UP);
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
        if (dateStr == null || dateStr.isBlank()) {
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
