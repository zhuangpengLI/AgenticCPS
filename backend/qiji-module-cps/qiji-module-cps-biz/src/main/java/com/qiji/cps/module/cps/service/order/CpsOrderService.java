package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CPS 订单 Service 接口
 *
 * @author CPS System
 */
public interface CpsOrderService {

    /**
     * 保存或更新订单（幂等处理，同步任务调用）
     *
     * <p>根据 platformOrderId 判断：不存在则新增，已存在则比对状态后决定是否更新。</p>
     *
     * @param orderDTO 平台返回的原始订单 DTO
     * @return 1-新增，2-更新，0-跳过（无变化）
     */
    int saveOrUpdateOrder(CpsOrderDTO orderDTO);

    /**
     * 批量保存或更新订单
     *
     * @param orderDTOs 订单列表
     * @return 统计结果 [newCount, updateCount, skipCount]
     */
    int[] batchSaveOrUpdateOrders(List<CpsOrderDTO> orderDTOs);

    /**
     * 获取订单（管理端）
     */
    CpsOrderDO getOrder(Long id);

    /**
     * 删除订单（管理端）
     */
    void deleteOrder(Long id);

    /**
     * 批量删除订单（管理端）
     */
    void deleteOrderList(List<Long> ids);

    /**
     * 分页查询订单（管理端）
     */
    PageResult<CpsOrderDO> getOrderPage(CpsOrderPageReqVO pageReqVO);

    /**
     * 分页查询指定会员的订单（App端）
     */
    PageResult<CpsOrderDO> getMemberOrderPage(CpsOrderPageReqVO pageReqVO, Long memberId);

    /**
     * 查询指定会员的订单详情（App端）
     */
    CpsOrderDO getMemberOrder(Long memberId, Long id);

    /**
     * 根据平台订单号获取订单
     */
    CpsOrderDO getOrderByPlatformOrderId(String platformCode, String platformOrderId);

    /**
     * 将订单中的淘宝 special_id 手动绑定到本地会员，并回写当前订单归因。
     */
    void bindSpecialIdToMember(Long orderId, Long memberId);

    /**
     * 将订单中的淘宝 special_id 手动绑定到本地会员，并记录幂等与人工复核审计信息。
     */
    void bindSpecialIdToMember(CpsOrderManualBindCommand command);

    /**
     * 手动触发单个平台订单同步（管理端操作）
     *
     * @param platformCode 平台编码
     * @param hours        向前追溯小时数（默认 2 小时）
     * @return 同步统计信息描述
     */
    String manualSync(String platformCode, Integer hours);

    /**
     * 手动触发单个平台订单同步（管理端操作）
     *
     * @param platformCode 平台编码
     * @param hours        向前追溯小时数（默认 2 小时）
     * @param queryType    查询时间维度（1=下单时间，2=付款时间，3=结算时间，4=更新时间）
     * @return 同步统计信息描述
     */
    String manualSync(String platformCode, Integer hours, Integer queryType);

    /**
     * 按指定起止时间同步订单；起止时间为空时回退到 hours 窗口。
     */
    String manualSync(String platformCode, Integer hours, Integer queryType,
                      LocalDateTime startTime, LocalDateTime endTime);

}
