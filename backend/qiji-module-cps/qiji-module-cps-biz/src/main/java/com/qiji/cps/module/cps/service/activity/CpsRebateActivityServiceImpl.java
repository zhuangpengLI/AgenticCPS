package com.qiji.cps.module.cps.service.activity;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPageReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivitySaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.REBATE_ACTIVITY_NOT_EXISTS;

@Service
@Validated
public class CpsRebateActivityServiceImpl implements CpsRebateActivityService {

    private static final String PLATFORM_HOT = "hot";
    private static final String SORT_MODE_LATEST = "latest";
    private static final String BILLING_TYPE_ALL = "all";
    private static final String BILLING_TYPE_CPS = "CPS";
    private static final String BILLING_TYPE_CPA = "CPA";
    private static final String BILLING_TYPE_CPS_CPA = "CPS+CPA";
    private static final String SOURCE_CONFIGURED = "configured";

    @Resource
    private CpsRebateActivityMapper activityMapper;

    @Resource
    private CpsPlatformService platformService;

    @Override
    public Long createActivity(CpsRebateActivitySaveReqVO createReqVO) {
        CpsRebateActivityDO activity = BeanUtils.toBean(createReqVO, CpsRebateActivityDO.class);
        activityMapper.insert(activity);
        return activity.getId();
    }

    @Override
    public void updateActivity(CpsRebateActivitySaveReqVO updateReqVO) {
        validateActivityExists(updateReqVO.getId());
        CpsRebateActivityDO updateObj = BeanUtils.toBean(updateReqVO, CpsRebateActivityDO.class);
        activityMapper.updateById(updateObj);
    }

    @Override
    public void deleteActivity(Long id) {
        validateActivityExists(id);
        activityMapper.deleteById(id);
    }

    @Override
    public CpsRebateActivityDO getActivity(Long id) {
        return activityMapper.selectById(id);
    }

