package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;

import java.util.List;

/**
 * Transaction boundary for one upstream order page.
 */
public interface CpsOrderSyncPageService {

    /**
     * Persists one complete page atomically and returns new/update/skip counts.
     */
    int[] persistPage(List<CpsOrderDTO> orders);
}
