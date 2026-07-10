package cn.didi.union.models;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HttpResponse {
    private Map<String, List<String>> header;
    private byte[] body;
    private int status;
    private BaseModel model;
    public Map<String, List<String>> getHeader() { return header; }
    public void setHeader(Map<String, List<String>> value) {
        header = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        value.forEach((key, item) -> { if (key != null) header.put(key, item); });
    }
    public byte[] getBody() { return body; }
    public void setBody(byte[] body) { this.body = body; }
    public String getBodyStr() { return body == null ? "" : new String(body, StandardCharsets.UTF_8); }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public BaseModel getModel() { return model; }
    public void setModel(BaseModel model) { this.model = model; }
}
