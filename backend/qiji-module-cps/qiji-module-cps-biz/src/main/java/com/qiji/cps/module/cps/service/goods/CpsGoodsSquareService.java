package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareLinkReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareLinkRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareMetaRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareMetaItemRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchRespVO;
import jakarta.validation.Valid;

import java.util.List;

public interface CpsGoodsSquareService {

    CpsGoodsSquareMetaRespVO getMeta(String platformCode, String vendorCode);

    List<CpsGoodsSquareMetaItemRespVO> getHotKeywords(String platformCode, String vendorCode, Integer type);

    List<CpsGoodsSquareMetaItemRespVO> suggestKeywords(String platformCode, String vendorCode, String keyword, Integer type);

    CpsGoodsSquareSearchRespVO getVendorGoods(String sourceCode, String platformCode, String vendorCode, Integer pageSize);

    CpsGoodsSquareSearchRespVO searchGoods(@Valid CpsGoodsSquareSearchReqVO reqVO);

    CpsGoodsSquareSearchRespVO searchByImage(@Valid CpsGoodsSquareSearchReqVO reqVO);

    CpsGoodsSquareLinkRespVO generateLink(@Valid CpsGoodsSquareLinkReqVO reqVO);

}
