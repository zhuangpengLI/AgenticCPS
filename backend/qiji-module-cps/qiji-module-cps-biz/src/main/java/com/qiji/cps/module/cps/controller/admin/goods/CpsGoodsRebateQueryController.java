package com.qiji.cps.module.cps.controller.admin.goods;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsBatchTransferReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsBatchTransferRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryRespVO;
import com.qiji.cps.module.cps.service.goods.CpsGoodsRebateQueryService;
import com.qiji.cps.module.cps.service.goods.CpsGoodsToolboxService;
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

    @Resource
    private CpsGoodsToolboxService goodsToolboxService;

    @PostMapping("/rebate-query")
    @Operation(summary = "查询商品返利并生成推广内容")
    @PreAuthorize("@ss.hasPermission('cps:goods-rebate-query:query')")
    public CommonResult<CpsGoodsRebateQueryRespVO> queryRebate(@Valid @RequestBody CpsGoodsRebateQueryReqVO reqVO) {
        return success(goodsRebateQueryService.queryRebate(reqVO));
    }

    @PostMapping("/parse")
    @Operation(summary = "解析商品链接、商品ID或口令")
    @PreAuthorize("@ss.hasPermission('cps:toolbox:query')")
    public CommonResult<CpsGoodsParseRespVO> parseContent(@Valid @RequestBody CpsGoodsParseReqVO reqVO) {
        return success(goodsToolboxService.parseContent(reqVO));
    }

    @PostMapping("/batch-transfer")
    @Operation(summary = "批量查询返利并生成推广内容")
    @PreAuthorize("@ss.hasPermission('cps:toolbox:link')")
    public CommonResult<CpsGoodsBatchTransferRespVO> batchTransfer(@Valid @RequestBody CpsGoodsBatchTransferReqVO reqVO) {
        return success(goodsToolboxService.batchTransfer(reqVO));
    }

}
