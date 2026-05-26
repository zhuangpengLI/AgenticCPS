package com.qiji.cps.module.cps.client.haodanku.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HdkActivityListRequest {

    private Integer pageNo;

    private String keyword;

    private Integer catId;

    private Integer promotionType;

    private Integer secondaryCatId;

    /**
     * 好单库页面排序：1 热门，2 最新。
     */
    private Integer order;

}
