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
    @DisplayName("parse - 淘宝 uland 商品地址解析加密 id")
    void parse_taobaoUlandItemDetailUrl() {
        CpsContentParseResult result = CpsContentParser.parse("taobao",
                "https://uland.taobao.com/item/detail?id=vNYO4nJhZt6weZ8o48I9rVT0te-vdGJZ");

        assertTrue(result.getSupported());
        assertEquals("vNYO4nJhZt6weZ8o48I9rVT0te-vdGJZ", result.getGoodsId());
        assertEquals("https://uland.taobao.com/item/detail?id=vNYO4nJhZt6weZ8o48I9rVT0te-vdGJZ", result.getItemLink());
    }

    @Test
    @DisplayName("parse - 淘宝 uland 优惠券地址保留券链接")
    void parse_taobaoUlandQuanDetailUrl() {
        CpsContentParseResult result = CpsContentParser.parse("taobao",
                "https://uland.taobao.com/quan/detail?sellerId=4757067876244917769&activityId=abc123");

        assertTrue(result.getSupported());
        assertNull(result.getGoodsId());
        assertEquals("https://uland.taobao.com/quan/detail?sellerId=4757067876244917769&activityId=abc123", result.getCouponLink());
        assertNull(result.getItemLink());
    }

    @Test
    @DisplayName("parse - 淘宝 uland 优惠券地址无 activityId 时仍保留券地址")
    void parse_taobaoUlandQuanDetailUrlWithoutActivityId() {
        CpsContentParseResult result = CpsContentParser.parse("taobao",
                "https://uland.taobao.com/quan/detail?sellerId=4757067876244917769");

        assertTrue(result.getSupported());
        assertNull(result.getGoodsId());
        assertEquals("https://uland.taobao.com/quan/detail?sellerId=4757067876244917769", result.getCouponLink());
        assertNull(result.getItemLink());
    }

    @Test
    @DisplayName("parse - 淘宝二合一长链解析 targetUrl 内商品 id")
    void parse_taobaoAccurateReturnTargetUrl() {
        CpsContentParseResult result = CpsContentParser.parse("taobao",
                "https://mos.m.taobao.com/union/accurate-return?targetUrl=https%3A%2F%2Fuland.taobao.com%2Fitem%2Fdetail%3Fid%3DtargetGoods123");

        assertTrue(result.getSupported());
        assertEquals("targetGoods123", result.getGoodsId());
        assertEquals("https://uland.taobao.com/item/detail?id=targetGoods123", result.getItemLink());
        assertEquals("https://mos.m.taobao.com/union/accurate-return?targetUrl=https%3A%2F%2Fuland.taobao.com%2Fitem%2Fdetail%3Fid%3DtargetGoods123", result.getSourceLink());
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
