package cn.didi.union.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;
public class EstimateQueryData {
    @SerializedName("estimate_success_list") private List<EstimateSuccessData> estimateSuccessList;
    @SerializedName("estimate_fail_list") private List<EstimateFailData> estimateFailList;
    public List<EstimateSuccessData> getEstimateSuccessList() { return estimateSuccessList; }
    public List<EstimateFailData> getEstimateFailList() { return estimateFailList; }
}
