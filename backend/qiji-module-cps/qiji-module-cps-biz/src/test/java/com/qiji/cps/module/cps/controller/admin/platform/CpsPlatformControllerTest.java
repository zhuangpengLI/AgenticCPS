package com.qiji.cps.module.cps.controller.admin.platform;

import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}
