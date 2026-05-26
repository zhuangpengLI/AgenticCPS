package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS工具箱淘礼金计划 Request VO")
@Data
public class CpsGoodsCashGiftPlanReqVO {

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "new-user")
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "计划名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "618 爆品补贴")
    @NotBlank(message = "计划名称不能为空")
    private String campaignName;

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode = "taobao";

    @Schema(description = "商品ID", example = "123456")
    private String goodsId;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "总预算（元）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "总预算不能为空")
    @DecimalMin(value = "0.01", message = "总预算必须大于 0")
    private BigDecimal budgetAmount;

    @Schema(description = "单个淘礼金金额（元）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "单个淘礼金金额不能为空")
    @DecimalMin(value = "0.01", message = "单个淘礼金金额必须大于 0")
    private BigDecimal giftAmount;

    @Schema(description = "发放份数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发放份数不能为空")
    @Min(value = 1, message = "发放份数最小值为 1")
    private Integer totalQuantity;

    @Schema(description = "每人限领份数")
    @Min(value = 1, message = "每人限领份数最小值为 1")
    private Integer perUserLimit = 1;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

}
