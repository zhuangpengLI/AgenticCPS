package com.qiji.cps.module.cps.service.rebate;

import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 批量任务逐单事务边界，避免同类 self-invocation 绕过代理。
 */
@Service
public class CpsRebateSettleExecutor {

    private final CpsRebateSettleService settleService;

    public CpsRebateSettleExecutor(@Lazy CpsRebateSettleService settleService) {
        this.settleService = settleService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean settleOne(CpsOrderDO order) {
        return settleService.settleOrder(order);
    }
}
