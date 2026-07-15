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

@TableName("cps_marketing_short_link")
@KeySequence("cps_marketing_short_link_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsMarketingShortLinkDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String shortCode;

    private String targetUrl;

    private String platformCode;

    private String vendorCode;

    private Long transferRecordId;

    private String campaignId;

    private String creativeId;

    private String channelCode;

    private String memberAttributionHash;

    private String requestHash;

    private Integer status;

    private LocalDateTime expireTime;

    private Long accessCount;

    private LocalDateTime lastAccessTime;

}
