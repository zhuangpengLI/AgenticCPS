package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - CPS工具箱归属检测 Request VO")
@Data
public class CpsGoodsOwnershipCheckReqVO {

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    @Schema(description = "待检测内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://item.taobao.com/item.htm?id=123")
    @NotBlank(message = "待检测内容不能为空")
    private String originalContent;

    @Schema(description = "期望会员ID", example = "100")
    private Long memberId;

    @Schema(description = "期望推广位ID", example = "mm_1_2_3")
    private String adzoneId;

    @Schema(description = "转链记录ID", example = "1000")
    private Long transferRecordId;

}
