package com.qiji.cps.module.cps.controller.admin.activity.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CPS活动中心聚合查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsRebateActivityCenterReqVO extends PageParam {

    @Schema(description = "平台编码，hot 表示热门全平台", example = "meituan")
    private String platformCode;

    @Schema(description = "活动来源/API 供应商，all 表示全部", example = "dataoke")
    private String sourceType;

    @Schema(description = "计费类型：CPS/CPA/CPS+CPA", example = "CPS")
    private String billingType;

    @Schema(description = "搜索关键词", example = "外卖")
    private String keyword;

    @Schema(description = "排序模式：hot/latest", example = "hot")
    private String sortMode;

}
