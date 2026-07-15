package com.qiji.cps.module.cps.service.exchange;

import java.util.List;

public interface CpsRebateTokenExchangeCompensationService {

    List<Long> getDueOrderIds(int limit);

    void compensate(Long orderId);
}
