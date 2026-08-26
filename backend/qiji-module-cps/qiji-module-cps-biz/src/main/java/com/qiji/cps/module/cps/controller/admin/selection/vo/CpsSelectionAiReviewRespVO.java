package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 选品结果人工复核 Response VO")
@Data
public class CpsSelectionAiReviewRespVO {

    private Long id;
    private String reviewContextId;
    private String platformCode;
    private String vendorCode;
    private String goodsId;
    private String goodsSign;
    private String title;
    private String mainPic;
    private String reviewStatus;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String remark;
}
