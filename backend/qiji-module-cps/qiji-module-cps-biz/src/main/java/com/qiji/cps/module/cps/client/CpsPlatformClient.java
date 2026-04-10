package com.qiji.cps.module.cps.client;

import com.qiji.cps.module.cps.client.dto.*;

import java.util.List;

/**
 * CPS 平台客户端策略接口
 *
 * <p>每个平台实现此接口，注册为 Spring Bean，无需修改核心代码即可接入新平台（开放-关闭原则）。</p>
 *
 * @author CPS System
 */
public interface CpsPlatformClient {

    /**
     * 获取平台编码（对应 {@link com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum}）
     *
     * @return 平台编码，如 "taobao" / "jd" / "pdd" / "douyin"
     */
    String getPlatformCode();

    /**
     * 搜索商品
     *
     * @param request 搜索请求
     * @return 搜索结果（分页）
     */
    CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request);

    /**
     * 生成推广链接（转链）
     *
     * @param request 转链请求
     * @return 推广链接结果
     */
    CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request);

    /**
     * 查询平台订单（用于定时同步）
     *
     * @param request 订单查询请求
     * @return 订单列表
     */
    List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request);

    /**
     * 测试平台连接是否正常（用于平台配置保存时校验）
     *
     * @return true-连接正常，false-连接异常
     */
    boolean testConnection();

}
