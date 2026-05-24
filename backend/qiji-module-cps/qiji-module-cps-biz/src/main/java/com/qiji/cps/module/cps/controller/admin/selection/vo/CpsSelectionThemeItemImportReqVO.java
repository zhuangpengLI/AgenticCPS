package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - CPS选品主题商品导入 Request VO")
@Data
public class CpsSelectionThemeItemImportReqVO {

    @Schema(description = "主题ID")
    @NotNull(message = "主题ID不能为空")
    private Long themeId;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "导入商品列表")
    @Valid
    @NotEmpty(message = "导入商品不能为空")
    private List<ImportItem> items;

    @Data
    public static class ImportItem {

        @NotBlank(message = "平台编码不能为空")
        private String platformCode;
        private String vendorCode;
        @NotBlank(message = "商品ID不能为空")
        private String goodsId;
        private String goodsSign;
        private String title;
        private String mainPic;
        private BigDecimal originalPrice;
        private BigDecimal actualPrice;
        private BigDecimal couponPrice;
        private BigDecimal commissionRate;
        private BigDecimal commissionAmount;
        private Long monthSales;
        private String shopName;
        private String brandName;
        private String categoryName;
        private String activityTag;
        private String rankTag;
        private String sellingPoint;
        private BigDecimal recommendScore;
        private String recommendReason;
        private Integer topFlag;
        private String status;
        private String itemLink;
        private Integer sort;
        private String rawData;
    }
}
