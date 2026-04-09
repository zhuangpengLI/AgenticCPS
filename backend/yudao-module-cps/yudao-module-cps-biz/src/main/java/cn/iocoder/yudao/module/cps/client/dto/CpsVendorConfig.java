package cn.iocoder.yudao.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * CPS API 供应商配置 DTO
 *
 * <p>从 {@link cn.iocoder.yudao.module.cps.dal.dataobject.vendor.CpsApiVendorDO} 转换而来，
 * 传入供应商客户端作为运行时配置。</p>
 *
 * @author CPS System
 */
@Data
@Builder
public class CpsVendorConfig {

    /**
     * 供应商编码（如 dataoke / haodanku / official）
     */
    private String vendorCode;

    /**
     * 供应商类型：aggregator(聚合平台) / official(官方API)
     */
    private String vendorType;

    /**
     * 电商平台编码（如 taobao / jd / pdd）
     */
    private String platformCode;

    /**
     * API Key
     */
    private String appKey;

    /**
     * API Secret
     */
    private String appSecret;

    /**
     * API 基础URL（如 https://openapi.dataoke.com/api）
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
     * 扩展配置（供应商特有参数，JSON解析后的Map）
     */
    private Map<String, String> extraConfig;

}
