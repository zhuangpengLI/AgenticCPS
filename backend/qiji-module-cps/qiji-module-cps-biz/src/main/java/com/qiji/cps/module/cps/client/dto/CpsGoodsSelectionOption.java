package com.qiji.cps.module.cps.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsGoodsSelectionOption {

    private String value;

    private String label;

    private String tag;

    private String imageUrl;

    private String description;

    public static CpsGoodsSelectionOption of(String value, String label) {
        return CpsGoodsSelectionOption.builder()
                .value(value)
                .label(label)
                .build();
    }

}
