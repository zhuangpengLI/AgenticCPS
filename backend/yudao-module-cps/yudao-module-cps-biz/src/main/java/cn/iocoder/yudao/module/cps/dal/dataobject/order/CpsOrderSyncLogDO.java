package cn.iocoder.yudao.module.cps.dal.dataobject.order;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * CPS 订单同步日志 DO
 *
 * <p>记录每次定时同步任务的执行情况，便于排查问题和监控同步状态。</p>
 *
 * @author CPS System
 */
@TableName("cps_order_sync_log")
@KeySequence("cps_order_sync_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsOrderSyncLogDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 平台编码（如 taobao / jd / pdd / douyin）
     */
    private String platformCode;

    /**
     * 同步类型（1-增量同步，2-全量补偿）
     */
    private Integer syncType;

    /**
     * 查询时间类型（1-创建时间，2-付款时间，3-结算时间，4-更新时间）
     */
    private Integer queryType;

    /**
     * 查询开始时间
     */
    private LocalDateTime queryStartTime;

    /**
     * 查询结束时间
     */
    private LocalDateTime queryEndTime;

    /**
     * 同步状态（1-成功，2-失败，3-部分成功）
     */
    private Integer syncStatus;

    /**
     * 拉取到的订单总数
     */
    private Integer totalCount;

    /**
     * 新增订单数
     */
    private Integer newCount;

    /**
     * 更新订单数
     */
    private Integer updateCount;

    /**
     * 忽略订单数（重复/无效）
     */
    private Integer skipCount;

    /**
     * 同步开始时间
     */
    private LocalDateTime syncStartTime;

    /**
     * 同步结束时间
     */
    private LocalDateTime syncEndTime;

    /**
     * 耗时（毫秒）
     */
    private Long costMs;

    /**
     * 错误信息（同步失败时记录）
     */
    private String errorMsg;

}
