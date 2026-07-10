package cn.didi.union.client;

import cn.didi.union.client.impl.DunionClientFactoryImpl;
import cn.didi.union.models.DunionClientConfig;

public interface DunionClientFactory {
    static DunionClientFactory build(DunionClientConfig config) {
        return new DunionClientFactoryImpl(config);
    }

    UnionClient getUnionClient();
}
