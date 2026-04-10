package com.qiji.cps.module.cps.controller.admin.risk;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.risk.vo.CpsRiskRulePageReqVO;
import com.qiji.cps.module.cps.controller.admin.risk.vo.CpsRiskRuleRespVO;
import com.qiji.cps.module.cps.controller.admin.risk.vo.CpsRiskRuleSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.risk.CpsRiskRuleDO;
import com.qiji.cps.module.cps.service.risk.CpsRiskService;
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
 * 管理后台 - CPS风控规则 Controller
 *
 * <p>提供风控规则（频率限制+黑名单）的 CRUD 管理接口。</p>
 *
 * @author CPS System
 */
@Tag(name = "管理后台 - CPS风控规则")
@RestController
@RequestMapping("/cps/risk/rule")
@Validated
public class CpsRiskRuleController {

    @Resource
    private CpsRiskService riskService;

    @PostMapping("/create")
    @Operation(summary = "创建风控规则")
    @PreAuthorize("@ss.hasPermission('cps:risk-rule:create')")
    public CommonResult<Long> createRule(@Valid @RequestBody CpsRiskRuleSaveReqVO reqVO) {
        return success(riskService.createRule(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新风控规则")
    @PreAuthorize("@ss.hasPermission('cps:risk-rule:update')")
    public CommonResult<Boolean> updateRule(@Valid @RequestBody CpsRiskRuleSaveReqVO reqVO) {
        riskService.updateRule(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除风控规则")
    @Parameter(name = "id", description = "规则ID", required = true)
    @PreAuthorize("@ss.hasPermission('cps:risk-rule:delete')")
    public CommonResult<Boolean> deleteRule(@RequestParam Long id) {
        riskService.deleteRule(id);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获取风控规则分页")
    @PreAuthorize("@ss.hasPermission('cps:risk-rule:query')")
    public CommonResult<PageResult<CpsRiskRuleRespVO>> getRulePage(
            @Valid CpsRiskRulePageReqVO reqVO) {
        PageResult<CpsRiskRuleDO> page = riskService.getRulePage(reqVO);
        return success(BeanUtils.toBean(page, CpsRiskRuleRespVO.class));
    }

}
