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

import java.time.LocalDateTime;

/**
 * CPS 选品主题 DO.
 */
@TableName("cps_selection_theme")
@KeySequence("cps_selection_theme_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsSelectionThemeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String themeCode;

    private String themeName;

    private String themeType;

    private String promotionEvent;

    private String platformCodes;

    private String vendorCode;

    private String coverPic;

    private String description;

    private String tags;

    private String ruleJson;

    private String aiPrompt;

    private String aiSummary;

    private String status;

    private Integer goodsSquareVisible;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String refreshStatus;

    private LocalDateTime lastRefreshTime;

    private Integer sort;

    private String remark;
}
