package com.qiji.cps.module.cps.service.withdraw;

import java.util.List;

public interface CpsWithdrawCompensationService {
    List<Long> getDueWithdrawIds(int limit);
    void compensate(Long withdrawId);
}
