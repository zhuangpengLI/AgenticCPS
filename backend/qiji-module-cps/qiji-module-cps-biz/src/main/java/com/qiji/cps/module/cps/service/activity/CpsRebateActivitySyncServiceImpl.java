package com.qiji.cps.module.cps.service.activity;

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
import java.util.List;

@Slf4j
@Service
public class CpsRebateActivitySyncServiceImpl {

    private static final String SOURCE_HAODANKU = "haodanku";
    private static final String HDK_EXTERNAL_PREFIX = "hdk:";
    private static final String VENDOR_ALL = "all";
    private static final int DEFAULT_MAX_SYNC_PAGES = 100;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        String externalIdValue = firstText(item.getActivityId(), item.getId());
        if (!StringUtils.hasText(externalIdValue)) {
            result.setSkippedCount(result.getSkippedCount() + 1);
            return;
        }
        String externalActivityId = HDK_EXTERNAL_PREFIX + externalIdValue;
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
        CpsRebateActivityDO existing = activityMapper.selectBySourceTypeAndExternalActivityId(sourceType,
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
                .platformCode(HaodankuActivityVendorClient.normalizeActivityPlatformCode(item.getPlatform()))
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

    private CpsRebateActivityDO toActivity(CpsRebateActivitySyncRequest request, CpsThirdPartyActivity item) {
        return CpsRebateActivityDO.builder()
                .activityName(item.getActivityName())
                .activityType(item.getActivityType())
                .platformCode(firstText(item.getPlatformCode(), request.getPlatformCode()))
                .mainPic(item.getMainPic())
                .shortDesc(item.getShortDesc())
                .rebateDesc(item.getRebateDesc())
                .billingType(firstText(item.getBillingType(), "CPS"))
                .promotionCount(item.getPromotionCount() == null ? 0 : item.getPromotionCount())
                .sourceType(firstText(item.getSourceType(), request.getVendorCode(),
                        CpsVendorCodeEnum.HAODANKU.getCode()))
                .externalActivityId(item.getExternalActivityId())
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
