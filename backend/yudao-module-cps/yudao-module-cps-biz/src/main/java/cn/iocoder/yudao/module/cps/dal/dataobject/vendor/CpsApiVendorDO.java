package cn.iocoder.yudao.module.cps.dal.dataobject.vendor;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * CPS API 供应商配置 DO
 *
 * @author CPS System
 */
@TableName("cps_api_vendor")
@KeySequence("cps_api_vendor_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsApiVendorDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 供应商编码（如 dataoke / haodanku / miaoyouquan / shihuizhu / official）
     */
    private String vendorCode;
    /**
     * 供应商名称
     */
    private String vendorName;
    /**
     * 供应商类型：aggregator(聚合平台) / official(官方API)
     */
    private String vendorType;
    /**
     * 电商平台编码（如 taobao / jd / pdd / vip / meituan / douyin）
     */
    private String platformCode;
    /**
     * API Key
     */
    private String appKey;
    /**
     * API Secret（加密存储）
     */
    private String appSecret;
    /**
     * API 基础URL
     */
    private String apiBaseUrl;
    /**
     * 授权令牌（OAuth2 token / unionId 等）
     */
    private String authToken;
    /**
     * 默认推广位ID
     */
    private String defaultAdzoneId;
    /**
     * 扩展配置（JSON格式），存储供应商特有参数
     */
    private String extraConfig;
    /**
     * 优先级，数字越大优先级越高
     */
    private Integer priority;
    /**
     * 状态（0禁用 1启用）
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
