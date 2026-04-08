package cn.iocoder.yudao.module.cps.dal.dataobject.platform;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * CPS平台配置 DO
 *
 * @author CPS System
 */
@TableName("cps_platform")
@KeySequence("cps_platform_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsPlatformDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 平台编码（唯一标识）
     */
    private String platformCode;
    /**
     * 平台名称
     */
    private String platformName;
    /**
     * 平台Logo图片URL
     */
    private String platformLogo;
    /**
     * AppKey
     */
    private String appKey;
    /**
     * AppSecret（加密存储）
     */
    private String appSecret;
    /**
     * API基础URL
     */
    private String apiBaseUrl;
    /**
     * 授权令牌
     */
    private String authToken;
    /**
     * 默认推广位ID
     */
    private String defaultAdzoneId;
    /**
     * 平台服务费率（百分比）
     */
    private BigDecimal platformServiceRate;
    /**
     * 排序权重
     */
    private Integer sort;
    /**
     * 状态（0禁用 1启用）
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 扩展配置（JSON格式）
     */
    private String extraConfig;
    /**
     * 备注
     */
    private String remark;

}
