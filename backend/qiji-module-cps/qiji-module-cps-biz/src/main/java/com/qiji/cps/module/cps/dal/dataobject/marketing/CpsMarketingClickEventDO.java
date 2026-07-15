package com.qiji.cps.module.cps.dal.dataobject.marketing;

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

@TableName("cps_marketing_click_event")
@KeySequence("cps_marketing_click_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsMarketingClickEventDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String clickId;

    private String shortCode;

    private Long shortLinkId;

    private String campaignId;

    private String creativeId;

    private String channelCode;

    private String memberAttributionHash;

    private String ipHash;

    private String userAgentHash;

    private String deviceHash;

    private String dedupeKey;

    private String trustedSource;

    private Integer status;

    private LocalDateTime clickTime;
}
