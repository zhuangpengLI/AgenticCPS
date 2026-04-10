package com.qiji.cps.module.cps.client.dto;

import lombok.Data;

/**
 * CPS 订单查询请求 DTO（平台无关）
 *
 * @author CPS System
 */
@Data
public class CpsOrderQueryRequest {

    /**
     * 查询时间类型（1-创建时间，2-付款时间，3-结算时间，4-更新时间）
     */
    private Integer queryType = 1;

    /**
     * 查询开始时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String startTime;

    /**
     * 查询结束时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String endTime;

    /**
     * 页码
     */
    private Integer pageNo = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 50;

    /**
     * 翻页游标（部分平台使用）
     */
    private String positionIndex;

    /**
     * 订单状态过滤（null-全部，平台相关）
     */
    private Integer orderStatus;

}
