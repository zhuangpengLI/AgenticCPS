package com.qiji.cps.module.cps.service.didi;

import cn.didi.union.auth.Auth;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;

@Component
public class DidiCallbackSignatureVerifier {
    static final long MAX_TIMESTAMP_SKEW_SECONDS = 300;
    private final Clock clock;

    public DidiCallbackSignatureVerifier() { this(Clock.systemUTC()); }
    DidiCallbackSignatureVerifier(Clock clock) { this.clock = clock; }

    public boolean verify(String appKey, String timestamp, String sign, String accessKey) {
        if (blank(appKey) || blank(timestamp) || blank(sign) || blank(accessKey)) return false;
        long seconds;
        try { seconds = Long.parseLong(timestamp); } catch (NumberFormatException ex) { return false; }
        if (Math.abs(clock.instant().getEpochSecond() - seconds) > MAX_TIMESTAMP_SKEW_SECONDS) return false;
        String expected = Auth.genSign(Map.of("App-Key", appKey, "Timestamp", seconds), Map.of(), accessKey);
        return normalize(expected).equals(normalize(sign));
    }

    private String normalize(String value) {
        try { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
        catch (IllegalArgumentException ex) { return value; }
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
