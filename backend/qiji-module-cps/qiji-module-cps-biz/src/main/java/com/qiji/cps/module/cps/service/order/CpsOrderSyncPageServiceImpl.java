package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default page-level transaction boundary for order synchronization.
 */
@Service
public class CpsOrderSyncPageServiceImpl implements CpsOrderSyncPageService {

    @Resource
    private CpsOrderService orderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int[] persistPage(List<CpsOrderDTO> orders) {
        int newCount = 0;
        int updateCount = 0;
        int skipCount = 0;
        Set<String> seenOrderKeys = new HashSet<>();
        for (CpsOrderDTO order : orders) {
            String orderKey = order.getPlatformCode() + "\u0000" + order.getPlatformOrderId();
            if (order.getPlatformCode() != null && order.getPlatformOrderId() != null
                    && !seenOrderKeys.add(orderKey)) {
                skipCount++;
                continue;
            }
            int result = orderService.saveOrUpdateOrder(order);
            if (result == 1) {
                newCount++;
            } else if (result == 2) {
                updateCount++;
            } else {
                skipCount++;
            }
        }
        return new int[]{newCount, updateCount, skipCount};
    }
}
