package cn.didi.union.models;
import com.google.gson.annotations.SerializedName;
public class EstimateSuccessData {
    @SerializedName("estimate_time") private String estimateTime;
    @SerializedName("estimate_channel") private String estimateChannel;
    @SerializedName("receive_status") private int receiveStatus;
    @SerializedName("receive_time") private String receiveTime;
    @SerializedName("scene_name") private String sceneName;
    public String getEstimateTime() { return estimateTime; } public String getEstimateChannel() { return estimateChannel; }
    public int getReceiveStatus() { return receiveStatus; } public String getReceiveTime() { return receiveTime; }
    public String getSceneName() { return sceneName; }
}
