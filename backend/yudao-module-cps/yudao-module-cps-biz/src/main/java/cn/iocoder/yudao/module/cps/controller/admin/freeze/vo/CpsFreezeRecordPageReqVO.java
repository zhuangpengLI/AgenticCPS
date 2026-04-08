package cn.iocoder.yudao.module.cps.controller.admin.freeze.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 管理后台 - CPS冻结记录分页查询 Request VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - CPS冻结记录分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsFreezeRecordPageReqVO extends PageParam {

    @Schema(description = "会员ID", example = "1001")
    private Long memberId;

    @Schema(description = "状态（pending/frozen/unfreezing/unfreezed）", example = "frozen")
    private String status;

    @Schema(description = "创建时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