    @Override
    public PageResult<CpsRebateActivityDO> getActivityPage(CpsRebateActivityPageReqVO pageReqVO) {
        return activityMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CpsRebateActivityDO> getEnabledActivityList() {
        return activityMapper.selectEnabledList(LocalDateTime.now());
    }

    @Override
    public CpsRebateActivityCenterRespVO getActivityCenter(CpsRebateActivityCenterReqVO reqVO) {
        List<CpsRebateActivityDO> enabledActivities = activityMapper.selectEnabledList(LocalDateTime.now());
        List<CpsPlatformDO> enabledPlatforms = loadEnabledPlatforms();
        Map<String, CpsPlatformDO> platformMap = enabledPlatforms.stream()
                .filter(platform -> StringUtils.hasText(platform.getPlatformCode()))
                .collect(Collectors.toMap(CpsPlatformDO::getPlatformCode, Function.identity(), (left, right) -> left));

        List<CpsRebateActivityDO> filtered = enabledActivities.stream()
                .filter(activity -> matchPlatform(activity, reqVO.getPlatformCode()))
                .filter(activity -> matchBillingType(activity, reqVO.getBillingType()))
                .filter(activity -> matchKeyword(activity, reqVO.getKeyword()))
                .sorted(buildActivityComparator(reqVO.getSortMode()))
                .toList();

        int total = filtered.size();
        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        int fromIndex = Math.min((pageNo - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);

        return CpsRebateActivityCenterRespVO.builder()
                .tabs(buildTabs(enabledActivities, enabledPlatforms))
                .billingTypeOptions(buildBillingTypeOptions(enabledActivities))
                .cards(filtered.subList(fromIndex, toIndex).stream()
                        .map(activity -> toCard(activity, platformMap))
                        .toList())
                .total((long) total)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .build();
    }

    private void validateActivityExists(Long id) {
        if (activityMapper.selectById(id) == null) {
            throw exception(REBATE_ACTIVITY_NOT_EXISTS);
        }
    }

    private List<CpsPlatformDO> loadEnabledPlatforms() {
        try {
            List<CpsPlatformDO> platforms = platformService.getEnabledPlatformList();
            return platforms == null ? List.of() : platforms;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private boolean matchPlatform(CpsRebateActivityDO activity, String platformCode) {
        return !StringUtils.hasText(platformCode)
                || PLATFORM_HOT.equals(platformCode)
                || platformCode.equals(activity.getPlatformCode());
    }

    private boolean matchBillingType(CpsRebateActivityDO activity, String billingType) {
        return !StringUtils.hasText(billingType)
                || BILLING_TYPE_ALL.equals(billingType)
                || billingType.equalsIgnoreCase(firstText(activity.getBillingType(), BILLING_TYPE_CPS));
    }

    private boolean matchKeyword(CpsRebateActivityDO activity, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(activity.getActivityName(), normalizedKeyword)
                || containsIgnoreCase(activity.getShortDesc(), normalizedKeyword)
                || containsIgnoreCase(activity.getRebateDesc(), normalizedKeyword)
                || containsIgnoreCase(activity.getSearchKeyword(), normalizedKeyword)
                || containsIgnoreCase(activity.getTagText(), normalizedKeyword);
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private Comparator<CpsRebateActivityDO> buildActivityComparator(String sortMode) {
        if (SORT_MODE_LATEST.equals(sortMode)) {
            return this::compareLatest;
        }
        return Comparator.comparing((CpsRebateActivityDO activity) ->
                        activity.getPromotionCount() == null ? 0 : activity.getPromotionCount())
                .reversed()
                .thenComparing(activity -> activity.getSort() == null ? Integer.MAX_VALUE : activity.getSort())
                .thenComparing((CpsRebateActivityDO activity) -> activity.getId() == null ? 0L : activity.getId(),
                        Comparator.reverseOrder());
    }

    private int compareLatest(CpsRebateActivityDO left, CpsRebateActivityDO right) {
        LocalDateTime leftTime = firstTime(left.getStartTime(), left.getCreateTime());
        LocalDateTime rightTime = firstTime(right.getStartTime(), right.getCreateTime());
        if (leftTime == null && rightTime == null) {
            return Long.compare(right.getId() == null ? 0L : right.getId(), left.getId() == null ? 0L : left.getId());
        }
        if (leftTime == null) {
            return 1;
        }
        if (rightTime == null) {
            return -1;
        }
        int timeCompare = rightTime.compareTo(leftTime);
        if (timeCompare != 0) {
            return timeCompare;
        }
        return Integer.compare(left.getSort() == null ? Integer.MAX_VALUE : left.getSort(),
                right.getSort() == null ? Integer.MAX_VALUE : right.getSort());
    }

    private LocalDateTime firstTime(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private List<CpsRebateActivityCenterRespVO.Tab> buildTabs(List<CpsRebateActivityDO> activities,
                                                             List<CpsPlatformDO> platforms) {
        Map<String, PlatformMeta> metas = new LinkedHashMap<>();
        putFallbackTabs(metas);
        platforms.stream()
                .filter(platform -> StringUtils.hasText(platform.getPlatformCode()))
                .sorted(Comparator.comparing(platform -> platform.getSort() == null ? Integer.MAX_VALUE : platform.getSort()))
                .forEach(platform -> metas.put(platform.getPlatformCode(), PlatformMeta.from(platform)));
        activities.stream()
                .map(CpsRebateActivityDO::getPlatformCode)
                .filter(StringUtils::hasText)
                .forEach(platformCode -> metas.putIfAbsent(platformCode, fallbackMeta(platformCode)));

        Map<String, Integer> countMap = activities.stream()
                .filter(activity -> StringUtils.hasText(activity.getPlatformCode()))
                .collect(Collectors.groupingBy(CpsRebateActivityDO::getPlatformCode, Collectors.collectingAndThen(
                        Collectors.counting(), Long::intValue)));
        countMap.put(PLATFORM_HOT, activities.size());

        return metas.values().stream()
                .map(meta -> CpsRebateActivityCenterRespVO.Tab.builder()
                        .platformCode(meta.platformCode())
                        .platformName(meta.platformName())
                        .platformLogo(meta.platformLogo())
                        .activityCount(countMap.getOrDefault(meta.platformCode(), 0))
                        .build())
                .toList();
    }

    private void putFallbackTabs(Map<String, PlatformMeta> metas) {
        List.of(
                new PlatformMeta(PLATFORM_HOT, "热门", null),
                new PlatformMeta("meituan", "美团", null),
                new PlatformMeta("eleme", "饿了么", null),
                new PlatformMeta("douyin", "抖音", null),
                new PlatformMeta("local_life", "本地生活", null),
                new PlatformMeta("fliggy", "飞猪旅行", null),
                new PlatformMeta("pdd", "拼多多", null),
                new PlatformMeta("taobao", "淘宝", null),
                new PlatformMeta("jd", "京东", null))
                .forEach(meta -> metas.put(meta.platformCode(), meta));
    }

    private List<CpsRebateActivityCenterRespVO.Option> buildBillingTypeOptions(List<CpsRebateActivityDO> activities) {
        Map<String, Integer> countMap = activities.stream()
                .map(activity -> firstText(activity.getBillingType(), BILLING_TYPE_CPS))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.collectingAndThen(
                        Collectors.counting(), Long::intValue)));
        List<CpsRebateActivityCenterRespVO.Option> options = new ArrayList<>();
        options.add(option(BILLING_TYPE_ALL, "全部", activities.size()));
        options.add(option(BILLING_TYPE_CPS, BILLING_TYPE_CPS, countMap.getOrDefault(BILLING_TYPE_CPS, 0)));
        options.add(option(BILLING_TYPE_CPA, BILLING_TYPE_CPA, countMap.getOrDefault(BILLING_TYPE_CPA, 0)));
        options.add(option(BILLING_TYPE_CPS_CPA, BILLING_TYPE_CPS_CPA, countMap.getOrDefault(BILLING_TYPE_CPS_CPA, 0)));
        return options;
    }

    private CpsRebateActivityCenterRespVO.Option option(String value, String label, Integer count) {
        return CpsRebateActivityCenterRespVO.Option.builder()
                .value(value)
                .label(label)
                .count(count)
                .build();
    }

    private CpsRebateActivityCenterRespVO.Card toCard(CpsRebateActivityDO activity,
                                                      Map<String, CpsPlatformDO> platformMap) {
        CpsPlatformDO platform = platformMap.get(activity.getPlatformCode());
        PlatformMeta fallback = fallbackMeta(activity.getPlatformCode());
        return CpsRebateActivityCenterRespVO.Card.builder()
                .id(activity.getId())
                .activityName(activity.getActivityName())
                .activityType(activity.getActivityType())
                .platformCode(activity.getPlatformCode())
                .platformName(platform != null ? firstText(platform.getPlatformName(), fallback.platformName()) : fallback.platformName())
                .platformLogo(platform != null ? platform.getPlatformLogo() : fallback.platformLogo())
                .mainPic(activity.getMainPic())
                .shortDesc(activity.getShortDesc())
                .rebateDesc(activity.getRebateDesc())
                .billingType(firstText(activity.getBillingType(), BILLING_TYPE_CPS))
                .promotionCount(activity.getPromotionCount() == null ? 0 : activity.getPromotionCount())
                .sourceType(firstText(activity.getSourceType(), SOURCE_CONFIGURED))
                .externalActivityId(activity.getExternalActivityId())
                .tagText(activity.getTagText())
                .jumpType(activity.getJumpType())
                .jumpUrl(activity.getJumpUrl())
                .searchKeyword(activity.getSearchKeyword())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .build();
    }

    private static PlatformMeta fallbackMeta(String platformCode) {
        return switch (platformCode == null ? "" : platformCode) {
            case PLATFORM_HOT -> new PlatformMeta(PLATFORM_HOT, "热门", null);
            case "meituan" -> new PlatformMeta("meituan", "美团", null);
            case "eleme" -> new PlatformMeta("eleme", "饿了么", null);
            case "douyin" -> new PlatformMeta("douyin", "抖音", null);
            case "local_life" -> new PlatformMeta("local_life", "本地生活", null);
            case "fliggy" -> new PlatformMeta("fliggy", "飞猪旅行", null);
            case "pdd" -> new PlatformMeta("pdd", "拼多多", null);
            case "taobao" -> new PlatformMeta("taobao", "淘宝", null);
            case "jd" -> new PlatformMeta("jd", "京东", null);
            case "vip" -> new PlatformMeta("vip", "唯品会", null);
            default -> new PlatformMeta(platformCode, StringUtils.hasText(platformCode) ? platformCode : "-", null);
        };
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private record PlatformMeta(String platformCode, String platformName, String platformLogo) {

        static PlatformMeta from(CpsPlatformDO platform) {
            PlatformMeta fallback = fallbackMeta(platform.getPlatformCode());
            return new PlatformMeta(platform.getPlatformCode(),
                    StringUtils.hasText(platform.getPlatformName()) ? platform.getPlatformName() : fallback.platformName(),
                    platform.getPlatformLogo());
        }

    }

}
