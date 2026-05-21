package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.dto.CpsContentParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpsContentParserTest {

    @Test
    @DisplayName("parse - 淘宝商品链接解析 item id")
    void parse_taobaoUrl() {
        CpsContentParseResult result = CpsContentParser.parse("taobao",
                "https://item.taobao.com/item.htm?id=123456789&skuId=1");

        assertTrue(result.getSupported());
        assertEquals("123456789", result.getGoodsId());
        assertEquals("https://item.taobao.com/item.htm?id=123456789&skuId=1", result.getItemLink());
    }

    @Test
    @DisplayName("parse - 京东商品链接解析 sku id")
    void parse_jdUrl() {
        CpsContentParseResult result = CpsContentParser.parse("jd",
                "https://item.jd.com/100012043978.html");

        assertTrue(result.getSupported());
        assertEquals("100012043978", result.getGoodsId());
        assertEquals("https://item.jd.com/100012043978.html", result.getItemLink());
    }

    @Test
    @DisplayName("parse - 拼多多商品链接解析 goods_id")
    void parse_pddUrl() {
        CpsContentParseResult result = CpsContentParser.parse("pdd",
                "https://mobile.yangkeduo.com/goods.html?goods_id=987654321");

        assertTrue(result.getSupported());
        assertEquals("987654321", result.getGoodsId());
        assertEquals("987654321", result.getGoodsSign());
    }

    @Test
    @DisplayName("parse - 抖音商品链接解析 product id")
    void parse_douyinUrl() {
        CpsContentParseResult result = CpsContentParser.parse("douyin",
                "https://haohuo.jinritemai.com/views/product/item2?id=11223344");

        assertTrue(result.getSupported());
        assertEquals("11223344", result.getGoodsId());
    }

    @Test
    @DisplayName("parse - 纯商品 ID 可作为兜底输入")
    void parse_plainGoodsId() {
        CpsContentParseResult result = CpsContentParser.parse("jd", "100012043978");

        assertTrue(result.getSupported());
        assertEquals("100012043978", result.getGoodsId());
        assertNull(result.getItemLink());
    }

    @Test
    @DisplayName("parse - 口令解析失败时返回可读失败原因")
    void parse_commandUnsupported() {
        CpsContentParseResult result = CpsContentParser.parse("taobao", "￥abc123￥复制打开淘宝");

        assertFalse(result.getSupported());
        assertEquals("COMMAND_UNSUPPORTED", result.getFailureCode());
        assertEquals("暂不支持该渠道口令自动解析，请粘贴商品链接或商品ID", result.getFailureReason());
    }
}
