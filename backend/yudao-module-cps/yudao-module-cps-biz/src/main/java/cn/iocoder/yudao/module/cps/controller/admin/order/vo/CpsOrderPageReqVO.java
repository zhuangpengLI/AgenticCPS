package cn.iocoder.yudao.module.cps.controller.admin.order.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CPS订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsOrderPageReqVO extends PageParam {

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "会员ID", example = "1")
    private Long memberId;

    @Schema(description = "订单状态", example = "paid")
    private String orderStatus;

    @Schema(description = "商品标题", example = "手机")
    private String itemTitle;

    @Schema(description = "平台订单号")
    private String platformOrderId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
