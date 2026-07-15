package com.qiji.cps.module.cps.client.taobao;

import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderPageResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaobaoPlatformClientAdapterOrderSyncTest {

    @Mock private CpsPlatformClientFactory factory;
    @Mock private CpsApiVendorClient vendor;
    private TaobaoPlatformClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TaobaoPlatformClientAdapter();
        ReflectionTestUtils.setField(adapter, "factory", factory);
    }

    @Test
    void missingVendorConfigurationFailsOrderSyncInsteadOfReturningEmptySuccess() {
        assertThrows(CpsVendorException.class,
                () -> adapter.queryOrderPage(new CpsOrderQueryRequest()));
    }

    @Test
    void delegatesExplicitPaginationContractFromVendor() {
        CpsVendorConfig config = CpsVendorConfig.builder().vendorCode("dataoke").platformCode("taobao").build();
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        CpsOrderPageResult expected = CpsOrderPageResult.cursor(
                List.of(CpsOrderDTO.builder().platformOrderId("1").build()), "next", true);
        when(factory.getActiveVendorClient("taobao")).thenReturn(vendor);
        when(factory.getActiveVendorConfig("taobao")).thenReturn(config);
        when(vendor.queryOrderPage(request, config)).thenReturn(expected);

        assertSame(expected, adapter.queryOrderPage(request));
        verify(vendor).queryOrderPage(request, config);
    }
}
