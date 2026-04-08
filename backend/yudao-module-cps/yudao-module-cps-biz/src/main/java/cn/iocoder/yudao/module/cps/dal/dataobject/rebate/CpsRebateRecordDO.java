package cn.iocoder.yudao.module.cps.dal.dataobject.rebate;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.cps.enums.CpsRebateStatusEnum;
import cn.iocoder.yudao.module.cps.enums.CpsRebateTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * CPS返利记录 DO
 *
 * @author CPS System
 */
@TableName("cps_rebate_record")
@KeySequence("cps_rebate_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRebateRecordDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 会员ID
     */
    private Long memberId;
    /**
     * 订单ID
     */
    private Long orderId;
    /**
     * 平台编码
     */
    private String platformCode;
    /**
     * 平台订单号
     */
    private String platformOrderId;
    /**
     * 商品ID
     */
    private String itemId;
    /**
     * 商品标题
     */
    private String itemTitle;
    /**
     * 订单金额
     */
    private BigDecimal orderAmount;
    /**
     * 可分配佣金
     */
    private BigDecimal commissionAmount;
    /**
     * 返利比例
     */
    private BigDecimal rebateRate;
    /**
     * 返利金额
     */
    private BigDecimal rebateAmount;
    /**
     * 返利类型
     *
     * 枚举 {@link CpsRebateTypeEnum}
     */
    private String rebateType;
    /**
     * 返利状态
     *
     * 枚举 {@link CpsRebateStatusEnum}
     */
    private String rebateStatus;
    /**
     * 前序返利ID（扣回时关联）
     */
    private Long precedingRebateId;
    /**
     * 关联的冻结记录ID
     */
    private Long freezeRecordId;
    /**
     * 备注
     */
    private String remark;

}
