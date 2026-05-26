package com.qiji.cps.module.cps.service.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRebateActivitySyncResult {

    private int insertedCount;
    private int updatedCount;
    private int skippedCount;
}
