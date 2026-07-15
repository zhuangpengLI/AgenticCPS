package com.qiji.cps.module.cps.controller.app.goods;

import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseRespVO;
import com.qiji.cps.module.cps.controller.app.goods.vo.AppCpsGoodsCompareReqVO;
import com.qiji.cps.module.cps.controller.app.goods.vo.AppCpsGoodsDetailReqVO;
import com.qiji.cps.module.cps.controller.app.goods.vo.AppCpsGoodsParseReqVO;
import com.qiji.cps.module.cps.controller.app.goods.vo.AppCpsGoodsSearchReqVO;
import com.qiji.cps.module.cps.controller.app.goods.vo.AppCpsLinkReqVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import com.qiji.cps.module.cps.service.goods.CpsGoodsToolboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppCpsGoodsControllerTest {

    @InjectMocks
    private AppCpsGoodsController controller;

    @Mock
    private CpsGoodsService cpsGoodsService;
    @Mock
    private CpsGoodsToolboxService goodsToolboxService;
    @Mock
    private CpsTransferRecordMapper transferRecordMapper;

    @Test
    @DisplayName("searchGoods maps commission amount to estimated rebate response")
    void searchGoods_mapsCommissionAmountToEstimatedRebateResponse() {
        AppCpsGoodsSearchReqVO reqVO = new AppCpsGoodsSearchReqVO();
        reqVO.setKeyword("coffee");
        reqVO.setPlatformCode("taobao");
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        when(cpsGoodsService.searchGoods(any(), any())).thenReturn(CpsGoodsSearchResult.builder()
                .list(List.of(CpsGoodsItem.builder()
                        .goodsId("ITEM-1")
                        .platformCode("taobao")
                        .title("Coffee")
                        .commissionAmount(new BigDecimal("1.23"))
                        .build()))
                .total(1L)
                .pageNo(1)
                .pageSize(10)
                .build());

        var response = controller.searchGoods(reqVO);

        assertEquals(new BigDecimal("1.23"),
                response.getData().getList().get(0).getEstimateRebateAmount());
    }

    @Test
    @DisplayName("compareGoods returns cheapest highest rebate and best overall goods")
    void compareGoods_returnsCheapestHighestRebateAndBestOverallGoods() {
        AppCpsGoodsCompareReqVO reqVO = new AppCpsGoodsCompareReqVO();
        reqVO.setKeyword("coffee");
        reqVO.setPageSize(10);
        when(cpsGoodsService.searchGoodsAllPlatforms(any())).thenReturn(List.of(
                CpsGoodsItem.builder()
                        .goodsId("CHEAP")
                        .platformCode("pdd")
                        .title("Cheap Coffee")
                        .actualPrice(new BigDecimal("9.90"))
                        .commissionAmount(new BigDecimal("0.50"))
                        .build(),
                CpsGoodsItem.builder()
                        .goodsId("REBATE")
                        .platformCode("taobao")
                        .title("High Rebate Coffee")
                        .actualPrice(new BigDecimal("39.90"))
                        .commissionAmount(new BigDecimal("6.00"))
                        .build(),
                CpsGoodsItem.builder()
                        .goodsId("BEST")
                        .platformCode("jd")
                        .title("Best Coffee")
                        .actualPrice(new BigDecimal("19.90"))
                        .commissionAmount(new BigDecimal("6.00"))
                        .build()
        ));

        var response = controller.compareGoods(reqVO);

        assertEquals("CHEAP", response.getData().getCheapestGoods().getGoodsId());
        assertEquals("REBATE", response.getData().getHighestRebateGoods().getGoodsId());
        assertEquals("BEST", response.getData().getBestOverallGoods().getGoodsId());
        assertEquals(3, response.getData().getList().size());
    }

    @Test
    @DisplayName("getDetail returns one goods detail with coupon and rebate estimate fields")
    void getDetail_returnsOneGoodsDetailWithCouponAndRebateEstimateFields() {
        AppCpsGoodsDetailReqVO reqVO = new AppCpsGoodsDetailReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setGoodsId("ITEM-1");
        when(cpsGoodsService.searchGoods(any(), any())).thenReturn(CpsGoodsSearchResult.builder()
                .list(List.of(CpsGoodsItem.builder()
                        .goodsId("ITEM-1")
                        .platformCode("taobao")
                        .title("Coffee")
                        .couponPrice(new BigDecimal("5.00"))
                        .couponConditions(new BigDecimal("50.00"))
                        .couponRemainNum(88L)
                        .couponEndTime("2026-08-01 23:59:59")
                        .commissionRate(new BigDecimal("12.50"))
                        .commissionAmount(new BigDecimal("1.23"))
                        .sellingPoint("高佣好券")
                        .build()))
                .total(1L)
                .pageNo(1)
                .pageSize(1)
                .build());

        var response = controller.getDetail(reqVO);

        assertEquals("ITEM-1", response.getData().getGoodsId());
        assertEquals(new BigDecimal("5.00"), response.getData().getCouponPrice());
        assertEquals(new BigDecimal("50.00"), response.getData().getCouponConditions());
        assertEquals(88L, response.getData().getCouponRemainNum());
        assertEquals("2026-08-01 23:59:59", response.getData().getCouponEndTime());
        assertEquals(new BigDecimal("1.23"), response.getData().getEstimateRebateAmount());
        assertEquals("高佣好券", response.getData().getSellingPoint());
    }

    @Test
    @DisplayName("parseContent exposes toolbox parse result through app endpoint")
    void parseContent_exposesToolboxParseResultThroughAppEndpoint() {
        AppCpsGoodsParseReqVO reqVO = new AppCpsGoodsParseReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setOriginalContent("https://item.taobao.com/item.htm?id=ITEM-1");
        when(goodsToolboxService.parseContent(any())).thenReturn(CpsGoodsParseRespVO.builder()
                .platformCode("taobao")
                .supported(true)
                .goodsId("ITEM-1")
                .itemLink("https://item.taobao.com/item.htm?id=ITEM-1")
                .parseSource("local")
                .build());

        var response = controller.parseContent(reqVO);

        assertEquals(true, response.getData().getSupported());
        assertEquals("ITEM-1", response.getData().getGoodsId());
        assertEquals("local", response.getData().getParseSource());
    }

    @Test
    @DisplayName("generateLink inserts transfer record for order attribution")
    void generateLink_insertsTransferRecordForOrderAttribution() {
        AppCpsLinkReqVO reqVO = new AppCpsLinkReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setGoodsId("ITEM-1");
        reqVO.setAdzoneId("mm_111_222_333");
        when(cpsGoodsService.resolvePromotionAdzoneId("taobao", 1001L, "mm_111_222_333"))
                .thenReturn("mm_111_222_333");
        when(cpsGoodsService.generatePromotionLink("taobao", "ITEM-1", null, 1001L, "mm_111_222_333"))
                .thenReturn(CpsPromotionLinkResult.builder()
                        .shortUrl("https://s.click/1")
                        .longUrl("https://item.taobao.com/item.htm?id=ITEM-1")
                        .tpwd("abc")
                        .build());

        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            var response = controller.generateLink(reqVO);

            assertEquals("https://s.click/1", response.getData().getShortUrl());
        }

        ArgumentCaptor<CpsTransferRecordDO> captor = ArgumentCaptor.forClass(CpsTransferRecordDO.class);
        verify(transferRecordMapper).insert(captor.capture());
        CpsTransferRecordDO record = captor.getValue();
        assertEquals(1001L, record.getMemberId());
        assertEquals("taobao", record.getPlatformCode());
        assertEquals("ITEM-1", record.getItemId());
        assertEquals("ITEM-1", record.getOriginalContent());
        assertEquals("mm_111_222_333", record.getAdzoneId());
        assertEquals("https://s.click/1", record.getPromotionUrl());
        assertEquals("abc", record.getTaoCommand());
        assertEquals(1, record.getStatus());
    }

    @Test
    @DisplayName("generateLink does not insert transfer record when link generation fails")
    void generateLink_doesNotInsertTransferRecordWhenLinkGenerationFails() {
        AppCpsLinkReqVO reqVO = new AppCpsLinkReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setGoodsId("ITEM-1");
        reqVO.setAdzoneId("mm_111_222_333");
        when(cpsGoodsService.resolvePromotionAdzoneId("taobao", 1001L, "mm_111_222_333"))
                .thenReturn("mm_111_222_333");
        when(cpsGoodsService.generatePromotionLink("taobao", "ITEM-1", null, 1001L, "mm_111_222_333"))
                .thenReturn(null);

        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            var response = controller.generateLink(reqVO);

            assertNull(response.getData());
        }

        verify(transferRecordMapper, never()).insert(any(CpsTransferRecordDO.class));
    }
}
