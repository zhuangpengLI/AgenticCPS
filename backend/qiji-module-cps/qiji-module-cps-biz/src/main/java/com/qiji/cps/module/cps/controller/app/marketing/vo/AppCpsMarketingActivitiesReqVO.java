package com.qiji.cps.module.cps.controller.app.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Schema(description = "用户 APP - 按 ID 查询 CPS 营销活动 Request VO")
public class AppCpsMarketingActivitiesReqVO {

    @Schema(description = "活动 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "1,2")
    @NotEmpty(message = "活动 ID 不能为空")
    @Size(max = 10, message = "活动 ID 最多 10 个")
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids == null ? null : new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
