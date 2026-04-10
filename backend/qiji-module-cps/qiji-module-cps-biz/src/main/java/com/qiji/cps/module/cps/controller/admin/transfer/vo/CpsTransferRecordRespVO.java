package com.qiji.cps.module.cps.controller.admin.transfer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台 - CPS转链记录 Response VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - CPS转链记录 Response VO")
@Data
public class CpsTransferRecordRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "商品ID")
    private String itemId;

    @Schema(description = "商品标题")
    private String itemTitle;

    @Schema(description = "原始口令/链接")
    private String originalContent;

    @Schema(description = "推广链接")
    private String promotionUrl;

    @Schema(description = "生成的淘口令")
    private String taoCommand;

    @Schema(description = "关联的订单号")
    private String platformOrderId;

    @Schema(description = "推广位ID")
    private String adzoneId;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "状态（0无效 1有效）")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
