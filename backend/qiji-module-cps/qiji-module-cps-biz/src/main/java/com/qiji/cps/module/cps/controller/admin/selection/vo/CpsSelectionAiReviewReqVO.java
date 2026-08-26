package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - AI 选品结果人工复核 Request VO")
@Data
public class CpsSelectionAiReviewReqVO {

    @NotBlank(message = "复核上下文不能为空")
    @Size(max = 128, message = "复核上下文长度不能超过 128 个字符")
    private String reviewContextId;

    @NotBlank(message = "平台编码不能为空")
    @Size(max = 32, message = "平台编码长度不能超过 32 个字符")
    private String platformCode;

    @Size(max = 32, message = "供应商编码长度不能超过 32 个字符")
    private String vendorCode;

    @NotBlank(message = "商品 ID 不能为空")
    @Size(max = 128, message = "商品 ID 长度不能超过 128 个字符")
    private String goodsId;

    @Size(max = 255, message = "商品签名长度不能超过 255 个字符")
    private String goodsSign;

    @Size(max = 255, message = "商品标题长度不能超过 255 个字符")
    private String title;

    @Size(max = 1024, message = "商品主图长度不能超过 1024 个字符")
    private String mainPic;

    @NotBlank(message = "复核状态不能为空")
    @Pattern(regexp = "CONFIRMED|WITHDRAWN", message = "复核状态仅支持 CONFIRMED 或 WITHDRAWN")
    private String reviewStatus;

    @Size(max = 500, message = "复核备注长度不能超过 500 个字符")
    private String remark;
}
