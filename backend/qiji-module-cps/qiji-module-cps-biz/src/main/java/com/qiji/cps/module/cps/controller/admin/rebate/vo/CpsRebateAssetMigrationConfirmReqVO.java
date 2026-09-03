package com.qiji.cps.module.cps.controller.admin.rebate.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CpsRebateAssetMigrationConfirmReqVO {

    @NotBlank
    @Size(max = 128)
    private String approvalRef;
}
