package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeItemRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeRespVO;
import jakarta.validation.Valid;

import java.util.List;

public interface AppCpsMarketingService {

    List<AppCpsMarketingActivityRespVO> getActivityCenter(Long trustedLoginId,
                                                          @Valid AppCpsMarketingActivityReqVO reqVO);

    List<AppCpsMarketingSelectionThemeRespVO> getSelectionThemes(Long trustedLoginId,
                                                                 @Valid AppCpsMarketingSelectionThemeReqVO reqVO);

    List<AppCpsMarketingSelectionThemeItemRespVO> getSelectionThemeItems(Long trustedLoginId, Long themeId);
}
