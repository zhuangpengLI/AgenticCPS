package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryRespVO;
import jakarta.validation.Valid;

/**
 * CPS 商品返利查询 Service.
 *
 * @author CPS System
 */
public interface CpsGoodsRebateQueryService {

    /**
     * 查询商品返利并生成推广内容.
     *
     * @param reqVO 查询请求
     * @return 查询结果
     */
    CpsGoodsRebateQueryRespVO queryRebate(@Valid CpsGoodsRebateQueryReqVO reqVO);

}
