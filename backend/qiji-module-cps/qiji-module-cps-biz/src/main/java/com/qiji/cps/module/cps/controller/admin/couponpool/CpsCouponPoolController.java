package com.qiji.cps.module.cps.controller.admin.couponpool;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolPageReqVO;
import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolRespVO;
import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolUsableReqVO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsCouponPoolDO;
import com.qiji.cps.module.cps.service.goods.coupon.CpsCouponPoolService;
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

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPS券池")
@RestController
@RequestMapping("/cps/coupon-pool")
@Validated
public class CpsCouponPoolController {

    @Resource
    private CpsCouponPoolService couponPoolService;

    @GetMapping("/page")
    @Operation(summary = "分页查询券池")
    @PreAuthorize("@ss.hasPermission('cps:coupon-pool:query')")
    public CommonResult<PageResult<CpsCouponPoolRespVO>> getCouponPage(@Valid CpsCouponPoolPageReqVO pageReqVO) {
        PageResult<CpsCouponPoolDO> pageResult = couponPoolService.getCouponPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsCouponPoolRespVO.class));
    }

    @PostMapping("/save")
    @Operation(summary = "保存券池记录")
    @PreAuthorize("@ss.hasPermission('cps:coupon-pool:update')")
    public CommonResult<Long> saveCoupon(@Valid @RequestBody CpsCouponPoolSaveReqVO reqVO) {
        return success(couponPoolService.saveCoupon(reqVO));
    }

    @GetMapping("/usable-list")
    @Operation(summary = "查询商品可用券")
    @PreAuthorize("@ss.hasPermission('cps:coupon-pool:query')")
    public CommonResult<List<CpsCouponPoolRespVO>> listUsableCoupons(@Valid CpsCouponPoolUsableReqVO reqVO) {
        return success(BeanUtils.toBean(couponPoolService.listUsableCoupons(reqVO), CpsCouponPoolRespVO.class));
    }
}
