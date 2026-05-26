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

import java.time.LocalDateTime;

@TableName("cpx_article")
@KeySequence("cpx_article_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpxArticleDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String title;
    private String category;
    private String summary;
    private String coverUrl;
    private String content;
    private String platformCode;
    private String promotionMethod;
    private Long relatedTaskId;
    private String tags;
    private Integer status;
    private LocalDateTime publishTime;
}
