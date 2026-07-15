package com.qiji.cps.module.cps.controller.admin.order.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.qiji.cps.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CPS 订单同步检查点分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsOrderSyncCheckpointPageReqVO extends PageParam {

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "供应商编码", example = "dataoke")
    private String vendorCode;

    @Schema(description = "订单场景", example = "2")
    private Integer orderScene;

    @Schema(description = "查询类型", example = "4")
    private String queryType;

    @Schema(description = "分页模式", example = "CURSOR")
    private String paginationMode;

    @Schema(description = "最后同步状态", example = "PARTIAL")
    private String lastSyncStatus;

    @Schema(description = "更新时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] updateTime;
}
