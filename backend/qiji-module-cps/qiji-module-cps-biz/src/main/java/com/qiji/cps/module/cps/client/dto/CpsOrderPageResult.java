package com.qiji.cps.module.cps.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 显式订单分页结果，避免使用最后一条订单隐式传递游标。
 */
@Data
@AllArgsConstructor
public class CpsOrderPageResult {

    private List<CpsOrderDTO> items;
    private CpsOrderPaginationMode paginationMode;
    private String nextCursor;
    private Integer nextPageNo;
    private boolean hasMore;

    public static CpsOrderPageResult page(List<CpsOrderDTO> items, Integer nextPageNo, boolean hasMore) {
        return new CpsOrderPageResult(safeItems(items), CpsOrderPaginationMode.PAGE,
                null, hasMore ? nextPageNo : null, hasMore);
    }

    public static CpsOrderPageResult cursor(List<CpsOrderDTO> items, String nextCursor, boolean hasMore) {
        return new CpsOrderPageResult(safeItems(items), CpsOrderPaginationMode.CURSOR,
                hasMore ? nextCursor : null, null, hasMore);
    }

    private static List<CpsOrderDTO> safeItems(List<CpsOrderDTO> items) {
        return items == null ? List.of() : List.copyOf(items);
    }
}
