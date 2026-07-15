package com.qiji.cps.module.cps.controller.admin.goodsmaster.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CpsGoodsSourceMappingRespVO {

    private Long id;
    private Long masterId;
    private String platformCode;
    private String vendorCode;
    private String externalGoodsId;
    private String goodsSign;
    private String itemLink;
    private String sourceTitle;
    private String sourceMainPic;
    private Integer status;
    private LocalDateTime lastSnapshotTime;
    private LocalDateTime createTime;
}
