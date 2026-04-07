package cn.iocoder.yudao.module.cps.controller.app.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户 APP - 商品搜索 Request VO
 *
 * @author CPS System
 */
@Schema(description = "用户 APP - 商品搜索 Request VO")
@Data
public class AppCpsGoodsSearchReqVO {

    @Schema(description = "搜索关键词", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    @Schema(description = "平台编码（不传则聚合所有平台）")
    private String platformCode;

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNo = 1;

    @Schema(description = "每页大小", defaultValue = "20")
    private Integer pageSize = 20;

    @Schema(description = "最低价格（券后价，元）")
    private BigDecimal priceLowerLimit;

    @Schema(description = "最高价格（券后价，元）")
    private BigDecimal priceUpperLimit;

    @Schema(description = "排序方式（0-综合，1-销量，2-价格升序，3-价格降序，4-佣金比例）", defaultValue = "0")
    private Integer sortType = 0;

    @Schema(description = "是否只返回有券商品（1-是，0-全部）")
    private Integer hasCoupon;

}
