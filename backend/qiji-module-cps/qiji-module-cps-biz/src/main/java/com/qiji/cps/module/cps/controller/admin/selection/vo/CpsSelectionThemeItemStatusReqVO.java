package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - CPS选品主题商品状态 Request VO")
@Data
public class CpsSelectionThemeItemStatusReqVO {

    @NotEmpty(message = "商品快照ID不能为空")
    private List<Long> ids;

    @NotBlank(message = "商品状态不能为空")
    private String status;
}
