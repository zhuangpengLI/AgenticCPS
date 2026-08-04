package com.qiji.cps.module.cps.controller.app.membergoods.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "用户 APP - CPS 商品记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppCpsMemberGoodsRecordPageReqVO extends PageParam {

    @Schema(description = "平台编码", example = "taobao")
    @Size(max = 32, message = "平台编码长度不能超过 32")
    private String platformCode;
}
