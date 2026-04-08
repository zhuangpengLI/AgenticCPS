package cn.iocoder.yudao.module.cps.dal.dataobject.adzone;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.cps.enums.CpsAdzoneTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * CPS推广位（PID）DO
 *
 * @author CPS System
 */
@TableName("cps_adzone")
@KeySequence("cps_adzone_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsAdzoneDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 所属平台编码
     */
    private String platformCode;
    /**
     * 推广位ID
     */
    private String adzoneId;
    /**
     * 推广位名称
     */
    private String adzoneName;
    /**
     * 推广位类型
     *
     * 枚举 {@link CpsAdzoneTypeEnum}
     */
    private String adzoneType;
    /**
     * 关联类型（channel/member）
     */
    private String relationType;
    /**
     * 关联渠道或用户ID
     */
    private Long relationId;
    /**
     * 是否默认（0否 1是）
     */
    private Integer isDefault;
    /**
     * 状态（0禁用 1启用）
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

}
