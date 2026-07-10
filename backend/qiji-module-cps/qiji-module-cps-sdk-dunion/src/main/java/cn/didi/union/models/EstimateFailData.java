package cn.didi.union.models;
import com.google.gson.annotations.SerializedName;
public class EstimateFailData {
    @SerializedName("fail_reason") private String failReason;
    @SerializedName("scene_name") private String sceneName;
    public String getFailReason() { return failReason; } public String getSceneName() { return sceneName; }
}
