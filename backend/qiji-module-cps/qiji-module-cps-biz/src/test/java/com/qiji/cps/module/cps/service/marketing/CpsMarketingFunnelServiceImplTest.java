package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelRespVO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingClickEventDO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingShortLinkDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.marketing.CpsMarketingClickEventMapper;
import com.qiji.cps.module.cps.dal.mysql.marketing.CpsMarketingShortLinkMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsMarketingFunnelServiceImplTest {

    @InjectMocks
    private CpsMarketingFunnelServiceImpl service;

    @Mock
    private CpsMarketingShortLinkMapper shortLinkMapper;
    @Mock
    private CpsMarketingClickEventMapper clickEventMapper;
    @Mock
    private CpsTransferRecordMapper transferRecordMapper;
    @Mock
    private CpsOrderMapper orderMapper;

    @Test
    @DisplayName("getFunnelSummary returns read-only marketing counts")
    void getFunnelSummary_returnsReadOnlyMarketingCounts() {
        CpsMarketingFunnelReqVO reqVO = new CpsMarketingFunnelReqVO();
        reqVO.setCampaignId("camp-1");
        reqVO.setStartTime(LocalDateTime.of(2026, 7, 1, 0, 0));
        reqVO.setEndTime(LocalDateTime.of(2026, 7, 31, 23, 59));
        when(shortLinkMapper.selectListForFunnel(reqVO)).thenReturn(List.of(
                CpsMarketingShortLinkDO.builder().id(1L).build(),
                CpsMarketingShortLinkDO.builder().id(2L).build()));
        when(clickEventMapper.selectListForFunnel(reqVO)).thenReturn(List.of(
                CpsMarketingClickEventDO.builder().id(1L).clickId("CLK1").build(),
                CpsMarketingClickEventDO.builder().id(2L).clickId("CLK2").build(),
                CpsMarketingClickEventDO.builder().id(3L).clickId("CLK3").build()));
        when(transferRecordMapper.selectListForMarketingFunnel(reqVO)).thenReturn(List.of(
                CpsTransferRecordDO.builder().id(1L).build(),
                CpsTransferRecordDO.builder().id(2L).build()));
        when(orderMapper.selectListForMarketingFunnel(reqVO)).thenReturn(List.of(
                CpsOrderDO.builder().id(1L).orderStatus(CpsOrderStatusEnum.SETTLED.getStatus()).rebateTime(null).build(),
                CpsOrderDO.builder().id(2L).orderStatus(CpsOrderStatusEnum.REBATE_RECEIVED.getStatus()).rebateTime(LocalDateTime.now()).build()));

        CpsMarketingFunnelRespVO result = service.getFunnelSummary(reqVO);

        assertEquals(2L, result.getExposureCount());
        assertEquals(3L, result.getClickCount());
        assertEquals(2L, result.getTransferCount());
        assertEquals(2L, result.getOrderCount());
        assertEquals(1L, result.getSettledOrderCount());
        assertEquals(1L, result.getRebateReadyOrderCount());
    }
}
