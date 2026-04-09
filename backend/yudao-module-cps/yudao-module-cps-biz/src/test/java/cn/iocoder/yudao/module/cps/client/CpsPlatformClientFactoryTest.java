package cn.iocoder.yudao.module.cps.client;

import cn.iocoder.yudao.module.cps.client.dto.*;
import cn.iocoder.yudao.module.cps.dal.dataobject.platform.CpsPlatformDO;
import cn.iocoder.yudao.module.cps.service.platform.CpsPlatformService;
import cn.iocoder.yudao.module.cps.service.vendor.CpsApiVendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link CpsPlatformClientFactory} 路由逻辑单元测试
 *
 * @author CPS System
 */
@ExtendWith(MockitoExtension.class)
class CpsPlatformClientFactoryTest {

    @InjectMocks
    private CpsPlatformClientFactory factory;

    @Mock
    private CpsPlatformService platformService;

    @Mock
    private CpsApiVendorService vendorService;

    /**
     * 简单的平台客户端实现
     */
    private static class StubPlatformClient implements CpsPlatformClient {
        private final String code;
        StubPlatformClient(String code) { this.code = code; }
        @Override public String getPlatformCode() { return code; }
        @Override public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) { return null; }
        @Override public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request) { return null; }
        @Override public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request) { return null; }
        @Override public boolean testConnection() { return true; }
    }

    /**
     * 简单的供应商客户端实现
     */
    private static class StubVendorClient implements CpsApiVendorClient {
        private final String vendorCode;
        private final String platformCode;
        StubVendorClient(String vendorCode, String platformCode) {
            this.vendorCode = vendorCode;
            this.platformCode = platformCode;
        }
        @Override public String getVendorCode() { return vendorCode; }
        @Override public String getPlatformCode() { return platformCode; }
        @Override public String getVendorType() { return "aggregator"; }
        @Override public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request, CpsVendorConfig config) { return null; }
        @Override public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request, CpsVendorConfig config) { return null; }
        @Override public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request, CpsVendorConfig config) { return Collections.emptyList(); }
        @Override public boolean testConnection(CpsVendorConfig config) { return true; }
    }

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射注入平台客户端和供应商客户端列表
        var platformClientsField = CpsPlatformClientFactory.class.getDeclaredField("platformClients");
        platformClientsField.setAccessible(true);
        platformClientsField.set(factory, List.of(
                new StubPlatformClient("taobao"),
                new StubPlatformClient("jd")
        ));

        var vendorClientsField = CpsPlatformClientFactory.class.getDeclaredField("vendorClients");
        vendorClientsField.setAccessible(true);
        vendorClientsField.set(factory, List.of(
                new StubVendorClient("dataoke", "taobao"),
                new StubVendorClient("dataoke", "jd"),
                new StubVendorClient("haodanku", "taobao")
        ));

        // 执行初始化
        factory.init();
    }

    @Test
    @DisplayName("getClient 应返回正确的平台适配器")
    void testGetClient() {
        assertNotNull(factory.getClient("taobao"));
        assertNotNull(factory.getClient("jd"));
        assertNull(factory.getClient("pdd"));
    }

    @Test
    @DisplayName("getRequiredClient 不存在时应抛出异常")
    void testGetRequiredClient_throwsOnMissing() {
        assertThrows(IllegalArgumentException.class, () -> factory.getRequiredClient("nonexistent"));
    }

    @Test
    @DisplayName("getVendorClient 应根据 vendorCode:platformCode 返回供应商客户端")
    void testGetVendorClient() {
        CpsApiVendorClient client = factory.getVendorClient("dataoke", "taobao");
        assertNotNull(client);
        assertEquals("dataoke", client.getVendorCode());
        assertEquals("taobao", client.getPlatformCode());

        // 好单库-淘宝
        CpsApiVendorClient hdkClient = factory.getVendorClient("haodanku", "taobao");
        assertNotNull(hdkClient);
        assertEquals("haodanku", hdkClient.getVendorCode());

        // 不存在的组合
        assertNull(factory.getVendorClient("haodanku", "jd"));
    }

    @Test
    @DisplayName("getActiveVendorClient 应根据平台配置返回激活的供应商")
    void testGetActiveVendorClient() {
        CpsPlatformDO platform = new CpsPlatformDO();
        platform.setActiveVendorCode("dataoke");
        when(platformService.getPlatformByCode("taobao")).thenReturn(platform);

        CpsApiVendorClient client = factory.getActiveVendorClient("taobao");
        assertNotNull(client);
        assertEquals("dataoke", client.getVendorCode());
    }

    @Test
    @DisplayName("getActiveVendorClient 平台不存在时返回 null")
    void testGetActiveVendorClient_platformNotFound() {
        when(platformService.getPlatformByCode("vip")).thenReturn(null);
        assertNull(factory.getActiveVendorClient("vip"));
    }

    @Test
    @DisplayName("getActiveVendorClient 未设置 activeVendorCode 时默认使用 dataoke")
    void testGetActiveVendorClient_defaultToDataoke() {
        CpsPlatformDO platform = new CpsPlatformDO();
        platform.setActiveVendorCode(null);
        when(platformService.getPlatformByCode("taobao")).thenReturn(platform);

        CpsApiVendorClient client = factory.getActiveVendorClient("taobao");
        assertNotNull(client);
        assertEquals("dataoke", client.getVendorCode());
    }

    @Test
    @DisplayName("getActiveVendorConfig 应通过 vendorService 获取配置")
    void testGetActiveVendorConfig() {
        CpsPlatformDO platform = new CpsPlatformDO();
        platform.setActiveVendorCode("dataoke");
        when(platformService.getPlatformByCode("taobao")).thenReturn(platform);

        CpsVendorConfig expectedConfig = CpsVendorConfig.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .appKey("testKey")
                .appSecret("testSecret")
                .apiBaseUrl("https://openapi.dataoke.com/api")
                .build();
        when(vendorService.getVendorConfig("dataoke", "taobao")).thenReturn(expectedConfig);

        CpsVendorConfig config = factory.getActiveVendorConfig("taobao");
        assertNotNull(config);
        assertEquals("dataoke", config.getVendorCode());
        assertEquals("testKey", config.getAppKey());
    }

    @Test
    @DisplayName("getRegisteredPlatformCodes 应返回所有注册的平台编码")
    void testGetRegisteredPlatformCodes() {
        var codes = factory.getRegisteredPlatformCodes();
        assertTrue(codes.contains("taobao"));
        assertTrue(codes.contains("jd"));
        assertEquals(2, codes.size());
    }

}
