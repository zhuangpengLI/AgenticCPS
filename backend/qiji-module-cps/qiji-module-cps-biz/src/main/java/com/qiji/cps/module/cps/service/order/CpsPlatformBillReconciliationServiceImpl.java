package com.qiji.cps.module.cps.service.order;

import cn.hutool.core.util.StrUtil;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsPlatformBillDiffPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsPlatformBillDiffDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsPlatformBillRowDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsPlatformBillDiffMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsPlatformBillRowMapper;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Validated
public class CpsPlatformBillReconciliationServiceImpl implements CpsPlatformBillReconciliationService {

    static final String STATUS_PENDING = "PENDING";
    static final String STATUS_HANDLED = "HANDLED";
    static final String STATUS_REPULL_REQUESTED = "REPULL_REQUESTED";
    private static final String CONCLUSION_TARGETED_REPULL = "TARGETED_REPULL";

    @Resource
    private CpsPlatformBillRowMapper billRowMapper;
    @Resource
    private CpsPlatformBillDiffMapper billDiffMapper;
    @Resource
    private CpsOrderMapper orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsPlatformBillReconciliationResult importAndReconcile(List<CpsPlatformBillImportRowCommand> rows) {
        int importedRows = 0;
        int createdDiffs = 0;
        if (rows == null || rows.isEmpty()) {
            return new CpsPlatformBillReconciliationResult(0, 0);
        }
        for (CpsPlatformBillImportRowCommand row : rows) {
            if (!valid(row)) {
                continue;
            }
            String rowKey = rowKey(row);
            CpsPlatformBillRowDO billRow = billRowMapper.selectByIdempotencyKey(rowKey);
            if (billRow == null) {
                billRow = toBillRow(row, rowKey);
                billRowMapper.insert(billRow);
                importedRows++;
            }
            CpsOrderDO order = orderMapper.selectByPlatformOrderId(row.platformCode(), row.platformOrderId());
            createdDiffs += createDiffs(row, billRow, order);
        }
        return new CpsPlatformBillReconciliationResult(importedRows, createdDiffs);
    }

    @Override
    public PageResult<CpsPlatformBillDiffDO> getDiffPage(CpsPlatformBillDiffPageReqVO reqVO) {
        return billDiffMapper.selectPage(reqVO);
    }

    @Override
    public void handleDiff(Long id, Long operatorId, String conclusion, String auditNote) {
        CpsPlatformBillDiffDO diff = requireDiff(id);
        billDiffMapper.updateById(CpsPlatformBillDiffDO.builder()
                .id(id)
                .diffStatus(STATUS_HANDLED)
                .handleConclusion(truncate(conclusion, 64))
                .handleAuditNote(truncate(auditNote, 500))
                .handleOperatorId(operatorId)
                .handleTime(LocalDateTime.now())
                .version(diff.getVersion())
                .build());
    }

    @Override
    public void requestTargetedRepull(Long id, Long operatorId, String auditNote) {
        CpsPlatformBillDiffDO diff = requireDiff(id);
        billDiffMapper.updateById(CpsPlatformBillDiffDO.builder()
                .id(id)
                .diffStatus(STATUS_REPULL_REQUESTED)
                .handleConclusion(CONCLUSION_TARGETED_REPULL)
                .handleAuditNote(truncate(auditNote, 500))
                .handleOperatorId(operatorId)
                .handleTime(LocalDateTime.now())
                .version(diff.getVersion())
                .build());
    }

    private int createDiffs(CpsPlatformBillImportRowCommand row, CpsPlatformBillRowDO billRow, CpsOrderDO order) {
        if (order == null) {
            return insertDiff(row, billRow, null, "MISSING_ORDER",
                    "platform bill row has no local order");
        }
        int count = 0;
        if (!sameMoney(order.getCommissionAmount(), row.commissionAmount())) {
            count += insertDiff(row, billRow, order, "COMMISSION_DIFF",
                    "local order commission differs from platform bill");
        }
        if (billShowsRefund(row) && !localOrderIsReversed(order)) {
            count += insertDiff(row, billRow, order, "REFUND_DIFF",
                    "platform bill shows refund but local order is not reversed");
        }
        if (row.settleTime() != null && !Objects.equals(order.getSettleTime(), row.settleTime())) {
            count += insertDiff(row, billRow, order, "SETTLEMENT_TIME_DIFF",
                    "local settlement time differs from platform bill");
        }
        if (order.getMemberId() == null) {
            count += insertDiff(row, billRow, order, "UNATTRIBUTED_ORDER",
                    "local order has no trusted member attribution");
        }
        return count;
    }

