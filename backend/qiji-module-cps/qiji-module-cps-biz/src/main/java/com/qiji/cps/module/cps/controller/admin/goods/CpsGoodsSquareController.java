package com.qiji.cps.module.cps.controller.admin.goods;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareLinkReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareLinkRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareMetaItemRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareMetaRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchRespVO;
import com.qiji.cps.module.cps.service.goods.CpsGoodsSquareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPS返利商品广场")
@RestController
@RequestMapping("/cps/goods-square")
@Validated
public class CpsGoodsSquareController {

    @Resource
    private CpsGoodsSquareService goodsSquareService;

    @GetMapping("/meta")
    @Operation(summary = "获取返利商品广场选品元数据")
    @PreAuthorize("@ss.hasPermission('cps:goods-square:query')")
    public CommonResult<CpsGoodsSquareMetaRespVO> getMeta(
            @RequestParam(value = "platformCode", required = false) String platformCode,
            @RequestParam(value = "vendorCode", required = false) String vendorCode) {
        return success(goodsSquareService.getMeta(platformCode, vendorCode));
    }

    @GetMapping("/hot-keywords")
    @Operation(summary = "获取大淘客热搜记录")
    @PreAuthorize("@ss.hasPermission('cps:goods-square:query')")
    public CommonResult<List<CpsGoodsSquareMetaItemRespVO>> getHotKeywords(
            @RequestParam(value = "platformCode", required = false, defaultValue = "taobao") String platformCode,
            @RequestParam(value = "vendorCode", required = false, defaultValue = "dataoke") String vendorCode,
            @RequestParam(value = "type", required = false, defaultValue = "1") Integer type) {
        return success(goodsSquareService.getHotKeywords(platformCode, vendorCode, type));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "获取大淘客搜索联想词")
    @PreAuthorize("@ss.hasPermission('cps:goods-square:query')")
    public CommonResult<List<CpsGoodsSquareMetaItemRespVO>> suggestKeywords(
            @RequestParam(value = "platformCode", required = false, defaultValue = "taobao") String platformCode,
            @RequestParam(value = "vendorCode", required = false, defaultValue = "dataoke") String vendorCode,
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "type", required = false, defaultValue = "1") Integer type) {
        return success(goodsSquareService.suggestKeywords(platformCode, vendorCode, keyword, type));
    }

    @GetMapping("/vendor-goods")
    @Operation(summary = "获取大淘客清单商品")
    @PreAuthorize("@ss.hasPermission('cps:goods-square:query')")
    public CommonResult<CpsGoodsSquareSearchRespVO> getVendorGoods(
            @RequestParam("sourceCode") String sourceCode,
            @RequestParam(value = "platformCode", required = false, defaultValue = "taobao") String platformCode,
            @RequestParam(value = "vendorCode", required = false, defaultValue = "dataoke") String vendorCode,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        return success(goodsSquareService.getVendorGoods(sourceCode, platformCode, vendorCode, pageSize));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索返利商品广场")
    @PreAuthorize("@ss.hasPermission('cps:goods-square:query')")
    public CommonResult<CpsGoodsSquareSearchRespVO> searchGoods(@Valid CpsGoodsSquareSearchReqVO reqVO) {
        return success(goodsSquareService.searchGoods(reqVO));
    }

    @PostMapping("/search-by-image")
    @Operation(summary = "图片搜商品")
    @PreAuthorize("@ss.hasPermission('cps:goods-square:query')")
    public CommonResult<CpsGoodsSquareSearchRespVO> searchByImage(@Valid @RequestBody CpsGoodsSquareSearchReqVO reqVO) {
        return success(goodsSquareService.searchByImage(reqVO));
    }

    @PostMapping("/link")
    @Operation(summary = "生成商品推广链接")
    @PreAuthorize("@ss.hasPermission('cps:goods-square:link')")
    public CommonResult<CpsGoodsSquareLinkRespVO> generateLink(@Valid @RequestBody CpsGoodsSquareLinkReqVO reqVO) {
        return success(goodsSquareService.generateLink(reqVO));
    }

}
