package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - CPS商品批量转链 Response VO")
@Data
@Builder
public class CpsGoodsBatchTransferRespVO {

    @Schema(description = "批量转链结果列表")
    private List<Item> items;

    @Schema(description = "成功数量")
    private Integer successCount;

    @Schema(description = "失败数量")
    private Integer failureCount;

    @Schema(description = "批量转链单条结果")
    @Data
    @Builder
    public static class Item {

        @Schema(description = "原始输入下标")
        private Integer inputIndex;

        @Schema(description = "原始内容")
        private String originalContent;

        @Schema(description = "处理状态 SUCCESS/PARSE_FAILED/LINK_FAILED")
        private String status;

        @Schema(description = "处理消息")
        private String message;

        @Schema(description = "商品信息")
        private CpsGoodsRebateQueryRespVO.Goods goods;

        @Schema(description = "返利信息")
        private CpsGoodsRebateQueryRespVO.Rebate rebate;

        @Schema(description = "链接信息")
        private CpsGoodsRebateQueryRespVO.Links links;

        @Schema(description = "转链记录ID")
        private Long transferRecordId;

        @Schema(description = "可复制推广内容")
        private String promotionContent;

    }

}
