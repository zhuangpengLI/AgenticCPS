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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("cps_goods_price_snapshot")
@KeySequence("cps_goods_price_snapshot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsGoodsPriceSnapshotDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long masterId;

    private Long sourceMappingId;

    private String platformCode;

    private String vendorCode;

    private String externalGoodsId;

    private String goodsSign;

    private Integer originalPrice;

    private Integer actualPrice;

    private Integer couponPrice;

    private LocalDateTime couponStartTime;

    private LocalDateTime couponEndTime;

    private BigDecimal commissionRate;

    private Integer commissionAmount;

    private Long monthSales;

    private String shopName;

    private String activityTag;

    private LocalDateTime snapshotTime;

    private String rawData;
}
