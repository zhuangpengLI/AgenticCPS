package com.qiji.cps.module.cps.service.exchange;

import com.qiji.cps.module.cps.config.CpsAitokenExchangeProperties;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeOrderRespDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangePreviewReqDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangePreviewRespDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeSubmitReqDTO;
import jakarta.annotation.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;

@Component
public class CpsAitokenExchangeClient {

    private static final String PREVIEW_PATH = "/api/v1/openapi/token/exchange/preview";
    private static final String SUBMIT_PATH = "/api/v1/openapi/token/exchange/submit";
    private static final String ORDER_PATH = "/api/v1/openapi/token/exchange/orders/";

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private CpsAitokenExchangeProperties properties;

    @Resource
    private CpsOpenApiSignatureService signatureService;

    public CpsAitokenExchangePreviewRespDTO preview(CpsAitokenExchangePreviewReqDTO request, Long tenantId) {
        HttpHeaders headers = buildHeaders("POST", PREVIEW_PATH, null, request, tenantId);
        return restTemplate.postForObject(properties.getBaseUrl() + PREVIEW_PATH,
                new HttpEntity<>(request, headers), CpsAitokenExchangePreviewRespDTO.class);
    }

    public CpsAitokenExchangeOrderRespDTO submit(CpsAitokenExchangeSubmitReqDTO request, Long tenantId) {
        HttpHeaders headers = buildHeaders("POST", SUBMIT_PATH, request.getIdempotencyKey(), request, tenantId);
        return restTemplate.postForObject(properties.getBaseUrl() + SUBMIT_PATH,
                new HttpEntity<>(request, headers), CpsAitokenExchangeOrderRespDTO.class);
    }

    public CpsAitokenExchangeOrderRespDTO getOrder(String exchangeOrderId, Long tenantId) {
        String path = ORDER_PATH + exchangeOrderId;
        HttpHeaders headers = buildHeaders("GET", path, null, null, tenantId);
        return restTemplate.exchange(properties.getBaseUrl() + path, HttpMethod.GET,
                new HttpEntity<>(headers), CpsAitokenExchangeOrderRespDTO.class).getBody();
    }

    private HttpHeaders buildHeaders(String method, String path, String idempotencyKey, Object body, Long tenantId) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-App-Id", properties.getAppId());
        headers.add("X-Tenant-Id", tenantId == null ? "0" : String.valueOf(tenantId));
        headers.add("X-Timestamp", timestamp);
        headers.add("X-Nonce", nonce);
        if (idempotencyKey != null) {
            headers.add("X-Idempotency-Key", idempotencyKey);
        }
        headers.add("X-Signature", signatureService.sign(properties.getAppSecret(), method, path,
                timestamp, nonce, idempotencyKey, body));
        return headers;
    }
}
