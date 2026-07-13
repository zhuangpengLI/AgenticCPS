package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsCouponInfoVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsCouponInfo;
import com.qiji.cps.module.cps.client.dto.CpsContentParseResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsBatchTransferReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsCashGiftPlanReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsCouponQueryReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsOwnershipCheckReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareGoodsRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchRespVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.member.dal.dataobject.user.MemberUserDO;
import com.qiji.cps.module.member.service.user.MemberUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsGoodsToolboxServiceImplTest {

    @InjectMocks
    private CpsGoodsToolboxServiceImpl service;

    @Mock
    private CpsGoodsRebateQueryService goodsRebateQueryService;

    @Mock
    private CpsGoodsSquareService goodsSquareService;

    @Mock
    private CpsTransferRecordMapper transferRecordMapper;

    @Mock
    private CpsPlatformClientFactory platformClientFactory;

    @Mock
    private MemberUserService memberUserService;

    @Mock
    private CpsPlatformClient platformClient;

    @Mock
    private CpsCouponInfoVendorClient couponInfoVendorClient;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            java.util.function.Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        }).when(platformClientFactory).withVendorCode(any(), any());
    }

    @Test
    @DisplayName("parseContent - 本地可解析链接时不调用平台解析且不转链")
    void parseContent_usesLocalParserWithoutGeneratingLink() {
        CpsGoodsParseReqVO reqVO = new CpsGoodsParseReqVO();
        reqVO.setPlatformCode("jd");
        reqVO.setOriginalContent("https://item.jd.com/100012043978.html");

        var result = service.parseContent(reqVO);

        assertTrue(result.getSupported());
        assertEquals("jd", result.getPlatformCode());
        assertEquals("100012043978", result.getGoodsId());
        assertEquals("https://item.jd.com/100012043978.html", result.getItemLink());
        assertEquals("local", result.getParseSource());
        assertNull(result.getFailureReason());
        verify(platformClientFactory, never()).getRequiredClient("jd");
        verify(goodsRebateQueryService, never()).queryRebate(any());
    }

    @Test
    @DisplayName("parseContent - 本地不支持口令时回退平台解析")
    void parseContent_fallsBackToPlatformParserForCommand() {
        CpsGoodsParseReqVO reqVO = new CpsGoodsParseReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setOriginalContent("￥abc123￥复制打开淘宝");
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.parseContent(any())).thenReturn(CpsContentParseResult.builder()
                .supported(true)
                .goodsId("123456")
                .itemLink("https://item.taobao.com/item.htm?id=123456")
                .title("平台商品")
                .build());

        var result = service.parseContent(reqVO);

        assertTrue(result.getSupported());
        assertEquals("platform", result.getParseSource());
        assertEquals("123456", result.getGoodsId());
        assertEquals("平台商品", result.getTitle());
        verify(platformClient).parseContent(any());
        verify(goodsRebateQueryService, never()).queryRebate(any());
    }

    @Test
    @DisplayName("parseContent - 指定供应商时平台解析使用临时供应商路由")
    void parseContent_usesExplicitVendorForPlatformParser() {
        CpsGoodsParseReqVO reqVO = new CpsGoodsParseReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setOriginalContent("￥abc123￥复制打开淘宝");
        reqVO.setVendorCode("haodanku");
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.parseContent(any())).thenReturn(CpsContentParseResult.builder()
                .supported(true)
                .goodsId("123456")
                .itemLink("https://item.taobao.com/item.htm?id=123456")
                .title("好单库解析商品")
                .build());

        var result = service.parseContent(reqVO);

        assertTrue(result.getSupported());
        assertEquals("platform", result.getParseSource());
        assertEquals("123456", result.getGoodsId());
        assertEquals("好单库解析商品", result.getTitle());
        verify(platformClientFactory, times(1)).withVendorCode(org.mockito.ArgumentMatchers.eq("haodanku"), any());
        verify(platformClient).parseContent(any());
        verify(goodsRebateQueryService, never()).queryRebate(any());
    }

    @Test
    @DisplayName("batchTransfer - 批量转链保留输入顺序且失败不阻断后续条目")
    void batchTransfer_preservesOrderAndContinuesAfterFailure() {
        CpsGoodsBatchTransferReqVO reqVO = new CpsGoodsBatchTransferReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setMemberId(100L);
        reqVO.setVendorCode("haodanku");
        reqVO.setAdzoneId("mm_1_2_3");
        reqVO.setOriginalContents(List.of(
                "https://item.taobao.com/item.htm?id=111",
                "bad command",
                "https://item.taobao.com/item.htm?id=333"));

        when(goodsRebateQueryService.queryRebate(any()))
                .thenReturn(successResponse("111", 11L))
                .thenReturn(failureResponse("无法识别商品内容"))
                .thenReturn(successResponse("333", 33L));

        var result = service.batchTransfer(reqVO);

        assertEquals(3, result.getItems().size());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("SUCCESS", result.getItems().get(0).getStatus());
        assertEquals(0, result.getItems().get(0).getInputIndex());
        assertEquals("111", result.getItems().get(0).getGoods().getGoodsId());
        assertEquals("PARSE_FAILED", result.getItems().get(1).getStatus());
        assertEquals(1, result.getItems().get(1).getInputIndex());
        assertEquals("无法识别商品内容", result.getItems().get(1).getMessage());
        assertEquals("333", result.getItems().get(2).getGoods().getGoodsId());

        ArgumentCaptor<com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryReqVO> captor =
                ArgumentCaptor.forClass(com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryReqVO.class);
        verify(goodsRebateQueryService, org.mockito.Mockito.times(3)).queryRebate(captor.capture());
        assertEquals("haodanku", captor.getAllValues().get(0).getVendorCode());
        assertEquals("mm_1_2_3", captor.getAllValues().get(0).getAdzoneId());
        assertEquals(100L, captor.getAllValues().get(2).getMemberId());
    }

    @Test
    @DisplayName("batchTransfer - 空白行忽略且超过 20 条报错")
    void batchTransfer_ignoresBlankLinesAndRejectsMoreThanTwentyItems() {
        CpsGoodsBatchTransferReqVO reqVO = new CpsGoodsBatchTransferReqVO();
        reqVO.setPlatformCode("jd");
        reqVO.setMemberId(100L);
        reqVO.setOriginalContents(List.of(" ", "https://item.jd.com/1.html", ""));
        when(goodsRebateQueryService.queryRebate(any())).thenReturn(successResponse("1", 1L));

        var result = service.batchTransfer(reqVO);

        assertEquals(1, result.getItems().size());
        assertEquals("https://item.jd.com/1.html", result.getItems().get(0).getOriginalContent());

        CpsGoodsBatchTransferReqVO tooManyReqVO = new CpsGoodsBatchTransferReqVO();
        tooManyReqVO.setPlatformCode("jd");
        tooManyReqVO.setMemberId(100L);
        tooManyReqVO.setOriginalContents(java.util.stream.IntStream.range(0, 21)
                .mapToObj(index -> "https://item.jd.com/" + index + ".html")
                .toList());

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> service.batchTransfer(tooManyReqVO));
        assertEquals("每次最多支持 20 条内容批量转链", exception.getMessage());
    }

    @Test
    @DisplayName("queryCoupons should use Dataoke coupon API for Taobao content")
    void queryCoupons_usesDataokeCouponInfoApiForTaobao() {
        CpsGoodsCouponQueryReqVO reqVO = new CpsGoodsCouponQueryReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setVendorCode("dataoke");
        reqVO.setQueryText("￥FV25gmAa2SI￥");
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        CpsVendorConfig config = CpsVendorConfig.builder().build();
        when(platformClientFactory.getVendorClient("dataoke", "taobao")).thenReturn(couponInfoVendorClient);
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(config);
        when(couponInfoVendorClient.queryCouponInfo("￥FV25gmAa2SI￥", config))
                .thenReturn(CpsCouponInfo.builder()
                        .couponId("abc")
                        .couponLink("https://uland.taobao.com/quan/detail?activityId=abc")
                        .couponAmount(new BigDecimal("40"))
                        .couponConditions(new BigDecimal("59"))
                        .couponRemainNum(93000L)
                        .couponTotalNum(100000L)
                        .couponReceiveNum(7000L)
                        .couponStartTime("2026-07-01 00:00:00")
                        .couponEndTime("2026-07-31 23:59:59")
                        .build());

        var result = service.queryCoupons(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        CpsGoodsSquareGoodsRespVO goods = result.getList().get(0);
        assertEquals("abc", goods.getGoodsId());
        assertEquals("taobao", goods.getPlatformCode());
        assertEquals("dataoke", goods.getVendorCode());
        assertEquals(new BigDecimal("40"), goods.getCouponPrice());
        assertNull(goods.getActualPrice());
        assertEquals(new BigDecimal("59"), goods.getCouponConditions());
        assertEquals(93000L, goods.getCouponRemainNum());
        assertEquals(100000L, goods.getCouponTotalNum());
        assertEquals(7000L, goods.getCouponReceiveNum());
        assertEquals("2026-07-01 00:00:00", goods.getCouponStartTime());
        assertEquals("https://uland.taobao.com/quan/detail?activityId=abc", goods.getItemLink());
        assertEquals("2026-07-31 23:59:59", goods.getCouponEndTime());
        verify(goodsSquareService, never()).searchGoods(any());
    }

    @Test
    @DisplayName("checkOwnership returns matched transfer record")
    void checkOwnership_returnsMatchedTransferRecord() {
        CpsGoodsOwnershipCheckReqVO reqVO = new CpsGoodsOwnershipCheckReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setOriginalContent("https://item.taobao.com/item.htm?id=111");
        reqVO.setMemberId(100L);
        reqVO.setAdzoneId("mm_1_2_3");
        CpsTransferRecordDO record = CpsTransferRecordDO.builder()
                .id(10L)
                .memberId(100L)
                .platformCode("taobao")
                .itemId("111")
                .itemTitle("测试商品")
                .adzoneId("mm_1_2_3")
                .promotionUrl("https://s.example/111")
                .taoCommand("￥111￥")
                .status(1)
                .build();
        when(transferRecordMapper.selectList(any())).thenReturn(List.of(record));
        when(memberUserService.getUser(100L)).thenReturn(MemberUserDO.builder()
                .id(100L)
                .nickname("张三")
                .mobile("13800138000")
                .build());

        var result = service.checkOwnership(reqVO);

        assertEquals("MATCH", result.getCheckStatus());
        assertEquals("是您的淘口令", result.getOwnershipResult());
        assertEquals(10L, result.getTransferRecordId());
        assertEquals("测试商品", result.getItemTitle());
        assertEquals(100L, result.getRecordMemberId());
        assertEquals("张三", result.getRecordMemberNickname());
        assertEquals("13800138000", result.getRecordMemberMobile());
        assertEquals("mm_1_2_3", result.getPid());
        assertEquals("mm_1_2_3", result.getRecordAdzoneId());
        assertTrue(result.getMismatches().isEmpty());
    }

    @Test
    @DisplayName("checkOwnership - 使用万能转链生成的淘口令能反查转链记录")
    void checkOwnership_findsTransferRecordByGeneratedTaoCommand() {
        CpsGoodsOwnershipCheckReqVO reqVO = new CpsGoodsOwnershipCheckReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setOriginalContent("￥FV25gmAa2SI￥");
        reqVO.setMemberId(285L);

        CpsTransferRecordDO record = CpsTransferRecordDO.builder()
                .id(20L)
                .memberId(285L)
                .platformCode("taobao")
                .originalContent("https://item.taobao.com/item.htm?id=222")
                .itemId("222")
                .itemTitle("万能转链商品")
                .adzoneId("mm_44880323_46012675_109717600050")
                .promotionUrl("https://uland.taobao.com/item/detail?id=222")
                .taoCommand("￥FV25gmAa2SI￥")
                .status(1)
                .build();
        when(transferRecordMapper.selectList(any())).thenReturn(List.of(record));
        when(memberUserService.getUser(285L)).thenReturn(MemberUserDO.builder()
                .id(285L)
                .nickname("我是喵团员")
                .build());

        var result = service.checkOwnership(reqVO);

        assertEquals("MATCH", result.getCheckStatus());
        assertEquals(20L, result.getTransferRecordId());
        assertEquals("万能转链商品", result.getItemTitle());
        assertEquals("mm_44880323_46012675_109717600050", result.getPid());
        assertEquals("我是喵团员", result.getRecordMemberNickname());
    }

    @Test
    @DisplayName("checkOwnership - 记录存在但期望会员不一致时返回不一致项")
    void checkOwnership_returnsMismatchReasons() {
        CpsGoodsOwnershipCheckReqVO reqVO = new CpsGoodsOwnershipCheckReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setOriginalContent("https://item.taobao.com/item.htm?id=111");
        reqVO.setMemberId(200L);
        when(transferRecordMapper.selectList(any())).thenReturn(List.of(CpsTransferRecordDO.builder()
                .id(10L)
                .memberId(100L)
                .platformCode("taobao")
                .itemId("111")
                .status(1)
                .build()));

        var result = service.checkOwnership(reqVO);

        assertEquals("MISMATCH", result.getCheckStatus());
        assertEquals("不是您的淘口令", result.getOwnershipResult());
        assertEquals(List.of("memberId"), result.getMismatches());
    }

    @Test
    @DisplayName("queryCoupons - 优惠券查询复用商品广场且强制只看有券")
    void queryCoupons_reusesGoodsSquareSearchWithCouponFilter() {
        CpsGoodsCouponQueryReqVO reqVO = new CpsGoodsCouponQueryReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setQueryText("零食");
        reqVO.setVendorCode("haodanku");
        reqVO.setCouponAmountMin(new BigDecimal("5"));
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        CpsGoodsSquareGoodsRespVO goods = new CpsGoodsSquareGoodsRespVO();
        goods.setGoodsId("111");
        goods.setTitle("有券商品");
        goods.setCouponPrice(new BigDecimal("6"));
        when(goodsSquareService.searchGoods(any())).thenReturn(CpsGoodsSquareSearchRespVO.builder()
                .list(List.of(goods))
                .total(1L)
                .pageNo(1)
                .pageSize(10)
                .build());

        var result = service.queryCoupons(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("已找到 1 个有券商品", result.getSummary());
        ArgumentCaptor<com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO> captor =
                ArgumentCaptor.forClass(com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO.class);
        verify(goodsSquareService).searchGoods(captor.capture());
        assertEquals(1, captor.getValue().getHasCoupon());
        assertEquals(new BigDecimal("5"), captor.getValue().getCouponAmountMin());
    }

    @Test
    @DisplayName("planCashGift - 生成淘礼金计划并提示预算缺口")
    void planCashGift_calculatesBudgetGap() {
        CpsGoodsCashGiftPlanReqVO reqVO = new CpsGoodsCashGiftPlanReqVO();
        reqVO.setTemplateCode("flash-sale");
        reqVO.setCampaignName("爆品补贴");
        reqVO.setPlatformCode("taobao");
        reqVO.setBudgetAmount(new BigDecimal("80"));
        reqVO.setGiftAmount(new BigDecimal("2"));
        reqVO.setTotalQuantity(50);

        var result = service.planCashGift(reqVO);

        assertEquals("RISK", result.getPlanStatus());
        assertEquals(new BigDecimal("20.00"), result.getBudgetGap());
        assertFalse(result.getBudgetEnough());
        assertFalse(result.getTemplates().isEmpty());
    }

    private CpsGoodsRebateQueryRespVO successResponse(String goodsId, Long transferRecordId) {
        CpsGoodsRebateQueryRespVO response = new CpsGoodsRebateQueryRespVO();
        response.setParseStatus("SUCCESS");
        response.setParseMessage("解析成功");
        CpsGoodsRebateQueryRespVO.Goods goods = new CpsGoodsRebateQueryRespVO.Goods();
        goods.setPlatformCode("taobao");
        goods.setGoodsId(goodsId);
        response.setGoods(goods);
        CpsGoodsRebateQueryRespVO.Links links = new CpsGoodsRebateQueryRespVO.Links();
        links.setShortUrl("https://s.example/" + goodsId);
        links.setTpwd("￥" + goodsId + "￥");
        response.setLinks(links);
        response.setTransferRecordId(transferRecordId);
        return response;
    }

    private CpsGoodsRebateQueryRespVO failureResponse(String message) {
        CpsGoodsRebateQueryRespVO response = new CpsGoodsRebateQueryRespVO();
        response.setParseStatus("PARSE_FAILED");
        response.setParseMessage(message);
        return response;
    }
}
