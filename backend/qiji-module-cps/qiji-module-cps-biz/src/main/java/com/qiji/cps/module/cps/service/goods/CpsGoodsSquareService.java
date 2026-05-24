package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareLinkReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareLinkRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareMetaRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchRespVO;
import jakarta.validation.Valid;

public interface CpsGoodsSquareService {

    CpsGoodsSquareMetaRespVO getMeta(String platformCode, String vendorCode);

    CpsGoodsSquareSearchRespVO searchGoods(@Valid CpsGoodsSquareSearchReqVO reqVO);

    CpsGoodsSquareLinkRespVO generateLink(@Valid CpsGoodsSquareLinkReqVO reqVO);

}
