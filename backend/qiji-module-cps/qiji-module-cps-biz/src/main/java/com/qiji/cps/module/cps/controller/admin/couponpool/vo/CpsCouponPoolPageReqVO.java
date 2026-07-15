package com.qiji.cps.module.cps.controller.admin.couponpool.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsCouponPoolPageReqVO extends PageParam {

    private String platformCode;
    private String vendorCode;
    private String externalGoodsId;
    private String status;
    private String sourceType;
    private Long activityId;
    private Long themeId;
}
