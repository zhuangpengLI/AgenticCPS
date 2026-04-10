package com.qiji.cps.module.cps.service.rebate;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateRecordPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;

/**
 * CPS 返利记录 Service 接口
 *
 * <p>提供返利记录的分页查询、详情查询、退款回扣触发等功能。</p>
 *
 * @author CPS System
 */
public interface CpsRebateRecordService {

    /**
     * 获取返利记录分页（管理端）
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<CpsRebateRecordDO> getRebateRecordPage(CpsRebateRecordPageReqVO pageReqVO);

    /**
     * 获取返利记录详情
     *
     * @param id 主键ID
     * @return 返利记录，不存在则返回 null
     */
    CpsRebateRecordDO getRebateRecord(Long id);

    /**
     * 获取会员的返利记录分页（App端）
     *
     * @param memberId  会员ID
     * @param pageNo    页码
     * @param pageSize  每页数量
     * @return 分页结果
     */
    PageResult<CpsRebateRecordDO> getMemberRebateRecordPage(Long memberId, Integer pageNo, Integer pageSize);

    /**
     * 触发订单退款回扣（管理端手动操作）
     *
     * <p>调用 {@link CpsRebateSettleService#reverseRebate(Long)} 扣回已入账返利。</p>
     *
     * @param orderId 订单ID
     * @return true-扣回成功，false-无需扣回（未入账）
     */
    boolean reverseRebate(Long orderId);

}
