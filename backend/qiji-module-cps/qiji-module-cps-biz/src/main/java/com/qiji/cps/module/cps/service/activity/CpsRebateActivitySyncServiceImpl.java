package com.qiji.cps.module.cps.service.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsThirdPartyActivityVendorClient;
import com.qiji.cps.module.cps.client.dataoke.DtkActivityVendorClient;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityCategory;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityClient;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityItem;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityListRequest;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityPage;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkSecondaryCategory;
import com.qiji.cps.module.cps.client.haodanku.activity.HaodankuActivityVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.jutuike.JutuikeUnionVendorClient;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CpsRebateActivitySyncServiceImpl {

    private static final int REBATE_DESC_MAX_LENGTH = 255;

    private static final String SOURCE_HAODANKU = "haodanku";
    private static final String HDK_EXTERNAL_PREFIX = "hdk:";
    private static final String VENDOR_ALL = "all";
    private static final int DEFAULT_MAX_SYNC_PAGES = 100;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private HdkActivityClient hdkActivityClient;

    @Resource
    private HaodankuActivityVendorClient haodankuActivityVendorClient;

    @Resource
    private JutuikeUnionVendorClient jutuikeUnionVendorClient;

    @Resource
    private DtkActivityVendorClient dtkActivityVendorClient;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @Resource
    private CpsRebateActivityMapper activityMapper;

    public CpsRebateActivitySyncResult syncHaodankuActivities(CpsRebateActivitySyncRequest request) {
        CpsRebateActivitySyncResult result = CpsRebateActivitySyncResult.builder().build();
        List<HdkActivityCategory> categories = hdkActivityClient.fetchCategories();
        int maxPages = resolveMaxPages(request);
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

    public CpsRebateActivitySyncResult syncThirdPartyActivities(CpsRebateActivitySyncRequest request) {
        if (isSyncAllVendors(request)) {
            return syncAllSupportedActivities(request);
        }
        CpsRebateActivitySyncResult result = CpsRebateActivitySyncResult.builder().build();
        CpsThirdPartyActivityVendorClient vendorClient = resolveThirdPartyActivityClient(request.getVendorCode());
        if (vendorClient == null) {
            result.setSkippedCount(1);
            return result;
        }
        CpsVendorConfig config = platformClientFactory.getVendorConfig(vendorClient.getVendorCode(),
                vendorClient.getPlatformCode());
        if (config == null && requiresVendorConfig(vendorClient)) {
            result.setSkippedCount(1);
            return result;
        }
        int maxPages = resolveMaxPages(request);
        int pageSize = request.getPageSize() == null || request.getPageSize() <= 0 ? 20 : request.getPageSize();
        for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
            CpsThirdPartyActivityRequest activityRequest = CpsThirdPartyActivityRequest.builder()
                    .vendorCode(request.getVendorCode())
                    .platformCode(request.getPlatformCode())
                    .keyword(request.getKeyword())
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .categoryName(request.getKeyword())
                    .build();
            CpsThirdPartyPage<CpsThirdPartyActivity> page;
            try {
                page = vendorClient.fetchActivities(activityRequest, config);
            } catch (Exception e) {
                log.warn("[CpsRebateActivitySyncService] 第三方活动拉取失败: vendorCode={}, pageNo={}",
                        vendorClient.getVendorCode(), pageNo, e);
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            if (page == null || page.getList() == null || page.getList().isEmpty()) {
                break;
            }
            for (CpsThirdPartyActivity item : page.getList()) {
                if (shouldSkipThirdPartyActivity(request, item)) {
                    result.setSkippedCount(result.getSkippedCount() + 1);
                    continue;
                }
                upsertThirdPartyActivity(request, result, item);
            }
            if (isLastThirdPartyPage(page, pageNo, pageSize)) {
                break;
            }
        }
        return result;
    }

    private boolean isSyncAllVendors(CpsRebateActivitySyncRequest request) {
        return request != null && VENDOR_ALL.equalsIgnoreCase(request.getVendorCode());
    }

    private CpsRebateActivitySyncResult syncAllSupportedActivities(CpsRebateActivitySyncRequest request) {
        CpsRebateActivitySyncResult aggregate = CpsRebateActivitySyncResult.builder().build();
        mergeResult(aggregate, syncThirdPartyActivities(request.toBuilder()
                .vendorCode(CpsVendorCodeEnum.DATAOKE.getCode())
                .platformCode(CpsPlatformCodeEnum.TAOBAO.getCode())
                .build()));
        mergeResult(aggregate, syncThirdPartyActivities(request.toBuilder()
                .vendorCode(CpsVendorCodeEnum.HAODANKU.getCode())
                .platformCode(null)
                .build()));
        mergeResult(aggregate, syncThirdPartyActivities(request.toBuilder()
                .vendorCode(CpsVendorCodeEnum.JUTUIKE.getCode())
                .platformCode(null)
                .build()));
        return aggregate;
    }

    private void mergeResult(CpsRebateActivitySyncResult aggregate, CpsRebateActivitySyncResult item) {
        if (item == null) {
            return;
        }
        aggregate.setInsertedCount(aggregate.getInsertedCount() + item.getInsertedCount());
        aggregate.setUpdatedCount(aggregate.getUpdatedCount() + item.getUpdatedCount());
        aggregate.setSkippedCount(aggregate.getSkippedCount() + item.getSkippedCount());
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
                break;
            }
            for (HdkActivityItem item : page.getItems()) {
                upsertActivity(request, result, category, secondaryCategory, item);
            }
            if (page.getCountPage() != null && pageNo >= page.getCountPage()) {
                break;
            }
        }
    }

    private int resolveMaxPages(CpsRebateActivitySyncRequest request) {
        return request.getMaxPages() == null || request.getMaxPages() <= 0
                ? DEFAULT_MAX_SYNC_PAGES
                : request.getMaxPages();
    }

    private boolean isLastThirdPartyPage(CpsThirdPartyPage<CpsThirdPartyActivity> page, int pageNo, int pageSize) {
        if (page.getTotal() != null && page.getTotal() >= 0) {
            return (long) pageNo * pageSize >= page.getTotal();
        }
        return page.getList().size() < pageSize;
    }

    private void upsertActivity(CpsRebateActivitySyncRequest request, CpsRebateActivitySyncResult result,
                                HdkActivityCategory category, HdkSecondaryCategory secondaryCategory,
                                HdkActivityItem item) {
        String platformCode = HaodankuActivityVendorClient.normalizeActivityPlatformCode(item.getPlatform());
        if (!StringUtils.hasText(item.getId())
                || !HaodankuActivityVendorClient.supportsOfficialActivityPromotionLink(platformCode)
                || !matchesRequestedPlatform(request.getPlatformCode(), platformCode)
                || !StringUtils.hasText(firstText(item.getActivityId(), item.getActivityUrl()))) {
            result.setSkippedCount(result.getSkippedCount() + 1);
            return;
        }
        String externalActivityId = HDK_EXTERNAL_PREFIX + platformCode + ":" + item.getId();
        CpsRebateActivityDO activity = toActivity(request, category, secondaryCategory, item, externalActivityId);
        CpsRebateActivityDO existing = activityMapper.selectBySourceTypeAndExternalActivityId(SOURCE_HAODANKU,
                externalActivityId);
        if (existing == null && StringUtils.hasText(item.getActivityId())) {
            existing = activityMapper.selectBySourceTypeAndExternalActivityId(SOURCE_HAODANKU,
                    HDK_EXTERNAL_PREFIX + item.getActivityId());
        }
        if (existing == null) {
            activityMapper.insert(activity);
            result.setInsertedCount(result.getInsertedCount() + 1);
            return;
        }
        activity.setId(existing.getId());
        activityMapper.updateById(activity);
        result.setUpdatedCount(result.getUpdatedCount() + 1);
    }

    private void upsertThirdPartyActivity(CpsRebateActivitySyncRequest request, CpsRebateActivitySyncResult result,
                                         CpsThirdPartyActivity item) {
        String sourceType = firstText(item.getSourceType(), request.getVendorCode(),
                CpsVendorCodeEnum.HAODANKU.getCode());
        String externalActivityId = item.getExternalActivityId();
        if (!StringUtils.hasText(externalActivityId)) {
            result.setSkippedCount(result.getSkippedCount() + 1);
            return;
        }
        CpsRebateActivityDO activity = toActivity(request, item);
        CpsRebateActivityDO existing = findExistingThirdPartyActivity(sourceType, externalActivityId, item);
        if (existing == null) {
            activityMapper.insert(activity);
            result.setInsertedCount(result.getInsertedCount() + 1);
            return;
        }
        activity.setId(existing.getId());
        activityMapper.updateById(activity);
        result.setUpdatedCount(result.getUpdatedCount() + 1);
    }

    private CpsRebateActivityDO findExistingThirdPartyActivity(String sourceType, String externalActivityId,
                                                               CpsThirdPartyActivity item) {
        CpsRebateActivityDO existing = activityMapper.selectBySourceTypeAndExternalActivityId(sourceType,
                externalActivityId);
        if (existing != null) {
            return existing;
        }
        String legacyExternalActivityId = getLegacyExternalActivityId(item);
        if (!StringUtils.hasText(legacyExternalActivityId) || externalActivityId.equals(legacyExternalActivityId)) {
            return null;
        }
        return activityMapper.selectBySourceTypeAndExternalActivityId(sourceType, legacyExternalActivityId);
    }

    private String getLegacyExternalActivityId(CpsThirdPartyActivity item) {
        Map<String, Object> extraFields = item.getExtraFields();
        if (extraFields == null) {
            return null;
        }
        Object value = extraFields.get("legacyExternalActivityId");
        return value == null ? null : String.valueOf(value);
    }

    private boolean shouldSkipThirdPartyActivity(CpsRebateActivitySyncRequest request, CpsThirdPartyActivity item) {
        if (!CpsVendorCodeEnum.HAODANKU.getCode().equalsIgnoreCase(request.getVendorCode())) {
            return false;
        }
        String platformCode = HaodankuActivityVendorClient.normalizeActivityPlatformCode(item.getPlatformCode());
        return !HaodankuActivityVendorClient.supportsOfficialActivityPromotionLink(platformCode)
                || !matchesRequestedPlatform(request.getPlatformCode(), platformCode)
                || !StringUtils.hasText(firstText(item.getPromotionActivityId(), item.getJumpUrl()));
    }

    private boolean matchesRequestedPlatform(String requestedPlatformCode, String activityPlatformCode) {
        return !StringUtils.hasText(requestedPlatformCode)
                || HaodankuActivityVendorClient.normalizeActivityPlatformCode(requestedPlatformCode)
                .equals(activityPlatformCode);
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
                .platformCode(HaodankuActivityVendorClient.normalizeActivityPlatformCode(item.getPlatform()))
                .mainPic(item.getActivityPic())
                .shortDesc(shortDesc)
                .rebateDesc(item.getCommissionRate())
                .billingType(resolveBillingType(item.getPromotionType()))
                .promotionCount(parseInt(item.getPromotionNum()))
                .sourceType(SOURCE_HAODANKU)
                .externalActivityId(externalActivityId)
                .promotionActivityId(item.getActivityId())
                .vendorMetadata(toJson(buildHaodankuMetadata(item)))
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

    private CpsRebateActivityDO toActivity(CpsRebateActivitySyncRequest request, CpsThirdPartyActivity item) {
        return CpsRebateActivityDO.builder()
                .activityName(item.getActivityName())
                .activityType(firstText(item.getActivityType(), item.getTagText(), "其他活动"))
                .platformCode(resolveThirdPartyPlatformCode(request, item))
                .mainPic(item.getMainPic())
                .shortDesc(item.getShortDesc())
                .rebateDesc(truncate(item.getRebateDesc(), REBATE_DESC_MAX_LENGTH))
                .billingType(firstText(item.getBillingType(), "CPS"))
                .promotionCount(item.getPromotionCount() == null ? 0 : item.getPromotionCount())
                .sourceType(firstText(item.getSourceType(), request.getVendorCode(),
                        CpsVendorCodeEnum.HAODANKU.getCode()))
                .externalActivityId(item.getExternalActivityId())
                .promotionActivityId(item.getPromotionActivityId())
                .vendorMetadata(firstText(item.getVendorMetadata(), toJson(item.getExtraFields())))
                .tagText(firstText(item.getTagText(), item.getActivityType()))
                .jumpType(firstText(item.getJumpType(), StringUtils.hasText(item.getJumpUrl()) ? "url" : "search"))
                .jumpUrl(item.getJumpUrl())
                .searchKeyword(firstText(item.getSearchKeyword(), item.getActivityName()))
                .startTime(item.getStartTime())
                .endTime(item.getEndTime())
                .sort(0)
                .status(1)
                .build();
    }

    private String resolveThirdPartyPlatformCode(CpsRebateActivitySyncRequest request, CpsThirdPartyActivity item) {
        String platformCode = firstText(item.getPlatformCode(), request.getPlatformCode());
        if (StringUtils.hasText(platformCode)) {
            return platformCode;
        }
        CpsThirdPartyActivityVendorClient vendorClient = resolveThirdPartyActivityClient(request.getVendorCode());
        return vendorClient == null ? null : vendorClient.getPlatformCode();
    }

    private Map<String, Object> buildHaodankuMetadata(HdkActivityItem item) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("row_id", item.getId());
        metadata.put("activity_id", item.getActivityId());
        metadata.put("activity_url", item.getActivityUrl());
        metadata.put("platform", item.getPlatform());
        metadata.put("is_channel", item.getIsChannel());
        String platformCode = HaodankuActivityVendorClient.normalizeActivityPlatformCode(item.getPlatform());
        metadata.put("supportsList", true);
        metadata.put("supportsPromotionLink",
                HaodankuActivityVendorClient.supportsOfficialActivityPromotionLink(platformCode));
        metadata.put("supportsOrders", "eleme".equals(platformCode));
        metadata.put("supportsMiniProgram", "eleme".equals(platformCode)
                && item.getIsChannel() != null && item.getIsChannel() == 1);
        metadata.put("supportsLocalLife", isLocalLifePlatform(platformCode));
        return metadata;
    }

    private boolean isLocalLifePlatform(String platformCode) {
        return "meituan".equals(platformCode)
                || "eleme".equals(platformCode)
                || "local_life".equals(platformCode)
                || "fliggy".equals(platformCode);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("[CpsRebateActivitySyncService] 活动供应商元数据序列化失败", e);
            return null;
        }
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

    private String truncate(String value, int maxCodePoints) {
        if (value == null || value.codePointCount(0, value.length()) <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
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
        return null;
    }

    private CpsThirdPartyActivityVendorClient resolveThirdPartyActivityClient(String vendorCode) {
        String normalizedVendorCode = StringUtils.hasText(vendorCode)
                ? vendorCode
                : CpsVendorCodeEnum.HAODANKU.getCode();
        if (CpsVendorCodeEnum.HAODANKU.getCode().equalsIgnoreCase(normalizedVendorCode)) {
            return haodankuActivityVendorClient;
        }
        if (CpsVendorCodeEnum.DATAOKE.getCode().equalsIgnoreCase(normalizedVendorCode)) {
            return dtkActivityVendorClient;
        }
        if (CpsVendorCodeEnum.JUTUIKE.getCode().equalsIgnoreCase(normalizedVendorCode)) {
            return jutuikeUnionVendorClient;
        }
        return null;
    }

    private boolean requiresVendorConfig(CpsThirdPartyActivityVendorClient vendorClient) {
        return CpsVendorCodeEnum.DATAOKE.getCode().equalsIgnoreCase(vendorClient.getVendorCode())
                || CpsVendorCodeEnum.JUTUIKE.getCode().equalsIgnoreCase(vendorClient.getVendorCode());
    }
}
