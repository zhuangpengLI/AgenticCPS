package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - CPS选品主题商品排序 Request VO")
@Data
public class CpsSelectionThemeItemSortReqVO {

    @NotNull(message = "主题ID不能为空")
    private Long themeId;

    @Valid
    @NotEmpty(message = "排序商品不能为空")
    private List<SortItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortItem {
        @NotNull(message = "商品快照ID不能为空")
        private Long id;
        private Integer sort;
        private Integer topFlag;
    }
}
