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
 * AI 选品结果人工复核记录 DO.
 */
@TableName("cps_selection_ai_review")
@KeySequence("cps_selection_ai_review_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsSelectionAiReviewDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String reviewContextId;

    /** 首次创建复核状态的管理员；用于同租户内的归属隔离。 */
    private Long ownerUserId;

    private String platformCode;

    private String vendorCode;

    private String goodsId;

    private String goodsSign;

    private String title;

    private String mainPic;

    private String reviewStatus;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    private String remark;
}
