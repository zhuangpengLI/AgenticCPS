package cn.didi.union.models;
import com.google.gson.annotations.SerializedName;
public class LinkData {
    @SerializedName("app_id") private String appId;
    @SerializedName("app_source") private String appSource;
    private String dsi; private String link;
    public String getAppId() { return appId; } public String getAppSource() { return appSource; }
    public String getDsi() { return dsi; } public String getLink() { return link; }
}
