package com.qiji.cps.module.cps.controller.admin.didi;

import com.qiji.cps.module.cps.controller.admin.didi.vo.DidiUnionMaterialGenerateReqVO;
import com.qiji.cps.module.cps.service.didi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DidiUnionControllerTest {
    @InjectMocks private DidiUnionController controller;
    @Mock private DidiUnionMaterialService service;

    @Test
    void shouldDelegateAllProductionEndpoints() throws Exception {
        DidiUnionMaterialGenerateReqVO request = new DidiUnionMaterialGenerateReqVO();
        request.setMaterialType(DidiUnionMaterialType.H5_LINK);
        request.setActivityId(1001L);
        DidiUnionMaterialResult material = new DidiUnionMaterialResult(DidiUnionMaterialType.H5_LINK,
                1001, 2002, "ops-1", "https://link", "dsi", null, null, null, null, null, "trace");
        when(service.generate(DidiUnionMaterialType.H5_LINK, 1001, null)).thenReturn(material);
        when(service.testConnection()).thenReturn(true);
        when(service.queryOrderAttribution("o-1")).thenReturn(
                new DidiUnionOrderAttributionResult("o-1", "trace", List.of(), List.of()));

        assertSame(material, controller.generateMaterial(request).getData());
        assertTrue(controller.testConnection().getData());
        assertEquals("o-1", controller.queryOrderAttribution("o-1").getData().orderId());

        assertEquals("@ss.hasPermission('cps:toolbox:link')", permission("generateMaterial",
                DidiUnionMaterialGenerateReqVO.class));
        assertEquals("@ss.hasPermission('cps:api-vendor:query')", permission("testConnection"));
        assertEquals("@ss.hasPermission('cps:order:query')", permission("queryOrderAttribution", String.class));
    }

    @Test
    void shouldNotExposeMockCallbackEndpoint() {
        assertTrue(java.util.Arrays.stream(DidiUnionController.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().toLowerCase().contains("mock")
                        || (method.getAnnotation(GetMapping.class) != null
                        && java.util.Arrays.stream(method.getAnnotation(GetMapping.class).value())
                        .anyMatch(path -> path.toLowerCase().contains("mock")))));
    }

    private String permission(String method, Class<?>... parameterTypes) throws Exception {
        return DidiUnionController.class.getDeclaredMethod(method, parameterTypes)
                .getAnnotation(PreAuthorize.class).value();
    }
}
