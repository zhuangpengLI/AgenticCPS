package com.qiji.cps.module.cps.dal.dataobject.goods;

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

@TableName("cps_goods_master")
@KeySequence("cps_goods_master_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsGoodsMasterDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String masterCode;

    private String standardTitle;

    private String brandName;

    private String categoryName;

    private String mainPic;

    private Integer status;

    private String remark;
}
