package com.qiji.cps.module.cps.controller.app.activity;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionRespVO;
import com.qiji.cps.module.cps.controller.app.activity.vo.AppCpsRebateActivityPromotionReqVO;
import com.qiji.cps.module.cps.service.activity.CpsRebateActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 APP - CPS 活动")
@RestController
@RequestMapping("/cps/rebate-activity")
@Validated
public class AppCpsRebateActivityController {

    @Resource
    private CpsRebateActivityService activityService;

    @PostMapping("/promotion")
    @Operation(summary = "生成当前会员的活动推广链接")
    public CommonResult<CpsRebateActivityPromotionRespVO> generatePromotion(
            @Valid @RequestBody AppCpsRebateActivityPromotionReqVO reqVO) {
        CpsRebateActivityPromotionReqVO serviceRequest = new CpsRebateActivityPromotionReqVO();
        serviceRequest.setActivityId(reqVO.getActivityId());
        serviceRequest.setChannelTag(reqVO.getChannelTag());
        return success(activityService.generatePromotionContent(
                serviceRequest, SecurityFrameworkUtils.getLoginUserId()));
    }
}
