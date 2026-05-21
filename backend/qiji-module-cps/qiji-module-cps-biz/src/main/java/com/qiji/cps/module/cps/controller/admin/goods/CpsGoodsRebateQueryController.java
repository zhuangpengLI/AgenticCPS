package com.qiji.cps.module.cps.controller.admin.goods;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryRespVO;
import com.qiji.cps.module.cps.service.goods.CpsGoodsRebateQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - CPS 商品返利查询 Controller.
 *
 * @author CPS System
 */
@Tag(name = "管理后台 - CPS商品返利查询")
@RestController
@RequestMapping("/cps/goods")
@Validated
public class CpsGoodsRebateQueryController {

    @Resource
    private CpsGoodsRebateQueryService goodsRebateQueryService;

    @PostMapping("/rebate-query")
    @Operation(summary = "查询商品返利并生成推广内容")
    @PreAuthorize("@ss.hasPermission('cps:goods-rebate-query:query')")
    public CommonResult<CpsGoodsRebateQueryRespVO> queryRebate(@Valid @RequestBody CpsGoodsRebateQueryReqVO reqVO) {
        return success(goodsRebateQueryService.queryRebate(reqVO));
    }

}
