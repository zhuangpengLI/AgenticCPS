package com.qiji.cps.module.cps.client.haodanku.activity;

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
public class HdkActivityCategory {

    private Integer catId;

    private String name;

    private String icon;

    @Builder.Default
    private List<HdkSecondaryCategory> secondaryCategories = new ArrayList<>();

}
