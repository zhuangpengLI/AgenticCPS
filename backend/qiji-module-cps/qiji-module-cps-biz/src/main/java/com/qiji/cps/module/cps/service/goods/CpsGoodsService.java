package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;

import java.util.List;

/**
 * CPS 商品搜索与转链 Service 接口
 *
 * @author CPS System
 */
public interface CpsGoodsService {

    /**
     * 在指定平台搜索商品
     *
     * @param platformCode 平台编码
     * @param request      搜索请求
     * @return 搜索结果
     */
    CpsGoodsSearchResult searchGoods(String platformCode, CpsGoodsSearchRequest request);

    /**
     * 聚合多平台搜索（比价）
     * <p>在所有已启用平台中搜索关键词，合并结果并按券后价排序</p>
     *
     * @param request 搜索请求（keyword 必填）
     * @return 聚合商品列表（跨平台），按价格升序
     */
    List<CpsGoodsItem> searchGoodsAllPlatforms(CpsGoodsSearchRequest request);

    /**
     * 生成推广链接（转链）
     *
     * @param platformCode 平台编码
     * @param goodsId      商品ID
     * @param goodsSign    商品goodsSign（拼多多必填）
     * @param memberId     会员ID（用于订单归因）
     * @param adzoneId     推广位ID（优先使用，不传则用平台默认）
     * @return 推广链接结果
     */
    CpsPromotionLinkResult generatePromotionLink(String platformCode, String goodsId,
                                                  String goodsSign, Long memberId, String adzoneId);

}
