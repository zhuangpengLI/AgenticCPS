package cn.didi.union.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;
public class OrderData {
    private int total;
    @SerializedName("order_list") private List<OrderDetail> orderList;
    public int getTotal() { return total; } public List<OrderDetail> getOrderList() { return orderList; }
}
