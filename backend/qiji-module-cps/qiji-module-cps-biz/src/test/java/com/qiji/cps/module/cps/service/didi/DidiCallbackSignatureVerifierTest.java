package com.qiji.cps.module.cps.service.didi;

import cn.didi.union.auth.Auth;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DidiCallbackSignatureVerifierTest {
    private static final long NOW = 1_700_000_000L;
    private final DidiCallbackSignatureVerifier verifier = new DidiCallbackSignatureVerifier(
            Clock.fixed(Instant.ofEpochSecond(NOW), ZoneOffset.UTC));

    @Test
    void acceptsOfficialHeaderOnlySignature() {
        String sign = Auth.genSign(Map.of("App-Key", "app", "Timestamp", NOW), Map.of(), "secret");
        assertTrue(verifier.verify("app", String.valueOf(NOW), sign, "secret"));
    }

    @Test
    void rejectsExpiredTimestampAndWrongSecret() {
        String sign = Auth.genSign(Map.of("App-Key", "app", "Timestamp", NOW), Map.of(), "secret");
        assertFalse(verifier.verify("app", String.valueOf(NOW - 301), sign, "secret"));
        assertFalse(verifier.verify("app", String.valueOf(NOW), sign, "other"));
    }
}
