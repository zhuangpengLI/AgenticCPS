package cn.iocoder.yudao.module.cps.controller.admin.rebate.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - 返利配置分页 Request VO
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - 返利配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsRebateConfigPageReqVO extends PageParam {

    @Schema(description = "会员等级ID")
    private Long memberLevelId;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "状态（0禁用 1启用）")
    private Integer status;

}
