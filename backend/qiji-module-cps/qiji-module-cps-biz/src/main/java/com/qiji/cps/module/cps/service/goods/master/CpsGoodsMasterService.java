package com.qiji.cps.module.cps.service.goods.master;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsMasterPageReqVO;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsPriceSnapshotPageReqVO;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsSourceMappingPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsMasterDO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsPriceSnapshotDO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsSourceMappingDO;
import jakarta.validation.Valid;

public interface CpsGoodsMasterService {

    Long importSelectionItem(Long selectionItemId);

    CpsGoodsMasterDO getGoodsMaster(Long id);

    PageResult<CpsGoodsMasterDO> getGoodsMasterPage(@Valid CpsGoodsMasterPageReqVO reqVO);

    PageResult<CpsGoodsSourceMappingDO> getSourceMappingPage(@Valid CpsGoodsSourceMappingPageReqVO reqVO);

    PageResult<CpsGoodsPriceSnapshotDO> getPriceSnapshotPage(@Valid CpsGoodsPriceSnapshotPageReqVO reqVO);
}
