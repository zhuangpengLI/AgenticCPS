package com.qiji.cps.module.cps.client.haodanku.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.qiji.cps.module.cps.client.CpsThirdPartyActivityVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyApiCategory;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 好单库活动统一适配器。
 */
@Component
public class HaodankuActivityVendorClient implements CpsThirdPartyActivityVendorClient {

    private static final String SOURCE_HAODANKU = "haodanku";
    private static final String EXTERNAL_PREFIX = "hdk:";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private HdkActivityClient hdkActivityClient;

    @Override
    public String getVendorCode() {
        return CpsVendorCodeEnum.HAODANKU.getCode();
    }

    @Override
    public String getPlatformCode() {
        return "activity";
    }

    @Override
    public CpsThirdPartyPage<CpsThirdPartyActivity> fetchActivities(CpsThirdPartyActivityRequest request,
                                                                    CpsVendorConfig config) {
        int pageNo = defaultInt(request.getPageNo(), 1);
        List<CpsThirdPartyActivity> activities = new ArrayList<>();
        List<HdkActivityCategory> categories = hdkActivityClient.fetchCategories();
        for (HdkActivityCategory category : categories) {
            List<HdkSecondaryCategory> secondaryCategories = category.getSecondaryCategories();
            if (secondaryCategories == null || secondaryCategories.isEmpty()) {
                activities.addAll(fetchActivitiesByCategory(request, category, null, pageNo));
                continue;
            }
            for (HdkSecondaryCategory secondaryCategory : secondaryCategories) {
                activities.addAll(fetchActivitiesByCategory(request, category, secondaryCategory, pageNo));
            }
        }
        return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                .list(activities)
                .total((long) activities.size())
                .pageNo(pageNo)
                .pageSize(defaultInt(request.getPageSize(), 20))
                .nextPageId(String.valueOf(pageNo + 1))
                .build();
    }

    private List<CpsThirdPartyActivity> fetchActivitiesByCategory(CpsThirdPartyActivityRequest request,
                                                                  HdkActivityCategory category,
                                                                  HdkSecondaryCategory secondaryCategory,
                                                                  int pageNo) {
        HdkActivityPage page = hdkActivityClient.fetchActivities(HdkActivityListRequest.builder()
                .pageNo(pageNo)
                .keyword(firstText(request.getKeyword(), request.getCategoryName()))
                .catId(category.getCatId())
                .secondaryCatId(secondaryCategory == null ? null : secondaryCategory.getSecondaryCatId())
                .order(1)
                .build());
        if (page == null || page.getItems() == null || page.getItems().isEmpty()) {
            return Collections.emptyList();
        }
        List<CpsThirdPartyActivity> activities = new ArrayList<>();
        String activityType = secondaryCategory == null ? category.getName() : secondaryCategory.getName();
        for (HdkActivityItem item : page.getItems()) {
            String rowId = item.getId();
            String platformCode = normalizeActivityPlatformCode(item.getPlatform());
            if (!StringUtils.hasText(rowId)
                    || !supportsOfficialActivityPromotionLink(platformCode)
                    || !matchesRequestedPlatform(request.getPlatformCode(), platformCode)
                    || !hasPromotionMaterial(item)) {
                continue;
            }
            Map<String, Object> extraFields = buildExtraFields(category, secondaryCategory, item);
            activities.add(CpsThirdPartyActivity.builder()
                    .sourceType(SOURCE_HAODANKU)
                    .externalActivityId(EXTERNAL_PREFIX + platformCode + ":" + rowId)
                    .promotionActivityId(item.getActivityId())
                    .activityName(firstText(item.getActivityName(), activityType))
                    .activityType(activityType)
                    .platformCode(platformCode)
                    .mainPic(item.getActivityPic())
                    .shortDesc(sanitizeDescription(item.getDescribe()))
                    .rebateDesc(item.getCommissionRate())
                    .billingType(resolveBillingType(item.getPromotionType()))
                    .promotionCount(parseInt(item.getPromotionNum()))
                    .tagText(firstText(item.getActivityLabel(), activityType))
                    .jumpType(StringUtils.hasText(item.getActivityUrl()) ? "url" : "search")
                    .jumpUrl(item.getActivityUrl())
                    .searchKeyword(firstText(item.getActivityName(), activityType))
                    .startTime(parseTime(item.getStartTime()))
                    .endTime(parseTime(item.getEndTime()))
                    .extraFields(extraFields)
                    .rawPayload(null)
                    .build());
        }
        return activities;
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

    private Map<String, Object> buildExtraFields(HdkActivityCategory category, HdkSecondaryCategory secondaryCategory,
                                                  HdkActivityItem item) {
        Map<String, Object> extraFields = new LinkedHashMap<>();
        extraFields.put("category_id", category.getCatId());
        extraFields.put("category_name", category.getName());
        if (secondaryCategory != null) {
            extraFields.put("secondary_category_id", secondaryCategory.getSecondaryCatId());
            extraFields.put("secondary_category_name", secondaryCategory.getName());
        }
        extraFields.put("activity_label", item.getActivityLabel());
        extraFields.put("row_id", item.getId());
        extraFields.put("activity_id", item.getActivityId());
        extraFields.put("activity_url", item.getActivityUrl());
        extraFields.put("platform", item.getPlatform());
        extraFields.put("describe", item.getDescribe());
        extraFields.put("commission_rate", item.getCommissionRate());
        extraFields.put("promotion_type", item.getPromotionType());
        extraFields.put("activity_date", item.getActivityDate());
        extraFields.put("is_channel", item.getIsChannel());
        if (StringUtils.hasText(item.getActivityId())) {
            extraFields.put("legacyExternalActivityId", EXTERNAL_PREFIX + item.getActivityId());
        }
        return extraFields;
    }

    public static String normalizeActivityPlatformCode(String platform) {
        return switch (platform == null ? "" : platform) {
            case "1", "15" -> CpsPlatformCodeEnum.TAOBAO.getCode();
            case "2" -> CpsPlatformCodeEnum.JD.getCode();
            case "3" -> CpsPlatformCodeEnum.PDD.getCode();
            case "4", "9", "88" -> CpsPlatformCodeEnum.DOUYIN.getCode();
            case "6" -> CpsPlatformCodeEnum.MEITUAN.getCode();
            case "7" -> "eleme";
            case "8", "10", "12", "13", "99" -> "local_life";
            case "16" -> "fliggy";
            default -> platform;
        };
    }

    /**
     * 只有已经在活动推广服务中完成官方转链和响应解析的平台，才允许进入活动同步。
     * 文档中存在接口但尚未完成参数映射的平台不能提前放行。
     */
    public static boolean supportsOfficialActivityPromotionLink(String platformCode) {
        String normalized = normalizeActivityPlatformCode(platformCode);
        return CpsPlatformCodeEnum.TAOBAO.getCode().equals(normalized) || "eleme".equals(normalized);
    }

    private boolean matchesRequestedPlatform(String requestedPlatformCode, String activityPlatformCode) {
        return !StringUtils.hasText(requestedPlatformCode)
                || normalizeActivityPlatformCode(requestedPlatformCode).equals(activityPlatformCode);
    }

    private boolean hasPromotionMaterial(HdkActivityItem item) {
        return StringUtils.hasText(item.getActivityId()) || StringUtils.hasText(item.getActivityUrl());
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private Integer defaultInt(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }
}
