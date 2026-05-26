package com.qiji.cps.module.cps.dal.dataobject.cpx;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("cpx_material")
@KeySequence("cpx_material_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpxMaterialDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long taskId;
    private Long offerId;
    private String materialType;
    private String title;
    private String content;
    private String imageUrl;
    private String landingUrl;
    private Integer status;
    private String remark;
}
