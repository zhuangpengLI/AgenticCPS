package com.qiji.cps.module.cps.client.haodanku.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HdkActivityClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("generateConferenceLink - 发送淘宝会场官方转链参数并解析链接和淘口令")
    void generateConferenceLink_mapsOfficialRequestAndResponse() throws Exception {
        CapturingClient client = new CapturingClient(OBJECT_MAPPER.readTree("""
                {"code":1,"data":{"url":"https://s.click.taobao.com/hdk","tao_code":"￥HDK123￥"}}
                """));
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("20150318020023228");
        request.setItemLink("https://pages.tmall.com/wow/activity");
        request.setAdzoneId("mm_1_2_3");
        request.setRelationId("verified-relation-1");
        CpsVendorConfig config = CpsVendorConfig.builder()
                .appKey("api-key")
                .authToken("authorized-tb-name")
                .build();

        CpsPromotionLinkResult result = client.generateConferenceLink(request, config, "淘宝官方超级补贴会场");

        assertEquals("api-key", client.params.get("apikey"));
        assertEquals("20150318020023228", client.params.get("activity_id"));
        assertFalse(client.params.containsKey("activity_url"));
        assertEquals("mm_1_2_3", client.params.get("pid"));
        assertEquals("authorized-tb-name", client.params.get("tb_name"));
        assertEquals("淘宝官方超级补贴会场", client.params.get("title"));
        assertEquals("verified-relation-1", client.params.get("relation_id"));
        assertEquals("https://s.click.taobao.com/hdk", result.getShortUrl());
        assertEquals("￥HDK123￥", result.getTpwd());
    }

    @Test
    @DisplayName("generateConferenceLink - 仅有会场地址时仍发送淘宝授权名")
    void generateConferenceLink_usesActivityUrlAsAlternativeToActivityId() throws Exception {
        CapturingClient client = new CapturingClient(OBJECT_MAPPER.readTree("""
                {"code":1,"data":{"url":"https://s.click.taobao.com/hdk-url"}}
                """));
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setItemLink("https://pages.tmall.com/wow/activity");
        request.setAdzoneId("mm_1_2_3");
        CpsVendorConfig config = CpsVendorConfig.builder()
                .appKey("api-key")
                .authToken("authorized-tb-name")
                .build();

        CpsPromotionLinkResult result = client.generateConferenceLink(request, config, "淘宝官方超级补贴会场");

        assertFalse(client.params.containsKey("activity_id"));
        assertEquals("https://pages.tmall.com/wow/activity", client.params.get("activity_url"));
        assertEquals("authorized-tb-name", client.params.get("tb_name"));
        assertEquals("https://s.click.taobao.com/hdk-url", result.getShortUrl());
    }

    @Test
    @DisplayName("generateConferenceLink - 缺少淘宝授权名时拒绝调用上游")
    void generateConferenceLink_rejectsMissingTbName() throws Exception {
        CapturingClient client = new CapturingClient(OBJECT_MAPPER.readTree("""
                {"code":1,"data":{"url":"https://s.click.taobao.com/hdk-url-fallback"}}
                """));
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("20150318020023228");
        request.setItemLink("https://pages.tmall.com/wow/activity");
        request.setAdzoneId("mm_1_2_3");
        CpsVendorConfig config = CpsVendorConfig.builder()
                .appKey("api-key")
                .build();

        CpsPromotionLinkResult result = client.generateConferenceLink(request, config, "淘宝官方超级补贴会场");

        assertNull(result);
        assertTrue(client.params.isEmpty());
    }

    @Test
    @DisplayName("generateConferenceLink - 不把普通渠道标签映射为 relation_id")
    void generateConferenceLink_doesNotUseExternalIdAsRelationId() throws Exception {
        CapturingClient client = new CapturingClient(OBJECT_MAPPER.readTree("""
                {"code":1,"data":{"url":"https://s.click.taobao.com/hdk"}}
                """));
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("10001");
        request.setAdzoneId("mm_1_2_3");
        request.setExternalId("wechat_group_a");
        CpsVendorConfig config = CpsVendorConfig.builder()
                .appKey("api-key")
                .authToken("authorized-tb-name")
                .build();

        client.generateConferenceLink(request, config, "淘宝官方超级补贴会场");

        assertFalse(client.params.containsKey("relation_id"));
    }

    @Test
    @DisplayName("generateConferenceLink - 配置不完整或上游无链接时返回空结果")
    void generateConferenceLink_returnsEmptyResultWhenUnavailable() throws Exception {
        CapturingClient client = new CapturingClient(OBJECT_MAPPER.readTree("""
                {"code":1,"data":{}}
                """));
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("10001");

        CpsPromotionLinkResult missingConfig = client.generateConferenceLink(request,
                CpsVendorConfig.builder().build(), "淘宝官方超级补贴会场");
        assertNull(missingConfig);

        request.setAdzoneId("mm_1_2_3");
        CpsPromotionLinkResult blankUrl = client.generateConferenceLink(request,
                CpsVendorConfig.builder().appKey("api-key").authToken("authorized-tb-name").build(),
                "淘宝官方超级补贴会场");
        assertNull(blankUrl);
    }

    @Test
    @DisplayName("generateElemeActivityLink - 发送合规 sid 并解析闪购推广载体")
    void generateElemeActivityLink_mapsOfficialRequestAndResponse() throws Exception {
        CapturingClient client = new CapturingClient(OBJECT_MAPPER.readTree("""
                {"code":200,"data":{
                  "h5_short_link":"https://s.click.ele.me/short",
                  "h5_url":"https://s.click.ele.me/long",
                  "full_taobao_word":"淘宝闪购官方活动口令",
                  "mini_qrcode":"https://img.example/mini.png",
                  "wx_appid":"wx123",
                  "wx_path":"pages/index/index",
                  "tb_scheme_url":"tbopen://activity",
                  "ele_scheme_url":"eleme://activity",
                  "alipay_mini_url":"alipays://activity"
                }}
                """));
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("activity-1001");
        request.setItemLink("https://market.m.taobao.com/app/eleme/activity");

        CpsPromotionLinkResult result = client.generateElemeActivityLink(request,
                CpsVendorConfig.builder().appKey("api-key").build(), "Abc_123456789");

        assertEquals("api-key", client.elemeParams.get("apikey"));
        assertEquals("activity-1001", client.elemeParams.get("activity_id"));
        assertEquals("https://market.m.taobao.com/app/eleme/activity", client.elemeParams.get("link"));
        assertEquals("Abc_123456789", client.elemeParams.get("sid"));
        assertEquals("https://s.click.ele.me/short", result.getShortUrl());
        assertEquals("https://s.click.ele.me/long", result.getLongUrl());
        assertEquals("淘宝闪购官方活动口令", result.getTpwd());
        assertEquals("eleme://activity", result.getMobileUrl());
        assertEquals("https://img.example/mini.png", result.getExtraFields().get("miniQrcode"));
        assertEquals("wx123", result.getExtraFields().get("wxAppId"));
        assertEquals("pages/index/index", result.getExtraFields().get("wxPath"));
        assertEquals("tbopen://activity", result.getExtraFields().get("taobaoSchemeUrl"));
        assertEquals("alipays://activity", result.getExtraFields().get("alipayMiniUrl"));
    }

    @Test
    @DisplayName("generateElemeActivityLink - H5 为空时保留官方返回的淘口令和 Scheme")
    void generateElemeActivityLink_acceptsSchemeAndTpwdWhenH5Missing() throws Exception {
        CapturingClient client = new CapturingClient(OBJECT_MAPPER.readTree("""
                {"code":200,"msg":"success","data":{
                  "h5_short_link":"",
                  "h5_url":"",
                  "full_taobao_word":"闪购品牌日官方淘口令",
                  "tb_scheme_url":"tbopen://m.taobao.com/activity",
                  "ele_scheme_url":"eleme://miniapp/activity"
                }}
                """));
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("12698");

        CpsPromotionLinkResult result = client.generateElemeActivityLink(request,
                CpsVendorConfig.builder().appKey("api-key").build(), null);

        assertNotNull(result);
        assertNull(result.getShortUrl());
        assertNull(result.getLongUrl());
        assertEquals("闪购品牌日官方淘口令", result.getTpwd());
        assertEquals("eleme://miniapp/activity", result.getMobileUrl());
        assertEquals("tbopen://m.taobao.com/activity", result.getExtraFields().get("taobaoSchemeUrl"));
    }

    @Test
    @DisplayName("generateElemeActivityLink - 拒绝暴露会员信息或格式不合规的 sid")
    void generateElemeActivityLink_rejectsInvalidSid() throws Exception {
        CapturingClient client = new CapturingClient(OBJECT_MAPPER.readTree("""
                {"code":200,"data":{"h5_short_link":"https://s.click.ele.me/short"}}
                """));
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();

        assertNull(client.generateElemeActivityLink(request,
                CpsVendorConfig.builder().appKey("api-key").build(), "member-10001"));
        assertNull(client.generateElemeActivityLink(request,
                CpsVendorConfig.builder().appKey("api-key").build(), "1234567890123456"));
        assertTrue(client.elemeParams.isEmpty());
    }

    private static final class CapturingClient extends HdkActivityClient {
        private final JsonNode response;
        private Map<String, Object> params = new LinkedHashMap<>();
        private Map<String, Object> elemeParams = new LinkedHashMap<>();

        private CapturingClient(JsonNode response) {
            this.response = response;
        }

        @Override
        protected JsonNode executeConferenceRequest(Map<String, Object> params) {
            this.params = new LinkedHashMap<>(params);
            return response;
        }

        @Override
        protected JsonNode executeElemeActivityRequest(Map<String, Object> params) {
            this.elemeParams = new LinkedHashMap<>(params);
            return response;
        }
    }
}
