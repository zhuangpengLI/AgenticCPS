package com.qiji.cps.module.cps.controller.admin.marketing;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingShortLinkCreateReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingShortLinkPageReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingShortLinkRespVO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingShortLinkDO;
import com.qiji.cps.module.cps.service.marketing.CpsMarketingShortLinkService;
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
import org.springframework.web.bind.annotation.RestController;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPS营销短链")
@RestController
@RequestMapping("/cps/marketing-short-link")
@Validated
public class CpsMarketingShortLinkController {

    @Resource
    private CpsMarketingShortLinkService shortLinkService;

    @PostMapping("/create")
    @Operation(summary = "创建营销短链")
    @PreAuthorize("@ss.hasPermission('cps:marketing-short-link:update')")
    public CommonResult<CpsMarketingShortLinkRespVO> createShortLink(
            @Valid @RequestBody CpsMarketingShortLinkCreateReqVO reqVO) {
        return success(BeanUtils.toBean(shortLinkService.createShortLink(reqVO), CpsMarketingShortLinkRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询营销短链")
    @PreAuthorize("@ss.hasPermission('cps:marketing-short-link:query')")
    public CommonResult<PageResult<CpsMarketingShortLinkRespVO>> getShortLinkPage(
            @Valid CpsMarketingShortLinkPageReqVO pageReqVO) {
        PageResult<CpsMarketingShortLinkDO> pageResult = shortLinkService.getShortLinkPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsMarketingShortLinkRespVO.class));
    }
}
