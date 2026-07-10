package cn.didi.union.models;

import java.util.Objects;

public final class DunionClientConfig {

    public static final String DEFAULT_BASE_URL = "https://union.didi.cn/openapi/v1.0";
    private final String appKey;
    private final String accessKey;
    private final String baseUrl;
    private final int connectTimeout;
    private final int readTimeout;

    private DunionClientConfig(Builder builder) {
        this.appKey = requireText(builder.appKey, "appKey");
        this.accessKey = requireText(builder.accessKey, "accessKey");
        this.baseUrl = trimTrailingSlash(builder.baseUrl == null ? DEFAULT_BASE_URL : builder.baseUrl);
        this.connectTimeout = positive(builder.connectTimeout, 5000);
        this.readTimeout = positive(builder.readTimeout, 5000);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAppKey() { return appKey; }
    public String getAccessKey() { return accessKey; }
    public String getBaseUrl() { return baseUrl; }
    public int getConnectTimeout() { return connectTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public int getTimeout() { return readTimeout; }

    @Override
    public String toString() {
        return "DunionClientConfig{appKey='" + mask(appKey) + "', accessKey=***, baseUrl='" + baseUrl
                + "', connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout + "}";
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private static int positive(int value, int fallback) { return value > 0 ? value : fallback; }
    private static String trimTrailingSlash(String value) {
        String url = requireText(value, "baseUrl");
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }
    private static String mask(String value) {
        Objects.requireNonNull(value);
        return value.length() <= 4 ? "***" : value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    public static final class Builder {
        private String appKey;
        private String accessKey;
        private String baseUrl;
        private int connectTimeout;
        private int readTimeout;
        public Builder appKey(String value) { this.appKey = value; return this; }
        public Builder accessKey(String value) { this.accessKey = value; return this; }
        public Builder baseUrl(String value) { this.baseUrl = value; return this; }
        public Builder timeout(int value) { this.connectTimeout = value; this.readTimeout = value; return this; }
        public Builder connectTimeout(int value) { this.connectTimeout = value; return this; }
        public Builder readTimeout(int value) { this.readTimeout = value; return this; }
        public DunionClientConfig build() { return new DunionClientConfig(this); }
    }
}
