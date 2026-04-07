package cn.iocoder.yudao.module.cps.dal.dataobject.transfer;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * CPS转链记录 DO
 *
 * @author CPS System
 */
@TableName("yudao_cps_transfer_record")
@KeySequence("yudao_cps_transfer_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsTransferRecordDO extends TenantBaseDO {

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
     * 平台编码
     */
    private String platformCode;
    /**
     * 原始口令/链接
     */
    private String originalContent;
    /**
     * 商品ID
     */
    private String itemId;
    /**
     * 商品标题
     */
    private String itemTitle;
    /**
     * 推广链接
     */
    private String promotionUrl;
    /**
     * 生成的淘口令
     */
    private String taoCommand;
    /**
     * 关联的订单号
     */
    private String platformOrderId;
    /**
     * 推广位ID
     */
    private String adzoneId;
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    /**
     * 状态（0无效 1有效）
     */
    private Integer status;

}
