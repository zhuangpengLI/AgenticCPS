package com.qiji.cps.module.cps.controller.admin.goodsmaster.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CPS商品主档分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsGoodsMasterPageReqVO extends PageParam {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "品牌")
    private String brandName;

    @Schema(description = "类目")
    private String categoryName;

    @Schema(description = "状态")
    private Integer status;
}
