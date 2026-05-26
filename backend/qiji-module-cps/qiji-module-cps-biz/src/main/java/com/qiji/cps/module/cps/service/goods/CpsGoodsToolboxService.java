package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsBatchTransferReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsBatchTransferRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsCashGiftPlanReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsCashGiftPlanRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsCouponQueryReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsCouponQueryRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsOwnershipCheckReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsOwnershipCheckRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseRespVO;
import jakarta.validation.Valid;

public interface CpsGoodsToolboxService {

    CpsGoodsParseRespVO parseContent(@Valid CpsGoodsParseReqVO reqVO);

    CpsGoodsBatchTransferRespVO batchTransfer(@Valid CpsGoodsBatchTransferReqVO reqVO);

    CpsGoodsOwnershipCheckRespVO checkOwnership(@Valid CpsGoodsOwnershipCheckReqVO reqVO);

    CpsGoodsCouponQueryRespVO queryCoupons(@Valid CpsGoodsCouponQueryReqVO reqVO);

    CpsGoodsCashGiftPlanRespVO planCashGift(@Valid CpsGoodsCashGiftPlanReqVO reqVO);

}
