package com.qiji.cps.module.cps.service.membergoods;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsIdentityReqVO;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsRecordPageReqVO;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsRecordRespVO;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsRecordSaveReqVO;
import jakarta.validation.Valid;

public interface CpsMemberGoodsRecordService {

    void recordBrowse(Long memberId, @Valid AppCpsMemberGoodsRecordSaveReqVO reqVO);

    PageResult<AppCpsMemberGoodsRecordRespVO> getBrowsePage(Long memberId,
                                                            @Valid AppCpsMemberGoodsRecordPageReqVO reqVO);

    void cleanBrowseHistory(Long memberId);

    void createFavorite(Long memberId, @Valid AppCpsMemberGoodsRecordSaveReqVO reqVO);

    void deleteFavorite(Long memberId, @Valid AppCpsMemberGoodsIdentityReqVO reqVO);

    PageResult<AppCpsMemberGoodsRecordRespVO> getFavoritePage(Long memberId,
                                                              @Valid AppCpsMemberGoodsRecordPageReqVO reqVO);
}
