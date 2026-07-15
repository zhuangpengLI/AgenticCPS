package com.qiji.cps.module.cps.controller.admin.goodsmaster.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS商品主档 Response VO")
@Data
public class CpsGoodsMasterRespVO {

    private Long id;
    private String masterCode;
    private String standardTitle;
    private String brandName;
    private String categoryName;
    private String mainPic;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
