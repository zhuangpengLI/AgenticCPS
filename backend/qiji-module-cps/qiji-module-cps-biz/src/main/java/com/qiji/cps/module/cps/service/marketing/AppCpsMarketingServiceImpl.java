package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeItemRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeRespVO;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeItemMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeMapper;
import com.qiji.cps.module.cps.service.selection.CpsSelectionConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

@Service
@Validated
public class AppCpsMarketingServiceImpl implements AppCpsMarketingService {

    @Resource
    private CpsRebateActivityMapper activityMapper;
    @Resource
    private CpsSelectionThemeMapper themeMapper;
    @Resource
    private CpsSelectionThemeItemMapper themeItemMapper;

    private Supplier<LocalDateTime> nowSupplier = LocalDateTime::now;

    @Override
    public List<AppCpsMarketingActivityRespVO> getActivityCenter(Long trustedLoginId,
                                                                 AppCpsMarketingActivityReqVO reqVO) {
        LocalDateTime now = nowSupplier.get();
        return activityMapper.selectEnabledList(now).stream()
                .filter(activity -> matchesActivityFilter(activity, reqVO))
                .map(activity -> BeanUtils.toBean(activity, AppCpsMarketingActivityRespVO.class))
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
}
