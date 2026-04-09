package cn.iocoder.yudao.module.cps.client.common;

import cn.iocoder.yudao.module.cps.enums.CpsVendorCodeEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 官方 API 供应商客户端抽象基类
 *
 * <p>封装各电商平台官方联盟 API 的通用模式：
 * <ul>
 *   <li>OAuth2 Token 管理（淘宝联盟、京东联盟需要）</li>
 *   <li>统一错误码处理</li>
 *   <li>频率限制管理</li>
 * </ul>
 * </p>
 *
 * <p>各官方 API 的认证方式差异较大：
 * <ul>
 *   <li>淘宝联盟：OAuth2 + TOP签名（md5/hmac）</li>
 *   <li>京东联盟：OAuth2 + 签名</li>
 *   <li>拼多多联盟：MD5签名</li>
 *   <li>唯品会联盟：签名</li>
 *   <li>美团联盟：签名</li>
 *   <li>抖音联盟：签名</li>
 * </ul>
 * 因此此基类主要提供类型标识和通用工具，具体签名和HTTP逻辑由各子类实现。</p>
 *
 * @author CPS System
 */
@Slf4j
public abstract class AbstractOfficialVendorClient extends AbstractApiVendorClient {

    @Override
    public String getVendorCode() {
        return CpsVendorCodeEnum.OFFICIAL.getCode();
    }

    @Override
    public String getVendorType() {
        return "official";
    }

}
