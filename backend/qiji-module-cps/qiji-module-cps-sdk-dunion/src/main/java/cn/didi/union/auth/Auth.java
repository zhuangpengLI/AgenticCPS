package cn.didi.union.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public final class Auth {

    private Auth() {
    }

    public static String genSign(Map<String, Object> header, Map<String, Object> body, String accessKey) {
        try {
            TreeMap<String, Object> params = new TreeMap<>();
            params.putAll(header);
            params.putAll(body);
            String canonical = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((left, right) -> left + "&" + right)
                    .orElse("");
            String encoded = URLEncoder.encode(canonical, StandardCharsets.UTF_8) + accessKey;
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(encoded.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value & 0xff));
            }
            String base64 = Base64.getEncoder().encodeToString(hex.toString().getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(base64, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate DUnion signature", ex);
        }
    }
}
