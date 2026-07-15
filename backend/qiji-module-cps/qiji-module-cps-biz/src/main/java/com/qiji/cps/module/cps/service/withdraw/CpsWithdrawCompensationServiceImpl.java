package com.qiji.cps.module.cps.service.withdraw;

import com.qiji.cps.module.cps.dal.mysql.withdraw.CpsWithdrawMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CpsWithdrawCompensationServiceImpl implements CpsWithdrawCompensationService {
    @Resource private CpsWithdrawMapper withdrawMapper;
    @Resource private CpsWithdrawTransferExecutor transferExecutor;

    @Override
    public List<Long> getDueWithdrawIds(int limit) {
        return withdrawMapper.selectDueCompensationIds(LocalDateTime.now(), limit);
    }

    @Override
    public void compensate(Long withdrawId) {
        transferExecutor.startTransfer(withdrawId);
    }
}
