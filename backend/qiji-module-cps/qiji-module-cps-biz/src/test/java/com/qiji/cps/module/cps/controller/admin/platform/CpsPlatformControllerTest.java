package com.qiji.cps.module.cps.controller.admin.platform;

import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsPlatformControllerTest {

    @InjectMocks
    private CpsPlatformController controller;

    @Mock
    private CpsPlatformService platformService;
    @Mock
    private CpsPlatformClientFactory platformClientFactory;
    @Mock
    private CpsPlatformClient client;
    @Mock
    private CpsApiVendorClient vendorClient;

    @Test
    void testConnection_returnsSuccessWhenRegisteredClientPasses() {
        when(platformClientFactory.getClient("taobao")).thenReturn(client);
        when(client.testConnection()).thenReturn(true);

        var response = controller.testConnection("taobao").getData();

        assertEquals("taobao", response.getPlatformCode());
        assertTrue(response.getSupported());
        assertTrue(response.getSuccess());
    }

    @Test
    void testConnection_returnsDiagnosticFailureWhenClientThrows() {
        when(platformClientFactory.getClient("taobao")).thenReturn(client);
        when(client.testConnection()).thenThrow(new IllegalStateException("missing appKey"));

        var response = controller.testConnection("taobao").getData();

        assertEquals("taobao", response.getPlatformCode());
        assertTrue(response.getSupported());
        assertFalse(response.getSuccess());
        assertEquals("missing appKey", response.getFailureReason());
    }

    @Test
    void testConnection_returnsUnsupportedWhenClientNotRegistered() {
        when(platformClientFactory.getClient("unknown")).thenReturn(null);

        var response = controller.testConnection("unknown").getData();

        assertEquals("unknown", response.getPlatformCode());
        assertFalse(response.getSupported());
        assertFalse(response.getSuccess());
    }

    @Test
    void testConnection_usesActiveVendorWhenPlatformClientIsNotRegistered() {
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode("jutuike")
                .platformCode("union")
                .build();
        when(platformClientFactory.getActiveVendorClient("union")).thenReturn(vendorClient);
        when(platformClientFactory.getActiveVendorConfig("union")).thenReturn(config);
        when(vendorClient.testConnection(config)).thenReturn(true);

        var response = controller.testConnection("union").getData();

        assertEquals("union", response.getPlatformCode());
        assertTrue(response.getSupported());
        assertTrue(response.getSuccess());
        verify(platformClientFactory, never()).getClient("union");
    }
}
