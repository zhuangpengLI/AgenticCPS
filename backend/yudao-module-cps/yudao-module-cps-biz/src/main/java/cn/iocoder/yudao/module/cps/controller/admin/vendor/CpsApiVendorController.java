package cn.iocoder.yudao.module.cps.controller.admin.vendor;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.cps.controller.admin.vendor.vo.CpsApiVendorPageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.vendor.vo.CpsApiVendorRespVO;
import cn.iocoder.yudao.module.cps.controller.admin.vendor.vo.CpsApiVendorSaveReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.vendor.CpsApiVendorDO;
import cn.iocoder.yudao.module.cps.service.vendor.CpsApiVendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPS API供应商配置")
@RestController
@RequestMapping("/cps/api-vendor")
@Validated
public class CpsApiVendorController {

    @Resource
    private CpsApiVendorService vendorService;

    @PostMapping("/create")
    @Operation(summary = "创建API供应商配置")
    @PreAuthorize("@ss.hasPermission('cps:api-vendor:create')")
    public CommonResult<Long> createVendor(@Valid @RequestBody CpsApiVendorSaveReqVO createReqVO) {
        return success(vendorService.createVendor(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新API供应商配置")
    @PreAuthorize("@ss.hasPermission('cps:api-vendor:update')")
    public CommonResult<Boolean> updateVendor(@Valid @RequestBody CpsApiVendorSaveReqVO updateReqVO) {
        vendorService.updateVendor(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除API供应商配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:api-vendor:delete')")
    public CommonResult<Boolean> deleteVendor(@RequestParam("id") Long id) {
        vendorService.deleteVendor(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取API供应商配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:api-vendor:query')")
    public CommonResult<CpsApiVendorRespVO> getVendor(@RequestParam("id") Long id) {
        CpsApiVendorDO vendor = vendorService.getVendor(id);
        return success(BeanUtils.toBean(vendor, CpsApiVendorRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取API供应商配置分页")
    @PreAuthorize("@ss.hasPermission('cps:api-vendor:query')")
    public CommonResult<PageResult<CpsApiVendorRespVO>> getVendorPage(@Valid CpsApiVendorPageReqVO pageReqVO) {
        PageResult<CpsApiVendorDO> pageResult = vendorService.getVendorPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsApiVendorRespVO.class));
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "获取已启用的供应商列表")
    @PreAuthorize("@ss.hasPermission('cps:api-vendor:query')")
    public CommonResult<List<CpsApiVendorRespVO>> getEnabledVendorList() {
        List<CpsApiVendorDO> list = vendorService.getEnabledVendorList();
        return success(BeanUtils.toBean(list, CpsApiVendorRespVO.class));
    }

    @GetMapping("/list-by-platform")
    @Operation(summary = "获取指定平台的供应商列表")
    @Parameter(name = "platformCode", description = "平台编码", required = true, example = "taobao")
    @PreAuthorize("@ss.hasPermission('cps:api-vendor:query')")
    public CommonResult<List<CpsApiVendorRespVO>> getVendorsByPlatform(@RequestParam("platformCode") String platformCode) {
        List<CpsApiVendorDO> list = vendorService.getEnabledVendorsByPlatform(platformCode);
        return success(BeanUtils.toBean(list, CpsApiVendorRespVO.class));
    }

}
