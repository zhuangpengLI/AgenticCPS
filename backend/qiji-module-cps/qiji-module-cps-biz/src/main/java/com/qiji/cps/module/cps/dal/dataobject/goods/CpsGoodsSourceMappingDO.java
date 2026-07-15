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

import java.time.LocalDateTime;

@TableName("cps_goods_source_mapping")
@KeySequence("cps_goods_source_mapping_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsGoodsSourceMappingDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long masterId;

    private String platformCode;

    private String vendorCode;

    private String externalGoodsId;

    private String goodsSign;

    private String itemLink;

    private String sourceTitle;

    private String sourceMainPic;

    private Integer status;

    private LocalDateTime lastSnapshotTime;

    private String rawData;
}
