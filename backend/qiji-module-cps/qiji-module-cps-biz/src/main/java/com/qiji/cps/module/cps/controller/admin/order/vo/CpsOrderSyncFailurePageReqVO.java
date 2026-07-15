package com.qiji.cps.module.cps.controller.admin.order.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS 订单同步失败分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CpsOrderSyncFailurePageReqVO extends PageParam {

    @Schema(description = "平台编码")
    private String platformCode;
    @Schema(description = "供应商编码")
    private String vendorCode;
    @Schema(description = "订单场景")
    private Integer orderScene;
    @Schema(description = "查询时间类型")
    private String queryType;
    @Schema(description = "失败阶段")
    private String failureStage;
    @Schema(description = "恢复状态")
    private String status;
    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;
}
