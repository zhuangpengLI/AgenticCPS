package com.qiji.cps.module.cps.client.official.jd;

import com.jd.open.api.sdk.JdClient;
import com.jd.open.api.sdk.request.JdRequest;
import com.jd.open.api.sdk.response.AbstractResponse;
import com.qiji.cps.module.cps.client.CpsCouponInfoVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdOfficialVendorClientTest {

    @Test
    void routesGoodsModesToTheOfficialSdkEndpoints() {
        CapturingClient client = new CapturingClient();
        TestJdOfficialVendorClient vendor = new TestJdOfficialVendorClient(client);

        CpsGoodsSearchRequest keyword = request(null);
        vendor.searchGoods(keyword, config(Map.of()));
        assertEquals("jd.union.open.goods.query", client.lastMethod());

        CpsGoodsSearchRequest jingfen = request("jingfen");
        vendor.searchGoods(jingfen, config(Map.of("jdEliteId", "1")));
        assertEquals("jd.union.open.goods.jingfen.query", client.lastMethod());

        CpsGoodsSearchRequest recommend = request("recommend");
        recommend.setKeyword("100012043978");
        vendor.searchGoods(recommend, config(Map.of("jdRecommendSceneId", "1")));
        assertEquals("jd.union.open.goods.recommend.query", client.lastMethod());
    }

    @Test
    void routesPromotionModesWithoutLosingAttributionParameters() {
        CapturingClient client = new CapturingClient();
        TestJdOfficialVendorClient vendor = new TestJdOfficialVendorClient(client);
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("100012043978");
        request.setAdzoneId("1001_2002_3003");
        request.setExternalId("member-42");

        vendor.generatePromotionLink(request, config(Map.of("promotionMode", "common")));
        assertEquals("jd.union.open.promotion.common.get", client.lastMethod());

        vendor.generatePromotionLink(request, config(Map.of("promotionMode", "social")));
        assertEquals("jd.union.open.promotion.bysubunionid.get", client.lastMethod());

        vendor.generatePromotionLink(request, config(Map.of("promotionMode", "tool", "unionId", "88")));
        assertEquals("jd.union.open.promotion.byunionid.get", client.lastMethod());
    }

    @Test
    void exposesCouponQueryThroughTheExistingToolboxContract() {
        CapturingClient client = new CapturingClient();
        TestJdOfficialVendorClient vendor = new TestJdOfficialVendorClient(client);

        vendor.queryCouponInfo("https://coupon.m.jd.com/coupons/show.action?key=test", config(Map.of()));

        assertInstanceOf(CpsCouponInfoVendorClient.class, vendor);
        assertEquals("jd.union.open.coupon.query", client.lastMethod());
        assertTrue(vendor.getCapabilities().stream().anyMatch(capability -> "coupon_query".equals(capability.getCode())));
    }

    @Test
    void routesRemotePositionPidAndChannelManagementThroughTheOfficialSdk() {
        CapturingClient client = new CapturingClient();
        TestJdOfficialVendorClient vendor = new TestJdOfficialVendorClient(client);
        CpsVendorConfig config = config(Map.of("unionId", "88"));

        vendor.createPositions(new JdOfficialManagementClient.CreatePositionCommand(
                88L, null, 1, 1, 1001L, List.of("系统推广位")), config);
        assertEquals("jd.union.open.position.create", client.lastMethod());

        vendor.queryPositions(new JdOfficialManagementClient.QueryPositionCommand(
                88L, null, 1, 1, 20), config);
        assertEquals("jd.union.open.position.query", client.lastMethod());

        vendor.getPid(new JdOfficialManagementClient.PidCommand(
                88L, null, 1, "系统推广位", "AgenticCPS"), config);
        assertEquals("jd.union.open.user.pid.get", client.lastMethod());

        vendor.createChannelRelation(new JdOfficialManagementClient.ChannelRelationCommand(
                "invite-code", "member-42", "AgenticCPS"), config);
        assertEquals("jd.union.open.channel.relation.get", client.lastMethod());
    }

    @Test
    void queriesBonusOrdersWhenTheVendorConfigEnablesThem() {
        CapturingClient client = new CapturingClient();
        TestJdOfficialVendorClient vendor = new TestJdOfficialVendorClient(client);
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setPageNo(1);
        request.setPageSize(20);
        request.setQueryType(1);
        request.setStartTime("2026-08-14 00:00:00");
        request.setEndTime("2026-08-14 01:00:00");

        vendor.queryOrderPage(request, config(Map.of("includeBonusOrders", "true")));

        assertEquals(List.of("jd.union.open.order.row.query", "jd.union.open.order.bonus.query"),
                client.methods());
    }

    @Test
    void routesJdMarketingAndEffectApisThroughTheOfficialSdk() {
        CapturingClient client = new CapturingClient();
        TestJdOfficialVendorClient vendor = new TestJdOfficialVendorClient(client);
        CpsVendorConfig config = config(Map.of());

        vendor.createGiftCoupon(Map.of("skuMaterialId", "100012043978"), config);
        vendor.stopGiftCoupon(Map.of("giftCouponKey", "gift-key"), config);
        vendor.queryGiftCouponEffect(Map.of("giftCouponKey", "gift-key"), config);
        vendor.queryRedPacketEffect(Map.of("pageIndex", 1), config);
        vendor.queryPromotionEffect(Map.of("itemId", "100012043978"), config);

        assertEquals(List.of(
                        "jd.union.open.coupon.gift.get",
                        "jd.union.open.coupon.gift.stop",
                        "jd.union.open.statistics.giftcoupon.query",
                        "jd.union.open.statistics.redpacket.query",
                        "jd.union.open.statistics.promotion.query"),
                client.methods());
    }

    private CpsGoodsSearchRequest request(String mode) {
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setSearchMode(mode);
        request.setKeyword("手机");
        request.setPageNo(1);
        request.setPageSize(10);
        return request;
    }

    private CpsVendorConfig config(Map<String, String> extraConfig) {
        return CpsVendorConfig.builder()
                .appKey("app-key")
                .appSecret("app-secret")
                .defaultAdzoneId("1001_2002_3003")
                .extraConfig(extraConfig)
                .build();
    }

    private static final class TestJdOfficialVendorClient extends JdOfficialVendorClient {
        private final JdClient client;

        private TestJdOfficialVendorClient(JdClient client) {
            this.client = client;
        }

        @Override
        protected JdClient createClient(CpsVendorConfig config) {
            return client;
        }
    }

    private static final class CapturingClient implements JdClient {
        private final List<String> methods = new ArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T extends AbstractResponse> T execute(JdRequest<T> request) throws Exception {
            methods.add(request.getApiMethod());
            T response = request.getResponseClass().getDeclaredConstructor().newInstance();
            response.setCode("0");
            return response;
        }

        @Override
        public <T extends AbstractResponse> String executeToString(JdRequest<T> request) {
            methods.add(request.getApiMethod());
            return "{}";
        }

        private String lastMethod() {
            return methods.get(methods.size() - 1);
        }

        private List<String> methods() {
            return List.copyOf(methods);
        }
    }
}
