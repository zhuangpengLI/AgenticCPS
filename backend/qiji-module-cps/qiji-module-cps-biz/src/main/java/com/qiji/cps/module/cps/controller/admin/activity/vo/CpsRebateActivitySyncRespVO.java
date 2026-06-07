package com.qiji.cps.module.cps.controller.admin.activity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - CPS活动中心第三方活动同步 Response VO")
@Data
public class CpsRebateActivitySyncRespVO {

    @Schema(description = "新增数量")
    private Integer insertedCount;

    @Schema(description = "更新数量")
    private Integer updatedCount;

    @Schema(description = "跳过数量")
    private Integer skippedCount;
}
