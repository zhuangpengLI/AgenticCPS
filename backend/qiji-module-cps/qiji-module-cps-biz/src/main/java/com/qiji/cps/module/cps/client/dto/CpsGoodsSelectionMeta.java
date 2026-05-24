package com.qiji.cps.module.cps.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsGoodsSelectionMeta {

    @Builder.Default
    private List<CpsGoodsSelectionOption> activities = new ArrayList<>();

    @Builder.Default
    private List<CpsGoodsSelectionOption> hotKeywords = new ArrayList<>();

    @Builder.Default
    private List<CpsGoodsSelectionOption> categories = new ArrayList<>();

    @Builder.Default
    private List<CpsGoodsSelectionOption> sortOptions = new ArrayList<>();

    @Builder.Default
    private List<CpsGoodsSelectionOption> filterOptions = new ArrayList<>();

    private String metaSource;

}
