package com.qiji.cps.module.cps.controller.app.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户 APP - 跨平台比价 Response VO
 *
 * @author CPS System
 */
@Schema(description = "用户 APP - 跨平台比价 Response VO")
@Data
public class AppCpsGoodsCompareRespVO {

    @Schema(description = "比价候选商品")
    private List<AppCpsGoodsRespVO> list;

    @Schema(description = "最低价商品")
    private AppCpsGoodsRespVO cheapestGoods;

    @Schema(description = "最高返利商品")
    private AppCpsGoodsRespVO highestRebateGoods;

    @Schema(description = "综合推荐商品")
    private AppCpsGoodsRespVO bestOverallGoods;

}
