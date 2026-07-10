package com.qiji.cps.module.cps.controller.admin.didi.vo;
import com.qiji.cps.module.cps.service.didi.DidiUnionMaterialType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
@Data
public class DidiUnionMaterialGenerateReqVO {
    @NotNull private DidiUnionMaterialType materialType;
    @NotNull @Positive private Long activityId;
    @Positive private Long promotionId;
}
