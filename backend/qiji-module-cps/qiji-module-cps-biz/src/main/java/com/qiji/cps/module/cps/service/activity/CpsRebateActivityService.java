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
import jakarta.validation.constraints.NotNull;

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

    // memberId 由 App 登录上下文或受信任的内部调用方单独提供，不能再校验管理端 VO 中的 memberId 字段。
    CpsRebateActivityPromotionRespVO generatePromotionContent(CpsRebateActivityPromotionReqVO reqVO,
                                                               @NotNull(message = "会员不能为空") Long memberId);

    void decorateActivityCapabilities(CpsRebateActivityDO activity, Object target);

}
