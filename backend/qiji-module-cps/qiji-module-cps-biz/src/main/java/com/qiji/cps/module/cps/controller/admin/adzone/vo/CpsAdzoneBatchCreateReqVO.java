package com.qiji.cps.module.cps.controller.admin.adzone.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - CPS 推广位批量创建 Request VO")
@Data
public class CpsAdzoneBatchCreateReqVO {

    @Schema(description = "推广位创建项，最多 50 条")
    @NotEmpty(message = "推广位创建项不能为空")
    @Size(max = 50, message = "单次最多创建 50 个推广位")
    @Valid
    private List<CpsAdzoneSaveReqVO> items;

}
