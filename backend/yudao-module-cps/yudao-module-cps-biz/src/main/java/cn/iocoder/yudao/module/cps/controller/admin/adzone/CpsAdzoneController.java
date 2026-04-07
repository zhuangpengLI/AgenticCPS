package cn.iocoder.yudao.module.cps.controller.admin.adzone;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.cps.controller.admin.adzone.vo.CpsAdzonePageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.adzone.vo.CpsAdzoneRespVO;
import cn.iocoder.yudao.module.cps.controller.admin.adzone.vo.CpsAdzoneSaveReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import cn.iocoder.yudao.module.cps.service.adzone.CpsAdzoneService;
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

@Tag(name = "管理后台 - CPS推广位")
@RestController
@RequestMapping("/cps/adzone")
@Validated
public class CpsAdzoneController {

    @Resource
    private CpsAdzoneService adzoneService;

    @PostMapping("/create")
    @Operation(summary = "创建推广位")
    @PreAuthorize("@ss.hasPermission('cps:adzone:create')")
    public CommonResult<Long> createAdzone(@Valid @RequestBody CpsAdzoneSaveReqVO createReqVO) {
        return success(adzoneService.createAdzone(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新推广位")
    @PreAuthorize("@ss.hasPermission('cps:adzone:update')")
    public CommonResult<Boolean> updateAdzone(@Valid @RequestBody CpsAdzoneSaveReqVO updateReqVO) {
        adzoneService.updateAdzone(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除推广位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:adzone:delete')")
    public CommonResult<Boolean> deleteAdzone(@RequestParam("id") Long id) {
        adzoneService.deleteAdzone(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取推广位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:adzone:query')")
    public CommonResult<CpsAdzoneRespVO> getAdzone(@RequestParam("id") Long id) {
        CpsAdzoneDO adzone = adzoneService.getAdzone(id);
        return success(BeanUtils.toBean(adzone, CpsAdzoneRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取推广位分页")
    @PreAuthorize("@ss.hasPermission('cps:adzone:query')")
    public CommonResult<PageResult<CpsAdzoneRespVO>> getAdzonePage(@Valid CpsAdzonePageReqVO pageReqVO) {
        PageResult<CpsAdzoneDO> pageResult = adzoneService.getAdzonePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsAdzoneRespVO.class));
    }

    @GetMapping("/list-by-platform")
    @Operation(summary = "获取平台下的推广位列表")
    @Parameter(name = "platformCode", description = "平台编码", required = true)
    @PreAuthorize("@ss.hasPermission('cps:adzone:query')")
    public CommonResult<List<CpsAdzoneRespVO>> getAdzoneListByPlatformCode(@RequestParam("platformCode") String platformCode) {
        List<CpsAdzoneDO> list = adzoneService.getAdzoneListByPlatformCode(platformCode);
        return success(BeanUtils.toBean(list, CpsAdzoneRespVO.class));
    }

}
