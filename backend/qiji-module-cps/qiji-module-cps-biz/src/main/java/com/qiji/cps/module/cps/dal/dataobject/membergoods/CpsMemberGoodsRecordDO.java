package com.qiji.cps.module.cps.dal.dataobject.membergoods;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * CPS 会员商品浏览/收藏展示快照。
 *
 * <p>该快照仅用于会员再次浏览和展示，严禁作为订单归因、返利结算或资产变更依据。</p>
 */
@TableName("cps_member_goods_record")
@KeySequence("cps_member_goods_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsMemberGoodsRecordDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long memberId;
    private String recordType;
    private String platformCode;
    private String goodsId;
    private String goodsSign;
    /** 标准化商品身份的 SHA-256 摘要。 */
    private String identityKey;
    /** 数据库生成的未删除记录唯一键，不参与写入。 */
    @TableField(value = "active_unique_key", insertStrategy = FieldStrategy.NEVER,
            updateStrategy = FieldStrategy.NEVER)
    private String activeUniqueKey;
    private String title;
    private String mainPic;
    private Long originalPriceCent;
    private Long actualPriceCent;
    private Long couponPriceCent;
    private Long estimateRebateAmountCent;
    private Long monthSales;
    private String shopName;
}
