package com.qiji.cps.module.cps.controller.admin.cpx.vo;

import lombok.Data;

@Data
public class CpxTaskListReqVO {

    private String keyword;
    private String promotionMethod;
    private Integer limit;
}
