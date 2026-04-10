package com.qiji.cps.module.promotion.controller.app.coupon;

import cn.hutool.core.collection.CollUtil;
import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.promotion.controller.app.coupon.vo.coupon.AppCouponPageReqVO;
import com.qiji.cps.module.promotion.controller.app.coupon.vo.coupon.AppCouponRespVO;
import com.qiji.cps.module.promotion.controller.app.coupon.vo.coupon.AppCouponTakeReqVO;
import com.qiji.cps.module.promotion.convert.coupon.CouponConvert;
import com.qiji.cps.module.promotion.dal.dataobject.coupon.CouponDO;
import com.qiji.cps.module.promotion.dal.dataobject.coupon.CouponTemplateDO;
import com.qiji.cps.module.promotion.enums.coupon.CouponTakeTypeEnum;
import com.qiji.cps.module.promotion.service.coupon.CouponService;
import com.qiji.cps.module.promotion.service.coupon.CouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;
import static com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 优惠劵")
@RestController
@RequestMapping("/promotion/coupon")
@Validated
public class AppCouponController {

    @Resource
    private CouponService couponService;
    @Resource
    private CouponTemplateService couponTemplateService;

    @PostMapping("/take")
    @Operation(summary = "领取优惠劵")
    @Parameter(name = "templateId", description = "优惠券模板编号", required = true, example = "1024")
    public CommonResult<Boolean> takeCoupon(@Valid @RequestBody AppCouponTakeReqVO reqVO) {
        // 1. 领取优惠劵
        Long userId = getLoginUserId();
        couponService.takeCoupon(reqVO.getTemplateId(), CollUtil.newHashSet(userId), CouponTakeTypeEnum.USER);

        // 2. 检查是否可以继续领取
        CouponTemplateDO couponTemplate = couponTemplateService.getCouponTemplate(reqVO.getTemplateId());
        boolean canTakeAgain = true;
        if (couponTemplate.getTakeLimitCount() != null && couponTemplate.getTakeLimitCount() > 0) {
            Integer takeCount = couponService.getTakeCount(reqVO.getTemplateId(), userId);
            canTakeAgain = takeCount < couponTemplate.getTakeLimitCount();
        }
        return success(canTakeAgain);
    }

    @GetMapping("/page")
    @Operation(summary = "我的优惠劵列表")
    public CommonResult<PageResult<AppCouponRespVO>> getCouponPage(AppCouponPageReqVO pageReqVO) {
        PageResult<CouponDO> pageResult = couponService.getCouponPage(
                CouponConvert.INSTANCE.convert(pageReqVO, Collections.singleton(getLoginUserId())));
        return success(BeanUtils.toBean(pageResult, AppCouponRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得优惠劵")
    @Parameter(name = "id", description = "优惠劵编号", required = true, example = "1024")
    public CommonResult<AppCouponRespVO> getCoupon(@RequestParam("id") Long id) {
        CouponDO coupon = couponService.getCoupon(getLoginUserId(), id);
        return success(BeanUtils.toBean(coupon, AppCouponRespVO.class));
    }

    @GetMapping(value = "/get-unused-count")
    @Operation(summary = "获得未使用的优惠劵数量")
    public CommonResult<Long> getUnusedCouponCount() {
        return success(couponService.getUnusedCouponCount(getLoginUserId()));
    }

}
