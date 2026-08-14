package com.qiji.cps.module.cps.controller.openapi.didi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.framework.tenant.core.aop.TenantIgnore;
import com.qiji.cps.module.cps.controller.openapi.didi.vo.DidiCallbackResponse;
import com.qiji.cps.module.cps.controller.openapi.didi.vo.DidiOrderCallbackReqVO;
import com.qiji.cps.module.cps.controller.openapi.didi.vo.DidiRewardCallbackReqVO;
import com.qiji.cps.module.cps.service.didi.DidiCallbackService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/openapi/cps/didi/callback")
@PermitAll
@TenantIgnore
@RequiredArgsConstructor
public class OpenApiDidiCallbackController {
    private final DidiCallbackService callbackService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/order", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DidiCallbackResponse order(@RequestHeader("App-Key") String appKey,
                                      @RequestHeader("Timestamp") String timestamp,
                                      @RequestHeader("Sign") String sign,
                                      @RequestBody String rawBody) {
        try {
            DidiOrderCallbackReqVO request = objectMapper.readValue(rawBody, DidiOrderCallbackReqVO.class);
            return callbackService.handleOrder(appKey, timestamp, sign, rawBody, request)
                    ? DidiCallbackResponse.ok() : DidiCallbackResponse.error();
        } catch (Exception ex) { return DidiCallbackResponse.error(); }
    }

    @PostMapping(value = "/reward", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DidiCallbackResponse reward(@RequestHeader("App-Key") String appKey,
                                       @RequestHeader("Timestamp") String timestamp,
                                       @RequestHeader("Sign") String sign,
                                       @RequestBody String rawBody) {
        try {
            DidiRewardCallbackReqVO request = objectMapper.readValue(rawBody, DidiRewardCallbackReqVO.class);
            return callbackService.handleReward(appKey, timestamp, sign, rawBody, request)
                    ? DidiCallbackResponse.ok() : DidiCallbackResponse.error();
        } catch (Exception ex) { return DidiCallbackResponse.error(); }
    }
}
