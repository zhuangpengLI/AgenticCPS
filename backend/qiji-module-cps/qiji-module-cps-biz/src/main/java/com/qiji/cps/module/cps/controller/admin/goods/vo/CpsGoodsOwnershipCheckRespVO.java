package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - CPS工具箱归属检测 Response VO")
@Data
@Builder
public class CpsGoodsOwnershipCheckRespVO {

    @Schema(description = "检测状态：MATCH/MISMATCH/NOT_FOUND")
    private String checkStatus;

    @Schema(description = "检测结论")
    private String message;

    @Schema(description = "归属结论文案")
    private String ownershipResult;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "商品ID")
    private String itemId;

    @Schema(description = "商品标题")
    private String itemTitle;

    @Schema(description = "转链记录ID")
    private Long transferRecordId;

    @Schema(description = "记录会员ID")
    private Long recordMemberId;

    @Schema(description = "记录会员昵称")
    private String recordMemberNickname;

    @Schema(description = "记录会员手机号")
    private String recordMemberMobile;

    @Schema(description = "记录推广位ID")
    private String recordAdzoneId;

    @Schema(description = "推广位ID / PID")
    private String pid;

    @Schema(description = "推广链接")
    private String promotionUrl;

    @Schema(description = "淘口令")
    private String taoCommand;

    @Schema(description = "记录状态")
    private Integer recordStatus;

    @Schema(description = "记录创建时间")
    private LocalDateTime createTime;

    @Schema(description = "不匹配项")
    private List<String> mismatches;

}
