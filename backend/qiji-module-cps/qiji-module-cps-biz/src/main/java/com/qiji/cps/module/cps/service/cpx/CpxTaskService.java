package com.qiji.cps.module.cps.service.cpx;

import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxArticleSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxDashboardRespVO;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxPlatformProfileSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxTaskSaveReqVO;
import com.qiji.cps.module.cps.controller.app.cpx.vo.AppCpxTrackingLinkCreateReqVO;
import com.qiji.cps.module.cps.controller.openapi.cpx.vo.CpxEventCreateReqVO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxArticleDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxConversionDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxEventDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxPlatformProfileDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTaskDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTrackingLinkDO;

import java.util.List;

public interface CpxTaskService {

    Long createTask(CpxTaskSaveReqVO createReqVO);

    void updateTask(CpxTaskSaveReqVO updateReqVO);

    CpxTaskDO getTask(Long id);

    List<CpxTaskDO> listAdminTasks(String keyword, String promotionMethod, Integer limit);

    List<CpxTaskDO> listPublishedTasks(String keyword, String promotionMethod, Integer limit);

    CpxTrackingLinkDO generateTrackingLink(AppCpxTrackingLinkCreateReqVO createReqVO, Long trustedMemberId);

    CpxEventDO recordEvent(CpxEventCreateReqVO createReqVO);

    Long createArticle(CpxArticleSaveReqVO createReqVO);

    void updateArticle(CpxArticleSaveReqVO updateReqVO);

    CpxArticleDO getArticle(Long id);

    List<CpxArticleDO> listAdminArticles(String keyword, String category, String promotionMethod, Integer limit);

    List<CpxArticleDO> searchArticles(String keyword, String category, String promotionMethod, Integer limit);

    Long createPlatformProfile(CpxPlatformProfileSaveReqVO createReqVO);

    void updatePlatformProfile(CpxPlatformProfileSaveReqVO updateReqVO);

    CpxPlatformProfileDO getPlatformProfile(Long id);

    List<CpxPlatformProfileDO> listPlatformProfiles();

    List<CpxPlatformProfileDO> listEnabledPlatformProfiles();

    List<CpxConversionDO> listMemberConversions(Long memberId, Integer limit);

    CpxDashboardRespVO getDashboardSummary();
}
