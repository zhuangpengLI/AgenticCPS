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
public class HdkActivityPage {

    @Builder.Default
    private List<HdkActivityItem> items = new ArrayList<>();

    private Integer countPage;

    private Integer itemCount;

}
