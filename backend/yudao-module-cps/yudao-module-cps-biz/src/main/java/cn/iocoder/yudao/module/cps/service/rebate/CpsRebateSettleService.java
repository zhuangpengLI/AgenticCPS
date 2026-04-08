package cn.iocoder.yudao.module.cps.service.rebate;

import cn.iocoder.yudao.module.cps.dal.dataobject.order.CpsOrderDO;

/**
 * CPS 返利结算 Service 接口
 *
 * <p>负责：订单确认收货后的返利金额计算、返利记录生成、返利账户余额入账。</p>
 *
 * @author CPS System
 */
public interface CpsRebateSettleService {

    /**
     * 对单笔订单执行返利结算
     *
     * <p>幂等操作：若该订单已生成返利记录，则跳过。</p>
     *
     * @param order 已收货/已结算的 CPS 订单
     * @return true-成功入账，false-跳过（已结算过或无需结算）
     */
    boolean settleOrder(CpsOrderDO order);

    /**
     * 批量结算（定时任务调用）
     *
     * @param batchSize 每批最大数量
     * @return 统计结果 [successCount, skipCount, failCount]
     */
    int[] batchSettle(int batchSize);

    /**
     * 退款回扣：订单退款后扣回已入账的返利
     *
     * @param orderId 订单ID
     * @return true-扣回成功，false-无需扣回（返利未入账）
     */
    boolean reverseRebate(Long orderId);

    /**
     * 获取会员返利账户（不存在则初始化）
     *
     * @param memberId 会员ID
     * @return 返利账户 DO
     */
    cn.iocoder.yudao.module.cps.dal.dataobject.rebate.CpsRebateAccountDO getOrInitAccount(Long memberId);

}
