package com.qiji.cps.module.cps.enums.membergoods;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 会员商品展示记录类型。 */
@Getter
@AllArgsConstructor
public enum CpsMemberGoodsRecordTypeEnum {

    BROWSE("BROWSE"),
    FAVORITE("FAVORITE");

    private final String type;
}
