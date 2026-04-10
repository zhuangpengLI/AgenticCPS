package com.qiji.cps.module.cps.controller.admin.freeze;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.freeze.vo.*;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.service.freeze.CpsFreezeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - CPS冻结解冻 Controller
 *
 * <p>提供冻结配置 CRUD 管理、冻结记录查询和手动解冻功能。</p>
 *
 * @author CPS System
 */
@Tag(name = "管理后台 - CPS冻结解冻")
@RestController
@RequestMapping("/cps/freeze")
@Validated
public class CpsFreezeController {

    @Resource
    private CpsFreezeService freezeService;

    // ==================== 冻结配置管理 ====================

    @PostMapping("/config/create")
    @Operation(summary = "创建冻结配置")
    @PreAuthorize("@ss.hasPermission('cps:freeze-config:create')")
    public CommonResult<Long> createFreezeConfig(@Valid @RequestBody CpsFreezeConfigSaveReqVO reqVO) {
        return success(freezeService.createFreezeConfig(reqVO));
    }

    @PutMapping("/config/update")
    @Operation(summary = "更新冻结配置")
    @PreAuthorize("@ss.hasPermission('cps:freeze-config:update')")
    public CommonResult<Boolean> updateFreezeConfig(@Valid @RequestBody CpsFreezeConfigSaveReqVO reqVO) {
        freezeService.updateFreezeConfig(reqVO);
        return success(true);
    }

    @DeleteMapping("/config/delete")
    @Operation(summary = "删除冻结配置")
    @Parameter(name = "id", description = "配置ID", required = true)
    @PreAuthorize("@ss.hasPermission('cps:freeze-config:delete')")
    public CommonResult<Boolean> deleteFreezeConfig(@RequestParam Long id) {
        freezeService.deleteFreezeConfig(id);
        return success(true);
    }

    @GetMapping("/config/page")
    @Operation(summary = "获取冻结配置分页")
    @PreAuthorize("@ss.hasPermission('cps:freeze-config:query')")
    public CommonResult<PageResult<CpsFreezeConfigRespVO>> getFreezeConfigPage(
            @Valid CpsFreezeConfigPageReqVO reqVO) {
        PageResult<CpsFreezeConfigDO> page = freezeService.getFreezeConfigPage(reqVO);
        return success(BeanUtils.toBean(page, CpsFreezeConfigRespVO.class));
    }

    // ==================== 冻结记录管理 ====================

    @GetMapping("/record/page")
    @Operation(summary = "获取冻结记录分页")
    @PreAuthorize("@ss.hasPermission('cps:freeze-record:query')")
    public CommonResult<PageResult<CpsFreezeRecordRespVO>> getFreezeRecordPage(
            @Valid CpsFreezeRecordPageReqVO reqVO) {
        PageResult<CpsFreezeRecordDO> page = freezeService.getFreezeRecordPage(reqVO);
        return success(BeanUtils.toBean(page, CpsFreezeRecordRespVO.class));
    }

    @PutMapping("/record/manual-unfreeze")
    @Operation(summary = "手动解冻指定记录")
    @Parameter(name = "id", description = "冻结记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('cps:freeze-record:unfreeze')")
    public CommonResult<Boolean> manualUnfreeze(@RequestParam Long id) {
        freezeService.manualUnfreeze(id);
        return success(true);
    }

}
