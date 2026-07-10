package cn.didi.union.client;
import cn.didi.union.models.DunionClientConfig;
import java.util.TreeMap;
public interface BasicClient {
    String doPost(DunionClientConfig config, String urlPath, int timeout, TreeMap<String, Object> params);
    String doGet(DunionClientConfig config, String urlPath, int timeout, TreeMap<String, Object> params);
}
