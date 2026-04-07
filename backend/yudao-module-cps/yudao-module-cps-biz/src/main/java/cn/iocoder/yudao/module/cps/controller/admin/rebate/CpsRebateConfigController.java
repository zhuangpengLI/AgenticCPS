package cn.iocoder.yudao.module.cps.controller.admin.rebate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.cps.controller.admin.rebate.vo.CpsRebateConfigPageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.rebate.vo.CpsRebateConfigRespVO;
import cn.iocoder.yudao.module.cps.controller.admin.rebate.vo.CpsRebateConfigSaveReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import cn.iocoder.yudao.module.cps.service.rebate.CpsRebateConfigService;
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

/**
 * 管理后台 - 返利配置
 *
 * @author CPS System
 */
@Tag(name = "管理后台 - CPS返利配置")
@RestController
@RequestMapping("/cps/rebate-config")
@Validated
public class CpsRebateConfigController {

    @Resource
    private CpsRebateConfigService rebateConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建返利配置")
    @PreAuthorize("@ss.hasPermission('cps:rebate-config:create')")
    public CommonResult<Long> createRebateConfig(@Valid @RequestBody CpsRebateConfigSaveReqVO createReqVO) {
        return success(rebateConfigService.createRebateConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新返利配置")
    @PreAuthorize("@ss.hasPermission('cps:rebate-config:update')")
    public CommonResult<Boolean> updateRebateConfig(@Valid @RequestBody CpsRebateConfigSaveReqVO updateReqVO) {
        rebateConfigService.updateRebateConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除返利配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:rebate-config:delete')")
    public CommonResult<Boolean> deleteRebateConfig(@RequestParam("id") Long id) {
        rebateConfigService.deleteRebateConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取返利配置详情")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:rebate-config:query')")
    public CommonResult<CpsRebateConfigRespVO> getRebateConfig(@RequestParam("id") Long id) {
        CpsRebateConfigDO config = rebateConfigService.getRebateConfig(id);
        return success(BeanUtils.toBean(config, CpsRebateConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取返利配置分页")
    @PreAuthorize("@ss.hasPermission('cps:rebate-config:query')")
    public CommonResult<PageResult<CpsRebateConfigRespVO>> getRebateConfigPage(@Valid CpsRebateConfigPageReqVO pageReqVO) {
        PageResult<CpsRebateConfigDO> pageResult = rebateConfigService.getRebateConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsRebateConfigRespVO.class));
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "获取所有已启用的返利配置列表")
    @PreAuthorize("@ss.hasPermission('cps:rebate-config:query')")
    public CommonResult<List<CpsRebateConfigRespVO>> getEnabledRebateConfigList() {
        List<CpsRebateConfigDO> list = rebateConfigService.getEnabledRebateConfigList();
        return success(BeanUtils.toBean(list, CpsRebateConfigRespVO.class));
    }

}
