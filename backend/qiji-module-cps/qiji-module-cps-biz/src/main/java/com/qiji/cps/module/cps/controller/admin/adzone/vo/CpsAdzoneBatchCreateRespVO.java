package com.qiji.cps.module.cps.controller.admin.adzone.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - CPS 推广位批量创建 Response VO")
@Data
@Builder
public class CpsAdzoneBatchCreateRespVO {

    @Schema(description = "总条数")
    private Integer totalCount;

    @Schema(description = "成功条数")
    private Integer successCount;

    @Schema(description = "失败条数")
    private Integer failureCount;

    @Schema(description = "逐条结果")
    private List<ItemResult> results;

    @Schema(description = "管理后台 - CPS 推广位批量创建逐条结果")
    @Data
    @Builder
    public static class ItemResult {

        @Schema(description = "输入序号，从 0 开始")
        private Integer index;

        @Schema(description = "推广位 ID")
        private String adzoneId;

        @Schema(description = "创建后的记录 ID")
        private Long id;

        @Schema(description = "是否成功")
        private Boolean success;

        @Schema(description = "失败原因")
        private String failureReason;

    }

}
