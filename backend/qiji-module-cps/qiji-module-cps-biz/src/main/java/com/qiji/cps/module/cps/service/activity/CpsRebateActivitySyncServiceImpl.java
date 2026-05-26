package com.qiji.cps.module.cps.service.activity;

import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityCategory;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityClient;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityItem;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityListRequest;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityPage;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkSecondaryCategory;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CpsRebateActivitySyncServiceImpl {

    private static final String SOURCE_HAODANKU = "haodanku";
    private static final String HDK_EXTERNAL_PREFIX = "hdk:";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private HdkActivityClient hdkActivityClient;

    @Resource
    private CpsRebateActivityMapper activityMapper;

    public CpsRebateActivitySyncResult syncHaodankuActivities(CpsRebateActivitySyncRequest request) {
        CpsRebateActivitySyncResult result = CpsRebateActivitySyncResult.builder().build();
        List<HdkActivityCategory> categories = hdkActivityClient.fetchCategories();
        int maxPages = request.getMaxPages() == null || request.getMaxPages() <= 0 ? 1 : request.getMaxPages();
        for (HdkActivityCategory category : categories) {
            List<HdkSecondaryCategory> secondaryCategories = category.getSecondaryCategories();
            if (secondaryCategories == null || secondaryCategories.isEmpty()) {
                syncCategoryPage(request, result, category, null, maxPages);
                continue;
            }
            for (HdkSecondaryCategory secondaryCategory : secondaryCategories) {
                syncCategoryPage(request, result, category, secondaryCategory, maxPages);
            }
        }
        return result;
    }

    private void syncCategoryPage(CpsRebateActivitySyncRequest request, CpsRebateActivitySyncResult result,
                                  HdkActivityCategory category, HdkSecondaryCategory secondaryCategory, int maxPages) {
        for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
            HdkActivityPage page = hdkActivityClient.fetchActivities(HdkActivityListRequest.builder()
                    .pageNo(pageNo)
                    .keyword(request.getKeyword())
                    .catId(category.getCatId())
                    .secondaryCatId(secondaryCategory == null ? null : secondaryCategory.getSecondaryCatId())
                    .order(1)
                    .build());
            if (page == null || page.getItems() == null || page.getItems().isEmpty()) {
                continue;
            }
            for (HdkActivityItem item : page.getItems()) {
                upsertActivity(request, result, category, secondaryCategory, item);
            }
        }
    }

    private void upsertActivity(CpsRebateActivitySyncRequest request, CpsRebateActivitySyncResult result,
                                HdkActivityCategory category, HdkSecondaryCategory secondaryCategory,
                                HdkActivityItem item) {
        String externalActivityId = HDK_EXTERNAL_PREFIX + firstText(item.getActivityId(), item.getId());
        if (!StringUtils.hasText(externalActivityId)) {
            result.setSkippedCount(result.getSkippedCount() + 1);
            return;
        }
        CpsRebateActivityDO activity = toActivity(request, category, secondaryCategory, item, externalActivityId);
        CpsRebateActivityDO existing = activityMapper.selectBySourceTypeAndExternalActivityId(SOURCE_HAODANKU,
                externalActivityId);
        if (existing == null) {
            activityMapper.insert(activity);
            result.setInsertedCount(result.getInsertedCount() + 1);
            return;
        }
        activity.setId(existing.getId());
        activityMapper.updateById(activity);
        result.setUpdatedCount(result.getUpdatedCount() + 1);
    }

    private CpsRebateActivityDO toActivity(CpsRebateActivitySyncRequest request, HdkActivityCategory category,
                                           HdkSecondaryCategory secondaryCategory, HdkActivityItem item,
                                           String externalActivityId) {
        String activityType = secondaryCategory == null ? category.getName() : secondaryCategory.getName();
        String activityName = firstText(item.getActivityName(), item.getActivityLabel(), activityType);
        String shortDesc = sanitizeDescription(item.getDescribe());
        return CpsRebateActivityDO.builder()
                .activityName(activityName)
                .activityType(activityType)
                .platformCode(firstText(request.getPlatformCode(), item.getPlatform()))
                .mainPic(item.getActivityPic())
                .shortDesc(shortDesc)
                .rebateDesc(item.getCommissionRate())
                .billingType(resolveBillingType(item.getPromotionType()))
                .promotionCount(parseInt(item.getPromotionNum()))
                .sourceType(SOURCE_HAODANKU)
                .externalActivityId(externalActivityId)
                .tagText(firstText(item.getActivityLabel(), activityType))
                .jumpType(StringUtils.hasText(item.getActivityUrl()) ? "url" : "search")
                .jumpUrl(item.getActivityUrl())
                .searchKeyword(activityName)
                .sort(0)
                .status(1)
                .startTime(parseTime(item.getStartTime()))
                .endTime(parseTime(item.getEndTime()))
                .build();
    }

    private String resolveBillingType(String promotionType) {
        if ("2".equals(promotionType)) {
            return "CPA";
        }
        if ("3".equals(promotionType)) {
            return "CPS+CPA";
        }
        return "CPS";
    }

    private String sanitizeDescription(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.replace("<br/>", "</br>").replace("<br>", "</br>");
        int splitIndex = normalized.lastIndexOf("</br>");
        if (splitIndex >= 0) {
            normalized = normalized.substring(splitIndex + "</br>".length());
        }
        return normalized.replaceAll("<[^>]+>", "").trim();
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Integer.valueOf(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }
}
