package com.qiji.cps.module.cps.controller.admin.marketing;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelRespVO;
import com.qiji.cps.module.cps.service.marketing.CpsMarketingFunnelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPS营销漏斗")
@RestController
@RequestMapping("/cps/marketing-funnel")
@Validated
public class CpsMarketingFunnelController {

    @Resource
    private CpsMarketingFunnelService funnelService;

    @GetMapping("/summary")
    @Operation(summary = "查询营销漏斗汇总")
    @PreAuthorize("@ss.hasPermission('cps:marketing-funnel:query')")
    public CommonResult<CpsMarketingFunnelRespVO> getFunnelSummary(@Valid CpsMarketingFunnelReqVO reqVO) {
        return success(funnelService.getFunnelSummary(reqVO));
    }
}
