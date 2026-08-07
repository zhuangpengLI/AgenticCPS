package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsSearchGoodsToolFunctionTest {

    @InjectMocks
    private CpsSearchGoodsToolFunction toolFunction;

    @Mock
    private CpsGoodsService goodsService;

    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    void apply_balancesAllPlatformTopResultsAndReportsCandidateCounts() {
        when(goodsService.searchGoodsAllPlatforms(any())).thenReturn(List.of(
                item("pdd-1", "pdd", "0.03"),
                item("pdd-2", "pdd", "0.06"),
                item("pdd-3", "pdd", "0.07"),
                item("pdd-4", "pdd", "0.08"),
                item("pdd-5", "pdd", "0.09"),
                item("tb-1", "taobao", "9.90"),
                item("tb-2", "taobao", "12.90"),
                item("jd-1", "jd", "15.90")
        ));
        CpsSearchGoodsToolFunction.Request request = new CpsSearchGoodsToolFunction.Request();
        request.setKeyword("毛巾");
        request.setPageSize(5);

        CpsSearchGoodsToolFunction.Response response = toolFunction.apply(request);

        assertNull(response.getError());
        assertEquals(8, response.getTotal());
        assertEquals(5, response.getReturned());
        assertEquals(Map.of("jd", 1, "pdd", 5, "taobao", 2), response.getPlatformCounts());
        assertEquals(List.of("pdd", "pdd", "pdd", "taobao", "jd"),
                response.getGoods().stream().map(CpsSearchGoodsToolFunction.Response.GoodsVO::getPlatformCode).toList());
        assertEquals(List.of("拼多多", "拼多多", "拼多多", "淘宝", "京东"),
                response.getGoods().stream().map(CpsSearchGoodsToolFunction.Response.GoodsVO::getPlatformName).toList());
        assertEquals("全平台均衡展示：已优先覆盖有结果的平台，再按券后价补足并升序排列", response.getSelectionNote());
    }

    @Test
    void apply_preservesVendorAndItemLinkForSearchToLinkRouting() {
        CpsGoodsItem item = item("tb-1", "taobao", "9.90");
        item.setVendorCode("dataoke");
        item.setItemLink("https://item.taobao.com/item.htm?id=1");
        when(goodsService.searchGoodsAllPlatforms(any())).thenReturn(List.of(item));
        CpsSearchGoodsToolFunction.Request request = new CpsSearchGoodsToolFunction.Request();
        request.setKeyword("毛巾");

        var response = toolFunction.apply(request);

        assertEquals("dataoke", response.getGoods().get(0).getVendorCode());
        assertEquals("https://item.taobao.com/item.htm?id=1", response.getGoods().get(0).getItemLink());
    }

    private CpsGoodsItem item(String goodsId, String platformCode, String actualPrice) {
        return CpsGoodsItem.builder()
                .goodsId(goodsId)
                .platformCode(platformCode)
                .title(goodsId)
                .actualPrice(new BigDecimal(actualPrice))
                .build();
    }

}
