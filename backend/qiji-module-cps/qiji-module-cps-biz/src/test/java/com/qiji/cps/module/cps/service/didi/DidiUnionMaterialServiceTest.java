package com.qiji.cps.module.cps.service.didi;

import cn.didi.union.client.UnionClient;
import cn.didi.union.models.*;
import com.google.gson.Gson;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.didi.DidiOfficialVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DidiUnionMaterialServiceTest {

    @Mock private CpsPlatformClientFactory platformFactory;
    @Mock private DidiOfficialVendorClient vendorClient;
    @Mock private UnionClient unionClient;
    private DidiUnionMaterialServiceImpl service;
    private CpsVendorConfig config;

    @BeforeEach
    void setUp() {
        service = new DidiUnionMaterialServiceImpl(platformFactory, vendorClient);
        config = CpsVendorConfig.builder().appKey("app").appSecret("secret").defaultAdzoneId("2002").build();
        lenient().when(platformFactory.getVendorConfig("official", "didi")).thenReturn(config);
        lenient().when(vendorClient.getClient(config)).thenReturn(unionClient);
        lenient().when(vendorClient.getTimeout(config)).thenReturn(5000);
    }

    @Test
    void shouldGenerateH5QrCodeWithServerOwnedSourceIdAndDefaultPromotion() {
        LinkResponse link = new Gson().fromJson("{\"errno\":0,\"traceid\":\"t1\",\"data\":{\"dsi\":\"d1\",\"link\":\"https://link\"}}", LinkResponse.class);
        QrCodeResponse qr = new Gson().fromJson("{\"errno\":0,\"traceid\":\"t2\",\"data\":{\"code_link\":\"https://qr\"}}", QrCodeResponse.class);
        when(unionClient.generateH5Link(eq(1001L), eq(2002L), startsWith("ops-"), eq(5000)))
                .thenReturn(Result.Builder.<LinkResponse>builder().success(true).model(link).build());
        when(unionClient.generateH5Code(eq("d1"), startsWith("ops-"), eq(5000)))
                .thenReturn(Result.Builder.<QrCodeResponse>builder().success(true).model(qr).build());

        DidiUnionMaterialResult result = service.generate(DidiUnionMaterialType.H5_QR_CODE, 1001L, null);

        assertTrue(result.sourceId().startsWith("ops-"));
        assertEquals("https://link", result.link());
        assertEquals("https://qr", result.qrCodeUrl());
        verify(unionClient).generateH5Code("d1", result.sourceId(), 5000);
    }

    @Test
    void shouldReturnConnectionTestResult() {
        when(vendorClient.testConnection(config)).thenReturn(true);
        assertTrue(service.testConnection());
    }

    @Test
    void shouldMapOrderAttributionDiagnosis() {
        OrderSelfQueryResponse response = new Gson().fromJson("""
                {"errno":0,"traceid":"trace-a","data":{"estimate_success_list":[{"estimate_channel":"h5","receive_status":1,"scene_name":"快车"}],"estimate_fail_list":[{"fail_reason":"source missing","scene_name":"代驾"}]}}
                """, OrderSelfQueryResponse.class);
        when(unionClient.selfQueryOrder("o-1", 5000))
                .thenReturn(Result.Builder.<OrderSelfQueryResponse>builder().success(true).model(response).build());

        DidiUnionOrderAttributionResult result = service.queryOrderAttribution("o-1");

        assertEquals("trace-a", result.traceId());
        assertEquals("h5", result.successList().get(0).estimateChannel());
        assertEquals("source missing", result.failList().get(0).failReason());
    }
}
