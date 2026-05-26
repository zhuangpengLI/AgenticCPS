package com.qiji.cps.module.cps.service.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRebateActivitySyncRequest {

    private String platformCode;
    private String keyword;
    private Integer pageSize;
    private Integer maxPages;
}
