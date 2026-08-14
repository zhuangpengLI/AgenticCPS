package com.qiji.cps.module.cps.controller.openapi.didi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.controller.openapi.didi.vo.DidiCallbackResponse;
import com.qiji.cps.module.cps.service.didi.DidiCallbackService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OpenApiDidiCallbackControllerTest {
    private final DidiCallbackService service = mock(DidiCallbackService.class);
    private final OpenApiDidiCallbackController controller =
            new OpenApiDidiCallbackController(service, new ObjectMapper());

    @Test
    void returnsDidiFixedSuccessResponse() {
        when(service.handleOrder(eq("app"), eq("1"), eq("sign"), any(), any())).thenReturn(true);
        DidiCallbackResponse response = controller.order("app", "1", "sign", "{\"order_id\":\"o-1\"}");
        assertEquals(0, response.code());
        assertEquals("ok", response.msg());
    }

    @Test
    void malformedPayloadReturnsDidiErrorResponse() {
        DidiCallbackResponse response = controller.reward("app", "1", "sign", "not-json");
        assertEquals(1, response.code());
        assertEquals("err", response.msg());
        verifyNoInteractions(service);
    }
}
