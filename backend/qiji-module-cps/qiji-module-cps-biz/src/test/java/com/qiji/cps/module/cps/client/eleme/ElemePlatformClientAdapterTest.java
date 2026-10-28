package com.qiji.cps.module.cps.client.eleme;

import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ElemePlatformClientAdapterTest {

    @Test
    @DisplayName("平台编码必须为 eleme")
    void getPlatformCode() {
        ElemePlatformClientAdapter adapter = new ElemePlatformClientAdapter();
        assertEquals(CpsPlatformCodeEnum.ELEME.getCode(), adapter.getPlatformCode());
    }

    @Test
    @DisplayName("supportsGoodsSearch 应根据当前激活供应商的能力集动态判定")
    void supportsGoodsSearch() throws Exception {
        ElemePlatformClientAdapter adapter = new ElemePlatformClientAdapter();
        CpsPlatformClientFactory factory = mock(CpsPlatformClientFactory.class);
        injectField(adapter, "factory", factory);

        // 场景1：无激活供应商
        when(factory.getActiveVendorClient("eleme")).thenReturn(null);
        assertFalse(adapter.supportsGoodsSearch());

        // 场景2：激活供应商不支持 GOODS_SEARCH
        CpsApiVendorClient v1 = mock(CpsApiVendorClient.class);
        when(v1.getCapabilities()).thenReturn(Set.of(CpsVendorCapability.PROMOTION_LINK));
        when(factory.getActiveVendorClient("eleme")).thenReturn(v1);
        assertFalse(adapter.supportsGoodsSearch());

        // 场景3：激活官方供应商，支持 GOODS_SEARCH
        CpsApiVendorClient v2 = mock(CpsApiVendorClient.class);
        when(v2.getCapabilities()).thenReturn(Set.of(CpsVendorCapability.GOODS_SEARCH, CpsVendorCapability.PROMOTION_LINK));
        when(factory.getActiveVendorClient("eleme")).thenReturn(v2);
        assertTrue(adapter.supportsGoodsSearch());
    }

    @Test
    @DisplayName("searchGoods 委托激活供应商执行商品/门店搜索")
    void searchGoods() throws Exception {
        ElemePlatformClientAdapter adapter = new ElemePlatformClientAdapter();
        CpsPlatformClientFactory factory = mock(CpsPlatformClientFactory.class);
        injectField(adapter, "factory", factory);

        CpsApiVendorClient vendor = mock(CpsApiVendorClient.class);
        when(vendor.getCapabilities()).thenReturn(Set.of(CpsVendorCapability.GOODS_SEARCH));
        CpsVendorConfig config = CpsVendorConfig.builder().build();

        when(factory.getActiveVendorClient("eleme")).thenReturn(vendor);
        when(factory.getActiveVendorConfig("eleme")).thenReturn(config);

        CpsGoodsSearchResult expected = CpsGoodsSearchResult.builder()
                .list(List.of(CpsGoodsItem.builder().goodsId("E123").title("测试店铺").build()))
                .total(1L)
                .build();
        when(vendor.searchGoods(any(), eq(config))).thenReturn(expected);

        CpsGoodsSearchResult result = adapter.searchGoods(new CpsGoodsSearchRequest());
        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("E123", result.getList().get(0).getGoodsId());
    }

    @Test
    @DisplayName("generatePromotionLink 主供应商成功时直接返回")
    void generatePromotionLink_primarySuccess() throws Exception {
        ElemePlatformClientAdapter adapter = new ElemePlatformClientAdapter();
        CpsPlatformClientFactory factory = mock(CpsPlatformClientFactory.class);
        injectField(adapter, "factory", factory);

        CpsApiVendorClient vendor = mock(CpsApiVendorClient.class);
        when(vendor.getVendorCode()).thenReturn("official");
        CpsVendorConfig config = CpsVendorConfig.builder().build();

        when(factory.getActiveVendorClient("eleme")).thenReturn(vendor);
        when(factory.getActiveVendorConfig("eleme")).thenReturn(config);

        CpsPromotionLinkResult success = CpsPromotionLinkResult.builder()
                .shortUrl("https://tb.cn/short_1")
                .longUrl("https://s.click.ele.me/long_1")
                .build();
        when(vendor.generatePromotionLink(any(), eq(config))).thenReturn(success);

        CpsPromotionLinkResult result = adapter.generatePromotionLink(new CpsPromotionLinkRequest());
        assertNotNull(result);
        assertEquals("https://tb.cn/short_1", result.getShortUrl());
        verify(factory, never()).getVendorClient(eq("haodanku"), eq("eleme"));
    }

    @Test
    @DisplayName("generatePromotionLink 主供应商失败时尝试备用供应商兜底")
    void generatePromotionLink_fallbackToBackupVendor() throws Exception {
        ElemePlatformClientAdapter adapter = new ElemePlatformClientAdapter();
        CpsPlatformClientFactory factory = mock(CpsPlatformClientFactory.class);
        injectField(adapter, "factory", factory);

        // 主供应商：official，返回 null
        CpsApiVendorClient primaryVendor = mock(CpsApiVendorClient.class);
        when(primaryVendor.getVendorCode()).thenReturn("official");
        CpsVendorConfig primaryConfig = CpsVendorConfig.builder().build();
        when(factory.getActiveVendorClient("eleme")).thenReturn(primaryVendor);
        when(factory.getActiveVendorConfig("eleme")).thenReturn(primaryConfig);
        when(primaryVendor.generatePromotionLink(any(), eq(primaryConfig))).thenReturn(null);

        // 备用供应商：haodanku
        CpsApiVendorClient backupVendor = mock(CpsApiVendorClient.class);
        when(backupVendor.getVendorCode()).thenReturn("haodanku");
        CpsVendorConfig backupConfig = CpsVendorConfig.builder().build();
        when(factory.getVendorClient("haodanku", "eleme")).thenReturn(backupVendor);
        when(factory.getVendorConfig("haodanku", "eleme")).thenReturn(backupConfig);

        CpsPromotionLinkResult fallbackResult = CpsPromotionLinkResult.builder()
                .shortUrl("https://tb.cn/fallback_short")
                .build();
        when(backupVendor.generatePromotionLink(any(), eq(backupConfig))).thenReturn(fallbackResult);

        CpsPromotionLinkResult result = adapter.generatePromotionLink(new CpsPromotionLinkRequest());
        assertNotNull(result);
        assertEquals("https://tb.cn/fallback_short", result.getShortUrl());
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
