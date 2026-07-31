package com.qiji.cps.module.cps.service.activity;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPageReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivitySaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import jakarta.validation.Valid;

import java.util.List;

public interface CpsRebateActivityService {

    Long createActivity(@Valid CpsRebateActivitySaveReqVO createReqVO);

    void updateActivity(@Valid CpsRebateActivitySaveReqVO updateReqVO);

    void deleteActivity(Long id);

    CpsRebateActivityDO getActivity(Long id);

    PageResult<CpsRebateActivityDO> getActivityPage(CpsRebateActivityPageReqVO pageReqVO);

    List<CpsRebateActivityDO> getEnabledActivityList();

    CpsRebateActivityCenterRespVO getActivityCenter(CpsRebateActivityCenterReqVO reqVO);

    CpsRebateActivityPromotionRespVO generatePromotionContent(@Valid CpsRebateActivityPromotionReqVO reqVO);

    CpsRebateActivityPromotionRespVO generatePromotionContent(@Valid CpsRebateActivityPromotionReqVO reqVO,
                                                               Long memberId);

}
