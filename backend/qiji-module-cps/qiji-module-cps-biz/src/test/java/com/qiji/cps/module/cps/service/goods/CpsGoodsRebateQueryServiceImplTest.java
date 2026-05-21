package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsContentParseResult;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryReqVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CpsGoodsRebateQueryServiceImplTest {

    @InjectMocks
    private CpsGoodsRebateQueryServiceImpl service;

    @Mock
    private CpsGoodsService goodsService;

    @Mock
    private CpsPlatformClientFactory platformClientFactory;

    @Mock
    private CpsPlatformClient platformClient;

    @Mock
    private CpsTransferRecordMapper transferRecordMapper;

    @Test
    @DisplayName("queryRebate - 链接解析后使用指定会员和默认推广位转链并写入转链记录")
    void queryRebate_generatesLinkWithMemberAndWritesTransferRecord() {
        CpsGoodsRebateQueryReqVO reqVO = new CpsGoodsRebateQueryReqVO();
        reqVO.setPlatformCode("jd");
        reqVO.setOriginalContent("https://item.jd.com/100012043978.html");
        reqVO.setMemberId(100L);

        when(goodsService.resolvePromotionAdzoneId("jd", 100L, null)).thenReturn("jd-default-pid");
        when(goodsService.generatePromotionLink("jd", "100012043978", null, 100L, "jd-default-pid"))
                .thenReturn(CpsPromotionLinkResult.builder()
                        .shortUrl("https://u.jd.com/short")
                        .longUrl("https://union.jd.com/long")
                        .actualPrice(new BigDecimal("88.00"))
                        .commissionRate(new BigDecimal("10.00"))
                        .commissionAmount(new BigDecimal("8.80"))
                        .couponInfo("满100减12")
                        .build());
        when(transferRecordMapper.insert(any(CpsTransferRecordDO.class))).thenAnswer(invocation -> {
            CpsTransferRecordDO record = invocation.getArgument(0);
            record.setId(999L);
            return 1;
        });

        var response = service.queryRebate(reqVO);

        assertEquals("SUCCESS", response.getParseStatus());
        assertEquals("解析成功", response.getParseMessage());
        assertEquals(999L, response.getTransferRecordId());
        assertEquals("100012043978", response.getGoods().getGoodsId());
        assertEquals("https://item.jd.com/100012043978.html", response.getGoods().getItemLink());
        assertEquals(new BigDecimal("8.80"), response.getRebate().getEstimateRebateAmount());
        assertEquals("jd-default-pid", response.getRebate().getUsedAdzoneId());
        assertEquals("https://u.jd.com/short", response.getLinks().getShortUrl());

        verify(goodsService).generatePromotionLink("jd", "100012043978", null, 100L, "jd-default-pid");
        ArgumentCaptor<CpsTransferRecordDO> recordCaptor = ArgumentCaptor.forClass(CpsTransferRecordDO.class);
        verify(transferRecordMapper).insert(recordCaptor.capture());
        CpsTransferRecordDO record = recordCaptor.getValue();
        assertEquals(100L, record.getMemberId());
        assertEquals("jd", record.getPlatformCode());
        assertEquals("100012043978", record.getItemId());
        assertEquals("https://item.jd.com/100012043978.html", record.getOriginalContent());
        assertEquals("https://u.jd.com/short", record.getPromotionUrl());
        assertEquals("jd-default-pid", record.getAdzoneId());
        assertEquals(1, record.getStatus());
    }

    @Test
    @DisplayName("queryRebate - 口令 URL 解析失败时调用平台解析能力并正常转链")
    void queryRebate_usesPlatformParseFallbackForCommand() {
        CpsGoodsRebateQueryReqVO reqVO = new CpsGoodsRebateQueryReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setOriginalContent("￥abc123￥复制打开淘宝");
        reqVO.setMemberId(100L);
        reqVO.setAdzoneId("mm_1_2_3");

        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.parseContent(any())).thenReturn(CpsContentParseResult.builder()
                .supported(true)
                .goodsId("123456")
                .itemLink("https://item.taobao.com/item.htm?id=123456")
                .title("平台解析商品")
                .build());
        when(goodsService.resolvePromotionAdzoneId("taobao", 100L, "mm_1_2_3")).thenReturn("mm_1_2_3");
        when(goodsService.generatePromotionLink("taobao", "123456", null, 100L, "mm_1_2_3"))
                .thenReturn(CpsPromotionLinkResult.builder()
                        .tpwd("￥newcmd￥")
                        .commissionAmount(new BigDecimal("3.20"))
                        .build());

        var response = service.queryRebate(reqVO);

        assertEquals("SUCCESS", response.getParseStatus());
        assertEquals("平台解析商品", response.getGoods().getTitle());
        assertEquals("￥newcmd￥", response.getLinks().getTpwd());
        verify(platformClient).parseContent(argThat(request ->
                "taobao".equals(request.getPlatformCode())
                        && "￥abc123￥复制打开淘宝".equals(request.getOriginalContent())));
    }

    @Test
    @DisplayName("queryRebate - 解析失败时不转链且返回失败原因")
    void queryRebate_returnsParseFailureWithoutGeneratingLink() {
        CpsGoodsRebateQueryReqVO reqVO = new CpsGoodsRebateQueryReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setOriginalContent("￥abc123￥复制打开淘宝");
        reqVO.setMemberId(100L);

        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.parseContent(any())).thenReturn(CpsContentParseResult.unsupported(
                "COMMAND_UNSUPPORTED", "暂不支持该渠道口令自动解析，请粘贴商品链接或商品ID"));

        var response = service.queryRebate(reqVO);

        assertEquals("PARSE_FAILED", response.getParseStatus());
        assertEquals("暂不支持该渠道口令自动解析，请粘贴商品链接或商品ID", response.getParseMessage());
        assertNull(response.getLinks());
        verify(goodsService, never()).generatePromotionLink(anyString(), any(), any(), any(), any());
        verify(transferRecordMapper, never()).insert(any());
    }
}
