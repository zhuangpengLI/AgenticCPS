package com.qiji.cps.module.cps.controller.app.marketing;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeItemRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeRespVO;
import com.qiji.cps.module.cps.service.marketing.AppCpsMarketingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 APP - CPS营销")
@RestController
@RequestMapping("/cps/marketing")
@Validated
public class AppCpsMarketingController {

    @Resource
    private AppCpsMarketingService marketingService;

    @GetMapping("/activity-center")
    @Operation(summary = "查询营销活动中心")
    public CommonResult<List<AppCpsMarketingActivityRespVO>> getActivityCenter(
            @Valid AppCpsMarketingActivityReqVO reqVO) {
        return success(marketingService.getActivityCenter(getMemberId(), reqVO));
    }

    @GetMapping("/selection-themes")
    @Operation(summary = "查询已发布选品主题")
    public CommonResult<List<AppCpsMarketingSelectionThemeRespVO>> getSelectionThemes(
            @Valid AppCpsMarketingSelectionThemeReqVO reqVO) {
        return success(marketingService.getSelectionThemes(getMemberId(), reqVO));
    }

    @GetMapping("/selection-theme-items")
    @Operation(summary = "查询选品主题商品")
    public CommonResult<List<AppCpsMarketingSelectionThemeItemRespVO>> getSelectionThemeItems(
            @RequestParam("themeId") Long themeId) {
        return success(marketingService.getSelectionThemeItems(getMemberId(), themeId));
    }

    private Long getMemberId() {
        return SecurityFrameworkUtils.getLoginUserId();
    }
}
