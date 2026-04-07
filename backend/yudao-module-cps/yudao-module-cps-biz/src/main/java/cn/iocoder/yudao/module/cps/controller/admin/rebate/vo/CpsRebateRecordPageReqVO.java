package cn.iocoder.yudao.module.cps.controller.admin.rebate.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CPS返利记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsRebateRecordPageReqVO extends PageParam {

    @Schema(description = "会员ID", example = "1")
    private Long memberId;

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "返利类型", example = "rebate")
    private String rebateType;

    @Schema(description = "返利状态", example = "pending")
    private String rebateStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
