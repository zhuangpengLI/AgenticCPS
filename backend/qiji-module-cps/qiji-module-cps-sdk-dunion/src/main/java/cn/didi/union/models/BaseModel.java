package cn.didi.union.models;
public class BaseModel {
    private String errmsg; private long errno; private String traceid;
    public String getErrmsg() { return errmsg; }
    public long getErrno() { return errno; }
    public String getTraceid() { return traceid; }
}
