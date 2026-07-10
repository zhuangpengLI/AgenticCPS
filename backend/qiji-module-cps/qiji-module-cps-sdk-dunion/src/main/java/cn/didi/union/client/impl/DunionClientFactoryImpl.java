package cn.didi.union.client.impl;

import cn.didi.union.client.DunionClientFactory;
import cn.didi.union.client.UnionClient;
import cn.didi.union.models.DunionClientConfig;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class DunionClientFactoryImpl implements DunionClientFactory {
    private final DunionClientConfig config;
    private final AtomicReference<UnionClient> client = new AtomicReference<>();

    public DunionClientFactoryImpl(DunionClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public UnionClient getUnionClient() {
        UnionClient current = client.get();
        if (current != null) return current;
        UnionClient created = new UnionClientImpl(config);
        return client.compareAndSet(null, created) ? created : client.get();
    }
}
