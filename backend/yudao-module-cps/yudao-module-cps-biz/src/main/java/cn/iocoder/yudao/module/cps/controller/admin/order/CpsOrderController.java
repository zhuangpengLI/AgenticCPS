package cn.iocoder.yudao.module.cps.controller.admin.order;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.order.vo.CpsOrderRespVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.order.CpsOrderDO;
import cn.iocoder.yudao.module.cps.service.order.CpsOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

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

    @PostMapping("/sync")
    @Operation(summary = "手动触发订单同步", description = "立即拉取指定平台最近N小时订单，默认同步最近2小时")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    @Parameter(name = "platformCode", description = "平台编码（taobao/jd/pdd/douyin）", required = true)
    @Parameter(name = "hours", description = "向前追溯小时数，默认2", example = "2")
    public CommonResult<String> manualSync(@RequestParam("platformCode") String platformCode,
                                           @RequestParam(value = "hours", defaultValue = "2") Integer hours) {
        String result = orderService.manualSync(platformCode, hours);
        return success(result);
    }

}
