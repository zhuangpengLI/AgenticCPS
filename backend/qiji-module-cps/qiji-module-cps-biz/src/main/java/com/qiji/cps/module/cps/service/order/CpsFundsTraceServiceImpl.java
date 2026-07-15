package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderStatusEventDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsPlatformBillDiffDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateDebtDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderStatusEventMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsPlatformBillDiffMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetLedgerMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateDebtMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CpsFundsTraceServiceImpl implements CpsFundsTraceService {

    @Resource
    private CpsOrderMapper orderMapper;
    @Resource
    private CpsOrderStatusEventMapper statusEventMapper;
    @Resource
    private CpsRebateRecordMapper rebateRecordMapper;
    @Resource
    private CpsFreezeRecordMapper freezeRecordMapper;
    @Resource
    private CpsRebateDebtMapper debtMapper;
    @Resource
    private CpsRebateAssetLedgerMapper assetLedgerMapper;
    @Resource
    private CpsPlatformBillDiffMapper billDiffMapper;

    @Override
    public CpsFundsTraceResult traceFunds(CpsFundsTraceQuery query) {
        if (query == null || noTraceKey(query)) {
            throw new IllegalArgumentException("至少提供订单ID、平台订单号、业务单号或幂等键之一");
        }
        CpsOrderDO order = resolveOrder(query);
        Long orderId = order == null ? query.orderId() : order.getId();
        String platformOrderId = order == null ? query.platformOrderId() : order.getPlatformOrderId();

        List<CpsOrderStatusEventDO> statusEvents = statusEventMapper.selectListByTrace(orderId, platformOrderId);
        List<CpsRebateRecordDO> rebateRecords =
                rebateRecordMapper.selectListByTrace(orderId, platformOrderId, query.idempotencyKey());
        List<CpsFreezeRecordDO> freezeRecords =
                freezeRecordMapper.selectListByTrace(orderId, platformOrderId, query.businessId(), query.idempotencyKey());
        List<CpsRebateDebtDO> debtRecords =
                debtMapper.selectListByTrace(orderId, platformOrderId, query.businessId(), query.idempotencyKey());
        List<CpsRebateAssetLedgerDO> assetLedgers =
                assetLedgerMapper.selectListByTrace(orderId, platformOrderId, query.businessId(), query.idempotencyKey());
        List<CpsPlatformBillDiffDO> billDiffs =
                billDiffMapper.selectListByTrace(orderId, platformOrderId, query.idempotencyKey());

        List<String> warnings = buildWarnings(order, assetLedgers);
        return CpsFundsTraceResult.builder()
                .order(order)
                .statusEvents(statusEvents)
                .rebateRecords(rebateRecords)
                .freezeRecords(freezeRecords)
                .debtRecords(debtRecords)
                .assetLedgers(assetLedgers)
                .billDiffs(billDiffs)
                .traceWarnings(warnings)
                .traceComplete(order != null && !statusEvents.isEmpty() && warnings.isEmpty())
                .build();
    }

    private CpsOrderDO resolveOrder(CpsFundsTraceQuery query) {
        if (query.orderId() != null) {
            return orderMapper.selectById(query.orderId());
        }
        if (!isBlank(query.platformCode()) && !isBlank(query.platformOrderId())) {
            return orderMapper.selectByPlatformOrderId(query.platformCode(), query.platformOrderId());
        }
        return null;
    }

    private List<String> buildWarnings(CpsOrderDO order, List<CpsRebateAssetLedgerDO> assetLedgers) {
        List<String> warnings = new ArrayList<>();
        if (order == null) {
            warnings.add("未找到订单主记录");
        }
        for (CpsRebateAssetLedgerDO ledger : assetLedgers) {
            if (ledger.getOrderId() == null && isBlank(ledger.getBusinessId()) && isBlank(ledger.getIdempotencyKey())) {
                warnings.add("资产流水缺少订单、业务单号和幂等键: ledgerId=" + ledger.getId());
            }
            if (isBlank(ledger.getOperatorType()) || isBlank(ledger.getOperatorId())) {
                warnings.add("资产流水缺少操作主体: ledgerId=" + ledger.getId());
            }
        }
        return warnings;
    }

    private boolean noTraceKey(CpsFundsTraceQuery query) {
        return query.orderId() == null
                && isBlank(query.platformOrderId())
                && isBlank(query.businessId())
                && isBlank(query.idempotencyKey());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
