package com.qiji.cps.module.cps.controller.admin.goodsmaster.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CPS商品来源映射分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsGoodsSourceMappingPageReqVO extends PageParam {

    private Long masterId;
    private String platformCode;
    private String vendorCode;
    private String externalGoodsId;
    private Integer status;
}