    private int insertDiff(CpsPlatformBillImportRowCommand row, CpsPlatformBillRowDO billRow, CpsOrderDO order,
                           String diffType, String summary) {
        String key = diffKey(row, diffType);
        if (billDiffMapper.selectByIdempotencyKey(key) != null) {
            return 0;
        }
        billDiffMapper.insert(CpsPlatformBillDiffDO.builder()
                .billRowId(billRow.getId())
                .orderId(order == null ? null : order.getId())
                .platformCode(truncate(row.platformCode(), 32))
                .vendorCode(truncate(row.vendorCode(), 64))
                .billBatchNo(truncate(row.billBatchNo(), 128))
                .platformOrderId(truncate(row.platformOrderId(), 128))
                .diffType(diffType)
                .diffStatus(STATUS_PENDING)
                .diffSummary(summary)
                .orderCommissionAmount(order == null ? null : order.getCommissionAmount())
                .billCommissionAmount(row.commissionAmount())
                .billRefundAmount(row.refundAmount())
                .orderStatus(order == null ? null : order.getOrderStatus())
                .billStatus(truncate(row.billStatus(), 64))
                .orderSettleTime(order == null ? null : order.getSettleTime())
                .billSettleTime(row.settleTime())
                .idempotencyKey(key)
                .version(0)
                .build());
        return 1;
    }

    private CpsPlatformBillRowDO toBillRow(CpsPlatformBillImportRowCommand row, String rowKey) {
        return CpsPlatformBillRowDO.builder()
                .platformCode(truncate(row.platformCode(), 32))
                .vendorCode(truncate(row.vendorCode(), 64))
                .billBatchNo(truncate(row.billBatchNo(), 128))
                .platformOrderId(truncate(row.platformOrderId(), 128))
                .parentOrderId(truncate(row.parentOrderId(), 128))
                .billStatus(truncate(row.billStatus(), 64))
                .commissionAmount(row.commissionAmount())
                .refundAmount(row.refundAmount())
                .orderTime(row.orderTime())
                .settleTime(row.settleTime())
                .refundTime(row.refundTime())
                .sourceFileName(truncate(row.sourceFileName(), 255))
                .rawSummary(truncate(row.rawSummary(), 2000))
                .idempotencyKey(rowKey)
                .version(0)
                .build();
    }

    private boolean valid(CpsPlatformBillImportRowCommand row) {
        return row != null
                && StrUtil.isNotBlank(row.platformCode())
                && StrUtil.isNotBlank(row.billBatchNo())
                && StrUtil.isNotBlank(row.platformOrderId());
    }

    private String rowKey(CpsPlatformBillImportRowCommand row) {
        return "platform-bill:" + row.billBatchNo() + ":" + row.platformCode() + ":" + row.platformOrderId();
    }

    private String diffKey(CpsPlatformBillImportRowCommand row, String diffType) {
        return "platform-bill-diff:" + row.billBatchNo() + ":" + row.platformCode()
                + ":" + row.platformOrderId() + ":" + diffType;
    }

    private boolean sameMoney(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private boolean billShowsRefund(CpsPlatformBillImportRowCommand row) {
        return "REFUNDED".equalsIgnoreCase(row.billStatus())
                || "REFUND".equalsIgnoreCase(row.billStatus())
                || row.refundAmount() != null && row.refundAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean localOrderIsReversed(CpsOrderDO order) {
        return CpsOrderStatusEnum.REFUNDED.getStatus().equals(order.getOrderStatus())
                || CpsOrderStatusEnum.INVALID.getStatus().equals(order.getOrderStatus());
    }

    private CpsPlatformBillDiffDO requireDiff(Long id) {
        CpsPlatformBillDiffDO diff = billDiffMapper.selectById(id);
        if (diff == null) {
            throw new IllegalArgumentException("platform bill diff not found: " + id);
        }
        return diff;
    }

    private String truncate(String value, int maxLength) {
        return StrUtil.isBlank(value) ? value : StrUtil.subWithLength(value, 0, maxLength);
    }
}
