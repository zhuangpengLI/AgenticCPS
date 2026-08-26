package com.qiji.cps.module.cps.dal.dataobject.selection;

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

/**
 * CPS 选品主题商品快照 DO.
 */
@TableName("cps_selection_theme_item")
@KeySequence("cps_selection_theme_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsSelectionThemeItemDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long themeId;

    private String platformCode;

    private String vendorCode;

    private String goodsId;

    private String goodsSign;

    private String title;

    private String mainPic;

    private BigDecimal originalPrice;

    private BigDecimal actualPrice;

    private BigDecimal couponPrice;

    private BigDecimal commissionRate;

    private BigDecimal commissionAmount;

    private Long monthSales;

    private String shopName;

    private String brandName;

    private String categoryName;

    private String activityTag;

    private String rankTag;

    private String sellingPoint;

    private BigDecimal recommendScore;

    private String recommendReason;

    private Integer topFlag;

    /** 是否经过人工排序、置顶或状态调整：0 否，1 是。 */
    private Integer manualAdjusted;

    private String status;

    private String sourceType;

    private String itemLink;

    private String rawData;

    private LocalDateTime snapshotTime;

    private Integer sort;
}
