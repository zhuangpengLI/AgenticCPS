package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelRespVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.mysql.marketing.CpsMarketingClickEventMapper;
import com.qiji.cps.module.cps.dal.mysql.marketing.CpsMarketingShortLinkMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class CpsMarketingFunnelServiceImpl implements CpsMarketingFunnelService {

    @Resource
    private CpsMarketingShortLinkMapper shortLinkMapper;
    @Resource
    private CpsMarketingClickEventMapper clickEventMapper;
    @Resource
    private CpsTransferRecordMapper transferRecordMapper;
    @Resource
    private CpsOrderMapper orderMapper;

    @Override
    public CpsMarketingFunnelRespVO getFunnelSummary(CpsMarketingFunnelReqVO reqVO) {
        List<CpsOrderDO> orders = orderMapper.selectListForMarketingFunnel(reqVO);
        return CpsMarketingFunnelRespVO.builder()
                .exposureCount((long) shortLinkMapper.selectListForFunnel(reqVO).size())
                .clickCount((long) clickEventMapper.selectListForFunnel(reqVO).size())
                .transferCount((long) transferRecordMapper.selectListForMarketingFunnel(reqVO).size())
                .orderCount((long) orders.size())
                .settledOrderCount(countStatus(orders, CpsOrderStatusEnum.SETTLED.getStatus()))
                .rebateReadyOrderCount(countRebateReady(orders))
                .build();
    }

    private long countStatus(List<CpsOrderDO> orders, String status) {
        return orders.stream().filter(order -> status.equals(order.getOrderStatus())).count();
    }

    private long countRebateReady(List<CpsOrderDO> orders) {
        return orders.stream()
                .filter(order -> CpsOrderStatusEnum.REBATE_RECEIVED.getStatus().equals(order.getOrderStatus())
                        || order.getRebateTime() != null)
                .count();
    }
}
