package cn.didi.union.auth;

import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthTest {

    @Test
    void shouldKeepOfficialSignatureAlgorithm() {
        TreeMap<String, Object> headers = new TreeMap<>();
        headers.put("App-Key", "test-app");
        headers.put("Timestamp", 1700000000);
        TreeMap<String, Object> body = new TreeMap<>();
        body.put("activity_id", 123);
        body.put("promotion_id", 456);
        body.put("source_id", "member A/1");

        assertEquals("MTY1Y2UzODY4NTM1NmE0MzgxYWI2NTM5ZTdmODEwYWViZmY1MTZmNg%3D%3D",
                Auth.genSign(headers, body, "secret-key"));
    }
}
