package com.qiji.cps.module.cps.controller.admin.marketing;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingClickEventPageReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingClickEventRecordReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingClickEventRespVO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingClickEventDO;
import com.qiji.cps.module.cps.service.marketing.CpsMarketingClickEventService;
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

@Tag(name = "管理后台 - CPS营销点击事件")
@RestController
@RequestMapping("/cps/marketing-click-event")
@Validated
public class CpsMarketingClickEventController {

    @Resource
    private CpsMarketingClickEventService clickEventService;

    @PostMapping("/record")
    @Operation(summary = "记录营销点击")
    @PreAuthorize("@ss.hasPermission('cps:marketing-click-event:update')")
    public CommonResult<CpsMarketingClickEventRespVO> recordClick(
            @Valid @RequestBody CpsMarketingClickEventRecordReqVO reqVO) {
        return success(BeanUtils.toBean(clickEventService.recordClick(reqVO), CpsMarketingClickEventRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询营销点击")
    @PreAuthorize("@ss.hasPermission('cps:marketing-click-event:query')")
    public CommonResult<PageResult<CpsMarketingClickEventRespVO>> getClickEventPage(
            @Valid CpsMarketingClickEventPageReqVO pageReqVO) {
        PageResult<CpsMarketingClickEventDO> pageResult = clickEventService.getClickEventPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsMarketingClickEventRespVO.class));
    }
}
