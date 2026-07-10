package com.qiji.cps.module.cps.client.didi;

import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DidiPlatformClientAdapterTest {
    @Mock private CpsPlatformClientFactory factory;
    @Mock private CpsApiVendorClient vendor;

    @Test
    void shouldDeclareNoGoodsSearchAndDelegateOrders() {
        CpsVendorConfig config = CpsVendorConfig.builder().build();
        when(factory.getActiveVendorClient("didi")).thenReturn(vendor);
        when(factory.getActiveVendorConfig("didi")).thenReturn(config);
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        when(vendor.queryOrders(request, config)).thenReturn(List.of());
        DidiPlatformClientAdapter adapter = new DidiPlatformClientAdapter(factory);

        assertFalse(adapter.supportsGoodsSearch());
        assertThrows(UnsupportedOperationException.class,
                () -> adapter.searchGoods(new CpsGoodsSearchRequest()));
        assertEquals(List.of(), adapter.queryOrders(request));
        verify(vendor).queryOrders(request, config);
    }
}
