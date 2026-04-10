package com.qiji.cps.module.cps.client;

import com.qiji.cps.module.cps.client.dto.*;

import java.util.List;

/**
 * CPS API 供应商客户端接口
 *
 * <p>每个实现代表一个「API供应商 + 电商平台」的组合。
 * 例如：大淘客-淘宝、好单库-京东、淘宝联盟官方API。</p>
 *
 * <p>与 {@link CpsPlatformClient} 的区别：
 * <ul>
 *   <li>CpsPlatformClient：面向业务层，按电商平台维度路由（taobao/jd/pdd）</li>
 *   <li>CpsApiVendorClient：面向底层实现，按供应商×平台双维度路由</li>
 * </ul>
 * </p>
 *
 * <p>设计决策：{@link CpsVendorConfig} 作为参数传入而非在实现中自行获取，
 * 实现「配置与逻辑分离」，便于测试和多租户支持。</p>
 *
 * @author CPS System
 */
public interface CpsApiVendorClient {

    /**
     * 获取供应商编码
     *
     * @return 供应商编码，如 "dataoke" / "haodanku" / "official"
     */
    String getVendorCode();

    /**
     * 获取电商平台编码
     *
     * @return 平台编码，如 "taobao" / "jd" / "pdd" / "vip" / "meituan" / "douyin"
     */
    String getPlatformCode();

    /**
     * 获取供应商类型
     *
     * @return 供应商类型：aggregator(聚合平台) / official(官方API)
     */
    String getVendorType();

    /**
     * 搜索商品
     *
     * @param request 搜索请求
     * @param config  供应商配置
     * @return 搜索结果（分页）
     */
    CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request, CpsVendorConfig config);

    /**
     * 生成推广链接（转链）
     *
     * @param request 转链请求
     * @param config  供应商配置
     * @return 推广链接结果
     */
    CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request, CpsVendorConfig config);

    /**
     * 查询平台订单（用于定时同步）
     *
     * @param request 订单查询请求
     * @param config  供应商配置
     * @return 订单列表
     */
    List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request, CpsVendorConfig config);

    /**
     * 测试供应商连接是否正常
     *
     * @param config 供应商配置
     * @return true-连接正常，false-连接异常
     */
    boolean testConnection(CpsVendorConfig config);

}
