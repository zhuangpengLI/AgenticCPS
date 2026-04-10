package com.qiji.cps.module.cps.controller.admin.transfer.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.qiji.cps.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 管理后台 - CPS转链记录分页 Request VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - CPS转链记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsTransferRecordPageReqVO extends PageParam {

    @Schema(description = "会员ID", example = "1")
    private Long memberId;

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "商品标题（模糊匹配）", example = "手机")
    private String itemTitle;

    @Schema(description = "状态（0无效 1有效）", example = "1")
    private Integer status;

    @Schema(description = "创建时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
