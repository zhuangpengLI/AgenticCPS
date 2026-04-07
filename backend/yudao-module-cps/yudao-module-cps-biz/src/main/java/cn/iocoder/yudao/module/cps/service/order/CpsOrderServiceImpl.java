package cn.iocoder.yudao.module.cps.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.cps.client.CpsPlatformClient;
import cn.iocoder.yudao.module.cps.client.CpsPlatformClientFactory;
import cn.iocoder.yudao.module.cps.client.dto.CpsOrderDTO;
import cn.iocoder.yudao.module.cps.client.dto.CpsOrderQueryRequest;
import cn.iocoder.yudao.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.order.CpsOrderDO;
import cn.iocoder.yudao.module.cps.dal.dataobject.order.CpsOrderSyncLogDO;
import cn.iocoder.yudao.module.cps.dal.mysql.order.CpsOrderMapper;
import cn.iocoder.yudao.module.cps.dal.mysql.order.CpsOrderSyncLogMapper;
import cn.iocoder.yudao.module.cps.enums.CpsOrderStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.cps.enums.CpsErrorCodeConstants.ORDER_NOT_EXISTS;

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

    @Resource
    private CpsOrderMapper orderMapper;

    @Resource
    private CpsOrderSyncLogMapper syncLogMapper;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    // ==================== 订单查询 ====================

    @Override
    public CpsOrderDO getOrder(Long id) {
        CpsOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw exception(ORDER_NOT_EXISTS);
        }
        return order;
    }

    @Override
    public PageResult<CpsOrderDO> getOrderPage(CpsOrderPageReqVO pageReqVO) {
        return orderMapper.selectPage(pageReqVO);
    }

    @Override
    public CpsOrderDO getOrderByPlatformOrderId(String platformOrderId) {
        return orderMapper.selectByPlatformOrderId(platformOrderId);
    }

    // ==================== 订单保存/更新 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveOrUpdateOrder(CpsOrderDTO orderDTO) {
        if (orderDTO == null || orderDTO.getPlatformOrderId() == null) {
            return 0;
        }

        CpsOrderDO existing = orderMapper.selectByPlatformOrderId(orderDTO.getPlatformOrderId());
        if (existing == null) {
            // 新订单：插入
            CpsOrderDO newOrder = convertToOrderDO(orderDTO);
            newOrder.setSyncTime(LocalDateTime.now());
            newOrder.setRetryCount(0);
            orderMapper.insert(newOrder);
            log.debug("[saveOrUpdateOrder] 新增订单: platform={}, orderId={}",
                    orderDTO.getPlatformCode(), orderDTO.getPlatformOrderId());
            return 1;
        } else {
            // 已有订单：判断是否需要更新
            String newStatus = mapPlatformStatus(orderDTO);
            if (Objects.equals(existing.getOrderStatus(), newStatus)
                    && Objects.equals(existing.getCommissionAmount(), orderDTO.getCommissionAmount())) {
                // 状态和佣金均无变化，跳过
                return 0;
            }
            // 更新字段
            CpsOrderDO updateDO = CpsOrderDO.builder()
                    .id(existing.getId())
                    .orderStatus(newStatus)
                    .commissionRate(orderDTO.getCommissionRate())
                    .commissionAmount(orderDTO.getCommissionAmount())
                    .syncTime(LocalDateTime.now())
                    .build();
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
            orderMapper.updateById(updateDO);
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
        int effectiveHours = (hours == null || hours <= 0) ? 2 : hours;
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(effectiveHours);

        CpsOrderSyncLogDO syncLog = CpsOrderSyncLogDO.builder()
                .platformCode(platformCode)
                .syncType(2) // 全量补偿
                .queryType(1)
                .queryStartTime(startTime)
                .queryEndTime(endTime)
                .syncStartTime(LocalDateTime.now())
                .build();

        long t0 = System.currentTimeMillis();
        int total = 0, newCount = 0, updateCount = 0, skipCount = 0;
        try {
            CpsPlatformClient client = platformClientFactory.getRequiredClient(platformCode);

            CpsOrderQueryRequest req = new CpsOrderQueryRequest();
            req.setQueryType(1);
            req.setStartTime(startTime.format(DTF));
            req.setEndTime(endTime.format(DTF));
            req.setPageSize(50);

            List<CpsOrderDTO> orders = client.queryOrders(req);
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

    // ==================== 私有辅助方法 ====================

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
                .orderStatus(mapPlatformStatus(dto))
                .build();

        // 归因会员：externalId 为会员ID字符串
        if (dto.getExternalId() != null) {
            try {
                order.setMemberId(Long.parseLong(dto.getExternalId()));
            } catch (NumberFormatException e) {
                log.warn("[convertToOrderDO] externalId 格式非会员ID: {}", dto.getExternalId());
            }
        }

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
