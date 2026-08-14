package com.qiji.cps.module.cps.client;

import com.qiji.cps.module.cps.client.didi.DidiOfficialVendorClient;
import com.qiji.cps.module.cps.client.didi.DidiPlatformClientAdapter;
import com.qiji.cps.module.cps.client.didi.DidiUnionClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.official.douyin.DouyinOfficialVendorClient;
import com.qiji.cps.module.cps.client.official.jd.JdOfficialVendorClient;
import com.qiji.cps.module.cps.client.official.pdd.PddOfficialVendorClient;
import com.qiji.cps.module.cps.client.official.taobao.TaobaoOfficialVendorClient;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class CpsSdkStandardizationContractTest {

    @Test
    void vendorDescriptor_exposesCapabilitiesAndConfigSchema() {
        CpsApiVendorClient vendor = new StubVendorClient("dataoke", "taobao");

        CpsVendorDescriptor descriptor = vendor.describe();

        assertEquals("dataoke", descriptor.getVendorCode());
        assertEquals("taobao", descriptor.getPlatformCode());
        assertTrue(descriptor.getCapabilities().contains(CpsVendorCapability.GOODS_SEARCH));
        assertTrue(descriptor.getCapabilities().contains(CpsVendorCapability.PROMOTION_LINK));
        assertTrue(descriptor.getCapabilities().contains(CpsVendorCapability.ORDER_QUERY));
        assertTrue(descriptor.getConfigSchema().validate(CpsVendorConfig.builder()
                .appKey("app-key")
                .appSecret("app-secret")
                .apiBaseUrl("https://vendor.example/api")
                .build()).isValid());
        assertFalse(descriptor.getConfigSchema().validate(CpsVendorConfig.builder()
                .appKey("app-key")
                .build()).isValid());
        assertEquals("***", descriptor.getConfigSchema().maskedSummary(CpsVendorConfig.builder()
                .appKey("app-key")
                .appSecret("secret-value")
                .build()).get("appSecret"));
    }

    @Test
    void vendorDescriptor_exposesConnectionGovernancePolicy() {
        CpsVendorDescriptor descriptor = new StubVendorClient("dataoke", "taobao").describe();

        CpsVendorGovernancePolicy governance = descriptor.getGovernancePolicy();

        assertEquals(5_000, governance.getTimeoutMillis());
        assertEquals(2, governance.getRetryPolicy().getMaxAttempts());
        assertEquals(100, governance.getRetryPolicy().getInitialBackoffMillis());
        assertEquals(10_000, governance.getCircuitBreakerOpenMillis());
        assertEquals(60, governance.getRateLimitPerMinute());
        assertFalse(governance.isTokenRefreshSupported());
        assertTrue(governance.isMetricsEnabled());
        assertTrue(governance.isMaskedDiagnosticsEnabled());
    }

    @Test
    void factoryListsRegisteredVendorCapabilityMatrix() throws Exception {
        CpsPlatformClientFactory factory = new CpsPlatformClientFactory();
        var vendorClientsField = CpsPlatformClientFactory.class.getDeclaredField("vendorClients");
        vendorClientsField.setAccessible(true);
        vendorClientsField.set(factory, List.of(
                new StubVendorClient("dataoke", "taobao"),
                new StubVendorClient("haodanku", "jd")
        ));
        factory.init();

        List<CpsVendorDescriptor> descriptors = factory.getRegisteredVendorDescriptors();

        assertEquals(2, descriptors.size());
        assertTrue(descriptors.stream().anyMatch(descriptor ->
                descriptor.getVendorCode().equals("dataoke")
                        && descriptor.getPlatformCode().equals("taobao")
                        && descriptor.getCapabilities().contains(CpsVendorCapability.ORDER_QUERY)));
    }

    @Test
    void unsupportedCapabilityUsesStructuredVendorException() {
        DidiOfficialVendorClient vendor = new DidiOfficialVendorClient(mock(DidiUnionClientFactory.class));
        DidiPlatformClientAdapter adapter = new DidiPlatformClientAdapter(mock(CpsPlatformClientFactory.class));

        assertFalse(vendor.getCapabilities().contains(CpsVendorCapability.GOODS_SEARCH));

        CpsVendorException vendorException = assertThrows(CpsVendorException.class,
                () -> vendor.searchGoods(new CpsGoodsSearchRequest(), CpsVendorConfig.builder().build()));
        assertEquals("CAPABILITY_UNSUPPORTED", vendorException.getCode());
        assertEquals("official", vendorException.getVendorCode());
        assertEquals("didi", vendorException.getPlatformCode());
        assertEquals(CpsVendorCapability.GOODS_SEARCH, vendorException.getCapability());

        CpsVendorException platformException = assertThrows(CpsVendorException.class,
                () -> adapter.searchGoods(new CpsGoodsSearchRequest()));
        assertEquals("CAPABILITY_UNSUPPORTED", platformException.getCode());
        assertEquals("didi", platformException.getPlatformCode());
        assertEquals(CpsVendorCapability.GOODS_SEARCH, platformException.getCapability());
    }

    @Test
    void defaultCapabilitySetCanBeExtendedBySpecializedInterfaces() {
        CpsCouponInfoVendorClient vendor = new CouponVendor();

        assertTrue(vendor.getCapabilities().containsAll(Set.of(
                CpsVendorCapability.GOODS_SEARCH,
                CpsVendorCapability.COUPON_QUERY)));
    }

    @Test
    void officialSkeletonsRemainLimitedAndMigratedJdExposesBusinessCapabilities() {
        List<CpsApiVendorClient> skeletons = List.of(
                new TaobaoOfficialVendorClient(),
                new PddOfficialVendorClient(),
                new DouyinOfficialVendorClient()
        );

        for (CpsApiVendorClient skeleton : skeletons) {
            assertEquals(Set.of(CpsVendorCapability.CONNECTION_TEST), skeleton.getCapabilities(),
                    skeleton.getPlatformCode());
        }

        CpsApiVendorClient jd = new JdOfficialVendorClient();
        assertTrue(jd.getCapabilities().containsAll(Set.of(
                CpsVendorCapability.GOODS_SEARCH,
                CpsVendorCapability.PROMOTION_LINK,
                CpsVendorCapability.ORDER_QUERY,
                CpsVendorCapability.CONNECTION_TEST)));
    }

    private static final class CouponVendor extends StubVendorClient implements CpsCouponInfoVendorClient {

        private CouponVendor() {
            super("dataoke", "taobao");
        }

        @Override
        public com.qiji.cps.module.cps.client.dto.CpsCouponInfo queryCouponInfo(
                String content, CpsVendorConfig config) {
            return null;
        }
    }

    private static class StubVendorClient implements CpsApiVendorClient {
        private final String vendorCode;
        private final String platformCode;

        StubVendorClient(String vendorCode, String platformCode) {
            this.vendorCode = vendorCode;
            this.platformCode = platformCode;
        }

        @Override
        public String getVendorCode() {
            return vendorCode;
        }

        @Override
        public String getPlatformCode() {
            return platformCode;
        }

        @Override
        public String getVendorType() {
            return "aggregator";
        }

        @Override
        public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request, CpsVendorConfig config) {
            return null;
        }

        @Override
        public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request, CpsVendorConfig config) {
            return null;
        }

        @Override
        public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request, CpsVendorConfig config) {
            return Collections.emptyList();
        }

        @Override
        public boolean testConnection(CpsVendorConfig config) {
            return true;
        }
    }
}
