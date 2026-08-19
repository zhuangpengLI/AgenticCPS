package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityCardRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeItemRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionRespVO;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeItemMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeMapper;
import com.qiji.cps.module.cps.service.activity.CpsRebateActivityService;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import com.qiji.cps.module.cps.service.selection.CpsSelectionConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Validated
public class AppCpsMarketingServiceImpl implements AppCpsMarketingService {

    @Resource
    private CpsRebateActivityMapper activityMapper;
    @Resource
    private CpsSelectionThemeMapper themeMapper;
    @Resource
    private CpsSelectionThemeItemMapper themeItemMapper;
    @Resource
    private CpsRebateActivityService activityService;
    @Resource
    private CpsPlatformService platformService;

    private Supplier<LocalDateTime> nowSupplier = LocalDateTime::now;

    @Override
    public List<AppCpsMarketingActivityCardRespVO> getActivitiesByIds(List<Long> ids) {
        List<Long> orderedIds = new LinkedHashSet<>(ids).stream().toList();
        LocalDateTime now = nowSupplier.get();
        Map<Long, CpsRebateActivityDO> activitiesById = activityMapper.selectByIds(orderedIds).stream()
                .filter(activity -> CpsRebateActivityMapper.CPS_ENABLE_STATUS.equals(activity.getStatus()))
                .filter(activity -> isEffective(activity.getStartTime(), activity.getEndTime(), now))
                .collect(Collectors.toMap(CpsRebateActivityDO::getId, Function.identity()));
        Map<String, String> platformNames = new HashMap<>();
        return orderedIds.stream()
                .map(activitiesById::get)
                .filter(Objects::nonNull)
                .map(activity -> toActivityCardResp(activity, platformNames))
                .toList();
    }

    @Override
    public List<AppCpsMarketingActivityRespVO> getActivityCenter(Long trustedLoginId,
                                                                 AppCpsMarketingActivityReqVO reqVO) {
        LocalDateTime now = nowSupplier.get();
        return activityMapper.selectEnabledList(now).stream()
                .filter(activity -> matchesActivityFilter(activity, reqVO))
                .map(activity -> toActivityResp(activity, trustedLoginId))
                .toList();
    }

    @Override
    public List<AppCpsMarketingSelectionThemeRespVO> getSelectionThemes(Long trustedLoginId,
                                                                        AppCpsMarketingSelectionThemeReqVO reqVO) {
        LocalDateTime now = nowSupplier.get();
        return themeMapper.selectPublishedList(reqVO.getKeyword(), reqVO.getPromotionEvent()).stream()
                .filter(theme -> CpsSelectionConstants.ThemeStatus.PUBLISHED.equals(theme.getStatus()))
                .filter(theme -> isEffective(theme.getStartTime(), theme.getEndTime(), now))
                .map(theme -> BeanUtils.toBean(theme, AppCpsMarketingSelectionThemeRespVO.class))
                .toList();
    }

    @Override
    public List<AppCpsMarketingSelectionThemeItemRespVO> getSelectionThemeItems(Long trustedLoginId, Long themeId) {
        LocalDateTime now = nowSupplier.get();
        CpsSelectionThemeDO theme = themeMapper.selectById(themeId);
        if (theme == null || !CpsSelectionConstants.ThemeStatus.PUBLISHED.equals(theme.getStatus())
                || !isEffective(theme.getStartTime(), theme.getEndTime(), now)) {
            return List.of();
        }
        return themeItemMapper.selectEnabledListByThemeId(themeId).stream()
                .filter(item -> CpsSelectionConstants.ItemStatus.ENABLED.equals(item.getStatus()))
                .map(this::toThemeItemResp)
                .toList();
    }

    void setClockForTest(Supplier<LocalDateTime> nowSupplier) {
        this.nowSupplier = nowSupplier;
    }

    private boolean matchesActivityFilter(CpsRebateActivityDO activity, AppCpsMarketingActivityReqVO reqVO) {
        if (StringUtils.hasText(reqVO.getPlatformCode()) && !reqVO.getPlatformCode().equals(activity.getPlatformCode())) {
            return false;
        }
        if (StringUtils.hasText(reqVO.getBillingType()) && !reqVO.getBillingType().equals(activity.getBillingType())) {
            return false;
        }
        if (!StringUtils.hasText(reqVO.getKeyword())) {
            return true;
        }
        String keyword = reqVO.getKeyword();
        return contains(activity.getActivityName(), keyword)
                || contains(activity.getShortDesc(), keyword)
                || contains(activity.getTagText(), keyword)
                || contains(activity.getSearchKeyword(), keyword);
    }

    private boolean isEffective(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime now) {
        return (startTime == null || !startTime.isAfter(now))
                && (endTime == null || !endTime.isBefore(now));
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && value.contains(keyword);
    }

    private AppCpsMarketingSelectionThemeItemRespVO toThemeItemResp(CpsSelectionThemeItemDO item) {
        return BeanUtils.toBean(item, AppCpsMarketingSelectionThemeItemRespVO.class);
    }

    private AppCpsMarketingActivityRespVO toActivityResp(CpsRebateActivityDO activity, Long memberId) {
        AppCpsMarketingActivityRespVO respVO = BeanUtils.toBean(activity, AppCpsMarketingActivityRespVO.class);
        activityService.decorateActivityCapabilities(activity, respVO);
        if (memberId != null && Boolean.TRUE.equals(respVO.getSupportsPromotionLink())) {
            try {
                CpsRebateActivityPromotionReqVO request = new CpsRebateActivityPromotionReqVO();
                request.setActivityId(activity.getId());
                CpsRebateActivityPromotionRespVO promotion = activityService.generatePromotionContent(request, memberId);
                if (promotion != null) {
                    respVO.setLinkStatus(promotion.getLinkStatus());
                    respVO.setLinkType(promotion.getLinkType());
                    respVO.setLinkMessage(promotion.getLinkMessage());
                    respVO.setAttributionStatus(promotion.getAttributionStatus());
                    respVO.setAttributionMessage(promotion.getAttributionMessage());
                    respVO.setPromotionUrl(promotion.getPromotionUrl());
                    respVO.setTpwd(promotion.getTpwd());
                    respVO.setPromotionContent(promotion.getPromotionContent());
                }
            } catch (RuntimeException ignored) {
                respVO.setLinkStatus("FAILED");
                respVO.setLinkType("NONE");
                respVO.setLinkMessage("Activity entrance is temporarily unavailable");
            }
        }
        return respVO;
    }

    private AppCpsMarketingActivityCardRespVO toActivityCardResp(CpsRebateActivityDO activity,
                                                                  Map<String, String> platformNames) {
        AppCpsMarketingActivityCardRespVO respVO =
                BeanUtils.toBean(activity, AppCpsMarketingActivityCardRespVO.class);
        String platformCode = activity.getPlatformCode();
        if (StringUtils.hasText(platformCode)) {
            respVO.setPlatformName(platformNames.computeIfAbsent(platformCode, code -> {
                var platform = platformService.getPlatformByCode(code);
                return platform != null && StringUtils.hasText(platform.getPlatformName())
                        ? platform.getPlatformName() : code;
            }));
        }
        return respVO;
    }
}
