package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsBatchTransferReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsBatchTransferRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseRespVO;
import jakarta.validation.Valid;

public interface CpsGoodsToolboxService {

    CpsGoodsParseRespVO parseContent(@Valid CpsGoodsParseReqVO reqVO);

    CpsGoodsBatchTransferRespVO batchTransfer(@Valid CpsGoodsBatchTransferReqVO reqVO);

}
