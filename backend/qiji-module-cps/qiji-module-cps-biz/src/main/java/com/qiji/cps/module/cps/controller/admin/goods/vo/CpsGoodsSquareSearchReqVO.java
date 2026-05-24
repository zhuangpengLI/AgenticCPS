package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CPS商品广场搜索 Request VO")
@Data
public class CpsGoodsSquareSearchReqVO {

    @Schema(description = "关键词", example = "今日精选")
    private String keyword;

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "供应商编码", example = "haodanku")
    private String vendorCode;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = 1;

    @Schema(description = "每页大小", example = "20")
    @Min(value = 1, message = "每页大小最小值为 1")
    @Max(value = 100, message = "每页大小最大值为 100")
    private Integer pageSize = 20;

    @Schema(description = "最低券后价")
    private BigDecimal priceLowerLimit;

    @Schema(description = "最高券后价")
    private BigDecimal priceUpperLimit;

    @Schema(description = "排序方式（0综合 1销量 2价格升序 3价格降序 4佣金比例）")
    private Integer sortType;

    @Schema(description = "是否只返回有券商品（1是 0全部）")
    private Integer hasCoupon;

    @Schema(description = "选品频道编码", example = "hot")
    private String channelCode;

    @Schema(description = "供应商类目ID", example = "10")
    private String categoryId;

    @Schema(description = "最低佣金比例")
    private BigDecimal minCommissionRate;

    @Schema(description = "最低预估佣金")
    private BigDecimal minCommissionAmount;

    @Schema(description = "最低月销量")
    private Long minMonthSales;

    @Schema(description = "最低优惠券金额")
    private BigDecimal couponAmountMin;

    @Schema(description = "是否只看天猫")
    private Boolean tmallOnly;

    @Schema(description = "是否只看品牌")
    private Boolean brandOnly;

    @Schema(description = "店铺类型")
    private String shopType;

    @Schema(description = "活动标签")
    private String activityTag;

}
