package com.qiji.cps.module.cps.controller.app.order;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import com.qiji.cps.module.cps.controller.app.order.vo.AppCpsOrderPageReqVO;
import com.qiji.cps.module.cps.controller.app.order.vo.AppCpsOrderRespVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.service.order.CpsOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 APP - CPS 订单")
@RestController
@RequestMapping("/cps/order")
@Validated
public class AppCpsOrderController {

    @Resource
    private CpsOrderService orderService;

    @GetMapping("/page")
    @Operation(summary = "获取我的 CPS 订单")
    public CommonResult<PageResult<AppCpsOrderRespVO>> getMyOrderPage(@Valid AppCpsOrderPageReqVO reqVO) {
        Long memberId = SecurityFrameworkUtils.getLoginUserId();
        PageResult<CpsOrderDO> page = orderService.getMemberOrderPage(toOrderPageReq(reqVO), memberId);
        return success(BeanUtils.toBean(page, AppCpsOrderRespVO.class));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取我的 CPS 订单详情")
    public CommonResult<AppCpsOrderRespVO> getMyOrder(@PathVariable Long id) {
        Long memberId = SecurityFrameworkUtils.getLoginUserId();
        return success(BeanUtils.toBean(orderService.getMemberOrder(memberId, id), AppCpsOrderRespVO.class));
    }

    private CpsOrderPageReqVO toOrderPageReq(AppCpsOrderPageReqVO reqVO) {
        CpsOrderPageReqVO pageReqVO = new CpsOrderPageReqVO();
        pageReqVO.setPageNo(reqVO.getPageNo());
        pageReqVO.setPageSize(reqVO.getPageSize());
        pageReqVO.setPlatformCode(reqVO.getPlatformCode());
        pageReqVO.setOrderStatus(reqVO.getOrderStatus());
        return pageReqVO;
    }
}
