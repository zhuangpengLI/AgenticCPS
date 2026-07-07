package com.qiji.cps.module.cps.controller.admin.order;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderBindSpecialIdReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderRespVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.service.order.CpsOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - CPS 订单管理
 *
 * @author CPS System
 */
@Tag(name = "管理后台 - CPS 订单管理")
@RestController
@RequestMapping("/cps/order")
@Validated
public class CpsOrderController {

    @Resource
    private CpsOrderService orderService;

    @GetMapping("/page")
    @Operation(summary = "获取订单分页")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<PageResult<CpsOrderRespVO>> getOrderPage(@Valid CpsOrderPageReqVO pageReqVO) {
        PageResult<CpsOrderDO> pageResult = orderService.getOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsOrderRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取订单详情")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    @Parameter(name = "id", description = "订单ID", required = true, example = "1")
    public CommonResult<CpsOrderRespVO> getOrder(@RequestParam("id") Long id) {
        CpsOrderDO order = orderService.getOrder(id);
        return success(BeanUtils.toBean(order, CpsOrderRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除订单")
    @PreAuthorize("@ss.hasPermission('cps:order:delete')")
    @Parameter(name = "id", description = "订单ID", required = true, example = "1")
    public CommonResult<Boolean> deleteOrder(@RequestParam("id") Long id) {
        orderService.deleteOrder(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除订单")
    @PreAuthorize("@ss.hasPermission('cps:order:delete')")
    @Parameter(name = "ids", description = "订单ID列表", required = true)
    public CommonResult<Boolean> deleteOrderList(@RequestParam("ids") List<Long> ids) {
        orderService.deleteOrderList(ids);
        return success(true);
    }

    @PostMapping("/sync")
    @Operation(summary = "手动触发订单同步", description = "立即拉取指定平台最近N小时订单，默认同步最近2小时")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    @Parameter(name = "platformCode", description = "平台编码（taobao/jd/pdd/douyin）", required = true)
    @Parameter(name = "hours", description = "向前追溯小时数，默认2", example = "2")
    @Parameter(name = "queryType", description = "查询时间维度：1下单时间 2付款时间 3结算时间 4更新时间，默认1", example = "4")
    public CommonResult<String> manualSync(@RequestParam("platformCode") String platformCode,
                                           @RequestParam(value = "hours", defaultValue = "2") Integer hours,
                                           @RequestParam(value = "queryType", defaultValue = "1") Integer queryType) {
        String result = orderService.manualSync(platformCode, hours, queryType);
        return success(result);
    }

    @PostMapping("/bind-special-id-member")
    @Operation(summary = "手动绑定 special_id 到会员")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    public CommonResult<Boolean> bindSpecialIdToMember(@Valid @RequestBody CpsOrderBindSpecialIdReqVO reqVO) {
        orderService.bindSpecialIdToMember(reqVO.getOrderId(), reqVO.getMemberId());
        return success(true);
    }

}
