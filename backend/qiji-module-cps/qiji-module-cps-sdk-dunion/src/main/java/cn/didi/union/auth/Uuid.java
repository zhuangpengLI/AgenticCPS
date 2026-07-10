package cn.didi.union.auth;

import java.util.UUID;

public final class Uuid {
    private Uuid() {
    }

    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
