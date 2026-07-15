package com.qiji.cps.module.cps.controller.admin.goodsmaster;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsMasterImportSelectionItemReqVO;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsMasterPageReqVO;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsMasterRespVO;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsPriceSnapshotPageReqVO;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsPriceSnapshotRespVO;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsSourceMappingPageReqVO;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsSourceMappingRespVO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsMasterDO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsPriceSnapshotDO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsSourceMappingDO;
import com.qiji.cps.module.cps.service.goods.master.CpsGoodsMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPS商品主档")
@RestController
@RequestMapping("/cps/goods-master")
@Validated
public class CpsGoodsMasterController {

    @Resource
    private CpsGoodsMasterService goodsMasterService;

    @GetMapping("/page")
    @Operation(summary = "分页查询商品主档")
    @PreAuthorize("@ss.hasPermission('cps:goods-master:query')")
    public CommonResult<PageResult<CpsGoodsMasterRespVO>> getGoodsMasterPage(
            @Valid CpsGoodsMasterPageReqVO pageReqVO) {
        PageResult<CpsGoodsMasterDO> pageResult = goodsMasterService.getGoodsMasterPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsGoodsMasterRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得商品主档")
    @Parameter(name = "id", description = "商品主档ID", required = true)
    @PreAuthorize("@ss.hasPermission('cps:goods-master:query')")
    public CommonResult<CpsGoodsMasterRespVO> getGoodsMaster(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(goodsMasterService.getGoodsMaster(id), CpsGoodsMasterRespVO.class));
    }

    @PostMapping("/import-selection-item")
    @Operation(summary = "从选品主题商品快照导入商品主档")
    @PreAuthorize("@ss.hasPermission('cps:goods-master:import')")
    public CommonResult<Long> importSelectionItem(@Valid @RequestBody CpsGoodsMasterImportSelectionItemReqVO reqVO) {
        return success(goodsMasterService.importSelectionItem(reqVO.getSelectionItemId()));
    }

    @GetMapping("/source/page")
    @Operation(summary = "分页查询商品来源映射")
    @PreAuthorize("@ss.hasPermission('cps:goods-master:query')")
    public CommonResult<PageResult<CpsGoodsSourceMappingRespVO>> getSourceMappingPage(
            @Valid CpsGoodsSourceMappingPageReqVO pageReqVO) {
        PageResult<CpsGoodsSourceMappingDO> pageResult = goodsMasterService.getSourceMappingPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsGoodsSourceMappingRespVO.class));
    }

    @GetMapping("/price-snapshot/page")
    @Operation(summary = "分页查询商品价格快照")
    @PreAuthorize("@ss.hasPermission('cps:goods-master:query')")
    public CommonResult<PageResult<CpsGoodsPriceSnapshotRespVO>> getPriceSnapshotPage(
            @Valid CpsGoodsPriceSnapshotPageReqVO pageReqVO) {
        PageResult<CpsGoodsPriceSnapshotDO> pageResult = goodsMasterService.getPriceSnapshotPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsGoodsPriceSnapshotRespVO.class));
    }
}
