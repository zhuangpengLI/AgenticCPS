package com.qiji.cps.module.cps.service.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPageReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivitySaveReqVO;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dataoke.DtkActivityVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.haodanku.activity.HaodankuActivityVendorClient;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityClient;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.service.adzone.CpsAdzoneService;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
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
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.REBATE_ACTIVITY_JUMP_INVALID;
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
    private static final String SOURCE_DATAOKE = "dataoke";
    private static final String SOURCE_HAODANKU = "haodanku";
    private static final String VENDOR_DATAOKE = "dataoke";
    private static final String VENDOR_HAODANKU = "haodanku";
    private static final String PLATFORM_TAOBAO = "taobao";
    private static final String PLATFORM_ELEME = "eleme";
    private static final String DTK_EXTERNAL_PREFIX = "dtk:";
    private static final String LINK_STATUS_SUCCESS = "SUCCESS";
    private static final String LINK_STATUS_INTERNAL_FALLBACK = "INTERNAL_FALLBACK";
    private static final String LINK_STATUS_FAILED = "FAILED";
    private static final String LINK_TYPE_EXTERNAL_PROMOTION = "EXTERNAL_PROMOTION";
    private static final String LINK_TYPE_INTERNAL_LANDING = "INTERNAL_LANDING";
    private static final String LINK_TYPE_NONE = "NONE";
    private static final String GOODS_SQUARE_PATH = "/cps-ops/goods/square";
    private static final String ATTRIBUTION_MEMBER_TRACKED = "MEMBER_TRACKED";
    private static final String ATTRIBUTION_CHANNEL_TRACKED = "CHANNEL_TRACKED";
    private static final String ATTRIBUTION_UNTRACKED = "UNTRACKED";
    private static final String ATTRIBUTION_TYPE_SID = "SID";
    private static final String SID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";
    private static final int SID_LENGTH = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private CpsRebateActivityMapper activityMapper;

    @Resource
    private CpsPlatformService platformService;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @Resource
    private DtkActivityVendorClient dtkActivityVendorClient;

    @Resource
    private HdkActivityClient hdkActivityClient;

    @Resource
    private CpsAdzoneService adzoneService;

    @Resource
    private CpsTransferRecordMapper transferRecordMapper;

    @Override
    public Long createActivity(CpsRebateActivitySaveReqVO createReqVO) {
        validateJumpTarget(createReqVO);
        CpsRebateActivityDO activity = BeanUtils.toBean(createReqVO, CpsRebateActivityDO.class);
        activityMapper.insert(activity);
        return activity.getId();
    }

    @Override
    public void updateActivity(CpsRebateActivitySaveReqVO updateReqVO) {
        validateActivityExists(updateReqVO.getId());
        validateJumpTarget(updateReqVO);
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
        return activityMapper.selectEnabledList(LocalDateTime.now()).stream()
                .filter(this::hasUsablePromotionCapability)
                .toList();
    }

    @Override
    public CpsRebateActivityCenterRespVO getActivityCenter(CpsRebateActivityCenterReqVO reqVO) {
        List<CpsRebateActivityDO> enabledActivities = activityMapper.selectEnabledList(LocalDateTime.now()).stream()
                .filter(this::hasUsablePromotionCapability)
                .toList();
        List<CpsPlatformDO> enabledPlatforms = loadEnabledPlatforms();
        Map<String, CpsPlatformDO> platformMap = enabledPlatforms.stream()
                .filter(platform -> StringUtils.hasText(platform.getPlatformCode()))
                .collect(Collectors.toMap(CpsPlatformDO::getPlatformCode, Function.identity(), (left, right) -> left));

        List<CpsRebateActivityDO> sourceFilteredActivities = enabledActivities.stream()
                .filter(activity -> matchSourceType(activity, reqVO.getSourceType()))
                .toList();
        List<CpsRebateActivityDO> filtered = sourceFilteredActivities.stream()
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
                .tabs(buildTabs(sourceFilteredActivities, enabledPlatforms))
                .billingTypeOptions(buildBillingTypeOptions(sourceFilteredActivities))
                .cards(filtered.subList(fromIndex, toIndex).stream()
                        .map(activity -> toCard(activity, platformMap))
                        .toList())
                .total((long) total)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public CpsRebateActivityPromotionRespVO generatePromotionContent(CpsRebateActivityPromotionReqVO reqVO) {
        return generatePromotionContent(reqVO, null);
    }

    @Override
    public CpsRebateActivityPromotionRespVO generatePromotionContent(CpsRebateActivityPromotionReqVO reqVO,
                                                                      Long memberId) {
        CpsRebateActivityDO activity = getActivity(reqVO.getActivityId());
        if (activity == null) {
            throw exception(REBATE_ACTIVITY_NOT_EXISTS);
        }
        if (SOURCE_DATAOKE.equalsIgnoreCase(firstText(activity.getSourceType(), ""))) {
            return generateDtkActivityLink(activity, reqVO);
        }
        if (SOURCE_HAODANKU.equalsIgnoreCase(firstText(activity.getSourceType(), ""))) {
            return generateHaodankuActivityLink(activity, reqVO, memberId);
        }
        if ("url".equals(activity.getJumpType())) {
            String promotionUrl = replaceUrlPlaceholders(activity.getJumpUrl(), reqVO);
            if (!isSafeExternalUrl(promotionUrl)) {
                return buildFailedPromotion(activity, reqVO, reqVO.getAdzoneId(), "活动未配置有效的公网跳转地址");
            }
            return buildExternalPromotion(activity, reqVO, reqVO.getAdzoneId(), promotionUrl, null, null,
                    "活动推广链接已生成");
        }
        if ("search".equals(activity.getJumpType())) {
            return buildInternalPromotion(activity, reqVO, reqVO.getAdzoneId(), "已生成站内商品广场落地页");
        }
        return buildFailedPromotion(activity, reqVO, reqVO.getAdzoneId(), "当前活动没有可用的跳转配置");
    }

    private CpsRebateActivityPromotionRespVO generateDtkActivityLink(CpsRebateActivityDO activity,
                                                                      CpsRebateActivityPromotionReqVO reqVO) {
        String promotionSceneId = parseDtkPromotionSceneId(activity);
        if (!StringUtils.hasText(promotionSceneId)) {
            return buildFailedPromotion(activity, reqVO, reqVO.getAdzoneId(), "大淘客活动缺少官方转链场景 ID");
        }
        CpsVendorConfig config = platformClientFactory.getVendorConfig(VENDOR_DATAOKE, PLATFORM_TAOBAO);
        if (config == null) {
            return buildFailedPromotion(activity, reqVO, reqVO.getAdzoneId(), "未配置大淘客淘宝供应商，无法生成官方活动链接");
        }

        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId(promotionSceneId);
        String actualAdzoneId = firstText(reqVO.getAdzoneId(), config.getDefaultAdzoneId());
        request.setAdzoneId(actualAdzoneId);
        request.setExternalId(reqVO.getChannelTag());
        CpsPromotionLinkResult linkResult = dtkActivityVendorClient.generateActivityLink(request, config);
        if (linkResult == null) {
            return buildFailedPromotion(activity, reqVO, actualAdzoneId, "大淘客官方活动转链暂不可用");
        }
        String promotionUrl = firstText(linkResult.getShortUrl(), linkResult.getLongUrl());
        if (!isSafeExternalUrl(promotionUrl)) {
            return buildFailedPromotion(activity, reqVO, actualAdzoneId, "大淘客官方活动转链暂不可用：未返回有效公网链接");
        }

        String longTpwd = null;
        if (linkResult.getExtraFields() != null) {
            Object value = linkResult.getExtraFields().get("longTpwd");
            longTpwd = value == null ? null : String.valueOf(value);
        }
        return buildExternalPromotion(activity, reqVO, actualAdzoneId, promotionUrl, linkResult.getTpwd(), longTpwd,
                "大淘客官方活动推广链接已生成");
    }

    private CpsRebateActivityPromotionRespVO generateHaodankuActivityLink(CpsRebateActivityDO activity,
                                                                          CpsRebateActivityPromotionReqVO reqVO,
                                                                          Long memberId) {
        String platformCode = normalizePlatformCode(activity.getPlatformCode());
        if (!HaodankuActivityVendorClient.supportsOfficialActivityPromotionLink(platformCode)) {
            return buildFailedPromotion(activity, reqVO, reqVO.getAdzoneId(),
                    "当前好单库活动平台尚未接入官方活动转链");
        }
        CpsVendorConfig config = platformClientFactory.getVendorConfig(VENDOR_HAODANKU, platformCode);
        if (config == null && PLATFORM_ELEME.equals(platformCode)) {
            // 好单库普通活动接口共用同一 apikey；允许闪购复用淘宝账号，平台专属配置仍优先。
            config = platformClientFactory.getVendorConfig(VENDOR_HAODANKU, PLATFORM_TAOBAO);
        }
        if (config == null) {
            return buildFailedPromotion(activity, reqVO, reqVO.getAdzoneId(),
                    "未配置好单库" + (PLATFORM_ELEME.equals(platformCode) ? "淘宝闪购" : "淘宝")
                            + "供应商，无法生成官方活动链接");
        }
        if (PLATFORM_TAOBAO.equals(platformCode)
                && !StringUtils.hasText(firstText(config.getAuthToken(),
                config.getExtraConfig() == null ? null : config.getExtraConfig().get("tb_name")))) {
            return buildFailedPromotion(activity, reqVO, reqVO.getAdzoneId(),
                    "好单库淘宝会场转链缺少授权淘宝账号名，请在供应商认证令牌或扩展配置 tb_name 中补齐");
        }
        CpsAdzoneDO memberAdzone = memberId == null || !PLATFORM_TAOBAO.equals(platformCode)
                ? null : adzoneService.getMemberAdzone(platformCode, memberId);
        boolean trustedTaobaoRelation = memberAdzone != null
                && memberId.equals(memberAdzone.getRelationId())
                && StringUtils.hasText(memberAdzone.getExternalRelationId());
        String actualAdzoneId = memberId == null
                ? firstText(reqVO.getAdzoneId(), config.getDefaultAdzoneId())
                : trustedTaobaoRelation ? memberAdzone.getAdzoneId() : config.getDefaultAdzoneId();
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId(firstText(activity.getPromotionActivityId(), metadataText(activity, "activity_id"),
                parseLegacyHaodankuPromotionActivityId(activity.getExternalActivityId())));
        request.setItemLink(firstText(metadataText(activity, "activity_url"), activity.getJumpUrl()));
        request.setAdzoneId(actualAdzoneId);
        request.setExternalId(memberId == null ? reqVO.getChannelTag() : null);
        if (trustedTaobaoRelation) {
            request.setRelationId(memberAdzone.getExternalRelationId());
        }

        CpsTransferRecordDO sidRecord = null;
        String sid = null;
        CpsPromotionLinkResult linkResult;
        if (PLATFORM_ELEME.equals(platformCode)) {
            if (memberId != null) {
                sidRecord = createPendingSidRecord(activity, memberId, actualAdzoneId);
                sid = sidRecord == null ? null : sidRecord.getAttributionToken();
            }
            linkResult = hdkActivityClient.generateElemeActivityLink(request, config, sid);
        } else {
            linkResult = hdkActivityClient.generateConferenceLink(request, config, activity.getActivityName());
        }
        String promotionUrl = resolveHaodankuPromotionTarget(linkResult);
        if (linkResult == null
                || (!StringUtils.hasText(promotionUrl) && !StringUtils.hasText(linkResult.getTpwd()))) {
            return buildFailedPromotion(activity, reqVO, actualAdzoneId, "好单库官方活动转链暂不可用");
        }
        if (sidRecord != null) {
            transferRecordMapper.updateById(CpsTransferRecordDO.builder()
                    .id(sidRecord.getId())
                    .promotionUrl(promotionUrl)
                    .taoCommand(linkResult.getTpwd())
                    .status(1)
                    .build());
        }
        String attributionStatus = trustedTaobaoRelation || sidRecord != null
                ? ATTRIBUTION_MEMBER_TRACKED : memberId == null && StringUtils.hasText(actualAdzoneId)
                ? ATTRIBUTION_CHANNEL_TRACKED : ATTRIBUTION_UNTRACKED;
        String attributionMessage = ATTRIBUTION_MEMBER_TRACKED.equals(attributionStatus)
                ? "已使用可信会员归因标识，订单同步后可自动绑定会员"
                : ATTRIBUTION_CHANNEL_TRACKED.equals(attributionStatus)
                ? "当前链接仅支持渠道跟踪，不能自动绑定具体会员"
                : "当前链接没有可信会员归因标识，下单后可通过订单号申请找回";
        return buildExternalPromotion(activity, reqVO, actualAdzoneId, promotionUrl, linkResult.getTpwd(), null,
                "好单库官方活动推广链接已生成", attributionStatus, attributionMessage);
    }

    private CpsRebateActivityPromotionRespVO buildExternalPromotion(CpsRebateActivityDO activity,
                                                                     CpsRebateActivityPromotionReqVO reqVO,
                                                                     String actualAdzoneId, String promotionUrl,
                                                                     String tpwd, String longTpwd, String message) {
        return buildExternalPromotion(activity, reqVO, actualAdzoneId, promotionUrl, tpwd, longTpwd, message,
                ATTRIBUTION_UNTRACKED, "当前链接未建立会员归因，下单后可通过订单号申请找回");
    }

    private CpsRebateActivityPromotionRespVO buildExternalPromotion(CpsRebateActivityDO activity,
                                                                     CpsRebateActivityPromotionReqVO reqVO,
                                                                     String actualAdzoneId, String promotionUrl,
                                                                     String tpwd, String longTpwd, String message,
                                                                     String attributionStatus,
                                                                     String attributionMessage) {
        return CpsRebateActivityPromotionRespVO.builder()
                .linkStatus(LINK_STATUS_SUCCESS)
                .linkType(LINK_TYPE_EXTERNAL_PROMOTION)
                .linkMessage(message)
                .attributionStatus(attributionStatus)
                .attributionMessage(attributionMessage)
                .activityId(activity.getId())
                .activityName(activity.getActivityName())
                .platformCode(normalizePlatformCode(activity.getPlatformCode()))
                .adzoneId(actualAdzoneId)
                .channelTag(reqVO.getChannelTag())
                .promotionUrl(promotionUrl)
                .tpwd(tpwd)
                .longTpwd(longTpwd)
                .promotionContent(buildPromotionContent(activity, reqVO, promotionUrl, tpwd, actualAdzoneId))
                .build();
    }

    private CpsTransferRecordDO createPendingSidRecord(CpsRebateActivityDO activity, Long memberId,
                                                       String adzoneId) {
        for (int attempt = 0; attempt < 5; attempt++) {
            CpsTransferRecordDO record = CpsTransferRecordDO.builder()
                    .memberId(memberId)
                    .platformCode(PLATFORM_ELEME)
                    .vendorCode(VENDOR_HAODANKU)
                    .activityId(activity.getId())
                    .attributionType(ATTRIBUTION_TYPE_SID)
                    .attributionToken(generateSid())
                    .originalContent(firstText(metadataText(activity, "activity_url"), activity.getJumpUrl()))
                    .itemId(firstText(activity.getPromotionActivityId(), metadataText(activity, "activity_id")))
                    .itemTitle(activity.getActivityName())
                    .adzoneId(adzoneId)
                    .expireTime(resolveAttributionExpireTime(activity))
                    .status(0)
                    .build();
            try {
                transferRecordMapper.insert(record);
                return record;
            } catch (org.springframework.dao.DuplicateKeyException ignored) {
                // 极低概率令牌碰撞，重新生成后重试。
            }
        }
        return null;
    }

    private String generateSid() {
        StringBuilder sid = new StringBuilder(SID_LENGTH);
        for (int index = 0; index < SID_LENGTH; index++) {
            sid.append(SID_ALPHABET.charAt(SECURE_RANDOM.nextInt(SID_ALPHABET.length())));
        }
        return sid.toString();
    }

    private LocalDateTime resolveAttributionExpireTime(CpsRebateActivityDO activity) {
        LocalDateTime maximum = LocalDateTime.now().plusDays(30);
        return activity.getEndTime() != null && activity.getEndTime().isBefore(maximum)
                && activity.getEndTime().isAfter(LocalDateTime.now()) ? activity.getEndTime() : maximum;
    }

    private CpsRebateActivityPromotionRespVO buildInternalPromotion(CpsRebateActivityDO activity,
                                                                     CpsRebateActivityPromotionReqVO reqVO,
                                                                     String actualAdzoneId, String message) {
        String promotionUrl = buildPromotionUrl(activity, reqVO);
        return CpsRebateActivityPromotionRespVO.builder()
                .linkStatus(LINK_STATUS_INTERNAL_FALLBACK)
                .linkType(LINK_TYPE_INTERNAL_LANDING)
                .linkMessage(message)
                .activityId(activity.getId())
                .activityName(activity.getActivityName())
                .platformCode(normalizePlatformCode(activity.getPlatformCode()))
                .adzoneId(actualAdzoneId)
                .channelTag(reqVO.getChannelTag())
                .promotionUrl(promotionUrl)
                .promotionContent(buildPromotionContent(activity, reqVO, promotionUrl, null, actualAdzoneId))
                .build();
    }

    private CpsRebateActivityPromotionRespVO buildFailedPromotion(CpsRebateActivityDO activity,
                                                                   CpsRebateActivityPromotionReqVO reqVO,
                                                                   String actualAdzoneId, String message) {
        return CpsRebateActivityPromotionRespVO.builder()
                .linkStatus(LINK_STATUS_FAILED)
                .linkType(LINK_TYPE_NONE)
                .linkMessage(message)
                .activityId(activity.getId())
                .activityName(activity.getActivityName())
                .platformCode(normalizePlatformCode(activity.getPlatformCode()))
                .adzoneId(actualAdzoneId)
                .channelTag(reqVO.getChannelTag())
                .build();
    }

    private String parseDtkPromotionSceneId(CpsRebateActivityDO activity) {
        if (!SOURCE_DATAOKE.equalsIgnoreCase(firstText(activity.getSourceType(), ""))) {
            return null;
        }
        if (StringUtils.hasText(activity.getPromotionActivityId())) {
            return activity.getPromotionActivityId();
        }
        String externalActivityId = activity.getExternalActivityId();
        if (!StringUtils.hasText(externalActivityId) || !externalActivityId.startsWith(DTK_EXTERNAL_PREFIX)) {
            return null;
        }
        String promotionSceneId = externalActivityId.substring(DTK_EXTERNAL_PREFIX.length());
        return StringUtils.hasText(promotionSceneId) ? promotionSceneId : null;
    }

    private void validateActivityExists(Long id) {
        if (activityMapper.selectById(id) == null) {
            throw exception(REBATE_ACTIVITY_NOT_EXISTS);
        }
    }

    private void validateJumpTarget(CpsRebateActivitySaveReqVO reqVO) {
        if ("url".equals(reqVO.getJumpType())) {
            if (!isSafeExternalUrl(reqVO.getJumpUrl())) {
                throw exception(REBATE_ACTIVITY_JUMP_INVALID, "URL 跳转必须配置非本机的 HTTP(S) 地址");
            }
            return;
        }
        if ("search".equals(reqVO.getJumpType())) {
            if (!StringUtils.hasText(reqVO.getSearchKeyword())) {
                throw exception(REBATE_ACTIVITY_JUMP_INVALID, "搜索跳转必须配置搜索关键词");
            }
            return;
        }
        if (!"none".equals(reqVO.getJumpType())) {
            throw exception(REBATE_ACTIVITY_JUMP_INVALID, "不支持的跳转类型");
        }
    }

    private String buildPromotionUrl(CpsRebateActivityDO activity, CpsRebateActivityPromotionReqVO reqVO) {
        String keyword = firstText(activity.getSearchKeyword(), activity.getActivityName());
        String activityTag = firstText(activity.getTagText(), activity.getExternalActivityId(), activity.getActivityName());
        String path = GOODS_SQUARE_PATH + "?platformCode=" + encode(normalizePlatformCode(activity.getPlatformCode()))
                + "&keyword=" + encode(keyword)
                + "&activityTag=" + encode(activityTag);
        return path;
    }

    private String replaceUrlPlaceholders(String url, CpsRebateActivityPromotionReqVO reqVO) {
        return url.replace("{adzoneId}", encode(firstText(reqVO.getAdzoneId(), "")))
                .replace("{channelTag}", encode(firstText(reqVO.getChannelTag(), "")));
    }

    private String buildPromotionContent(CpsRebateActivityDO activity, CpsRebateActivityPromotionReqVO reqVO,
                                         String promotionUrl, String tpwd, String actualAdzoneId) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, activity.getActivityName());
        appendLine(builder, activity.getShortDesc());
        appendLine(builder, activity.getRebateDesc());
        if ("search".equals(activity.getJumpType()) && StringUtils.hasText(activity.getSearchKeyword())) {
            appendLine(builder, "搜索词：" + activity.getSearchKeyword());
        }
        appendLine(builder, buildActivityWindowText(activity));
        appendLine(builder, promotionUrl);
        if (StringUtils.hasText(tpwd)) {
            appendLine(builder, "淘口令：" + tpwd);
        }
        if (StringUtils.hasText(actualAdzoneId)) {
            appendLine(builder, "推广位：" + actualAdzoneId);
        }
        if (StringUtils.hasText(reqVO.getChannelTag())) {
            appendLine(builder, "渠道：" + reqVO.getChannelTag());
        }
        return builder.toString().trim();
    }

    private String buildActivityWindowText(CpsRebateActivityDO activity) {
        if (activity.getStartTime() == null && activity.getEndTime() == null) {
            return null;
        }
        if (activity.getStartTime() == null) {
            return "活动时间：截至" + activity.getEndTime();
        }
        if (activity.getEndTime() == null) {
            return "活动时间：" + activity.getStartTime() + "起";
        }
        return "活动时间：" + activity.getStartTime() + " 至 " + activity.getEndTime();
    }

    private void appendLine(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(value).append('\n');
        }
    }

    private String metadataText(CpsRebateActivityDO activity, String fieldName) {
        if (!StringUtils.hasText(activity.getVendorMetadata())) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(activity.getVendorMetadata());
            String value = root.path(fieldName).asText(null);
            return StringUtils.hasText(value) ? value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String parseLegacyHaodankuPromotionActivityId(String externalActivityId) {
        if (!StringUtils.hasText(externalActivityId) || !externalActivityId.startsWith("hdk:")) {
            return null;
        }
        String value = externalActivityId.substring("hdk:".length());
        return StringUtils.hasText(value) && !value.contains(":") ? value : null;
    }

    private boolean isSafeExternalUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            String candidate = value.trim()
                    .replace("{adzoneId}", "adzone")
                    .replace("{channelTag}", "channel");
            URI uri = URI.create(candidate);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    || !StringUtils.hasText(host)) {
                return false;
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return !"localhost".equals(normalizedHost)
                    && !normalizedHost.endsWith(".localhost")
                    && !normalizedHost.startsWith("127.")
                    && !normalizedHost.startsWith("10.")
                    && !normalizedHost.startsWith("192.168.")
                    && !isPrivate172Host(normalizedHost)
                    && !normalizedHost.startsWith("169.254.")
                    && !"0.0.0.0".equals(normalizedHost)
                    && !"::1".equals(normalizedHost)
                    && !"[::1]".equals(normalizedHost);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String resolveHaodankuPromotionTarget(CpsPromotionLinkResult linkResult) {
        if (linkResult == null) {
            return null;
        }
        return firstSafePromotionTarget(linkResult.getShortUrl(), linkResult.getLongUrl(), linkResult.getMobileUrl(),
                linkExtraText(linkResult, "taobaoSchemeUrl"), linkExtraText(linkResult, "alipayMiniUrl"));
    }

    private String firstSafePromotionTarget(String... values) {
        for (String value : values) {
            if (isSafeOfficialPromotionTarget(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isSafeOfficialPromotionTarget(String value) {
        if (isSafeExternalUrl(value)) {
            return true;
        }
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            return StringUtils.hasText(uri.getSchemeSpecificPart())
                    && ("eleme".equalsIgnoreCase(scheme)
                    || "tbopen".equalsIgnoreCase(scheme)
                    || "alipays".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String linkExtraText(CpsPromotionLinkResult linkResult, String key) {
        if (linkResult.getExtraFields() == null) {
            return null;
        }
        Object value = linkResult.getExtraFields().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private boolean isPrivate172Host(String host) {
        if (!host.startsWith("172.")) {
            return false;
        }
        String[] parts = host.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
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
                || platformCode.equals(normalizePlatformCode(activity.getPlatformCode()));
    }

    private boolean matchSourceType(CpsRebateActivityDO activity, String sourceType) {
        return !StringUtils.hasText(sourceType)
                || BILLING_TYPE_ALL.equalsIgnoreCase(sourceType)
                || sourceType.equalsIgnoreCase(firstText(activity.getSourceType(), ""));
    }

    private boolean hasUsablePromotionCapability(CpsRebateActivityDO activity) {
        return !SOURCE_HAODANKU.equalsIgnoreCase(firstText(activity.getSourceType(), ""))
                || HaodankuActivityVendorClient.supportsOfficialActivityPromotionLink(activity.getPlatformCode());
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
                .map(this::normalizePlatformCode)
                .filter(StringUtils::hasText)
                .forEach(platformCode -> metas.putIfAbsent(platformCode, fallbackMeta(platformCode)));

        Map<String, Integer> countMap = activities.stream()
                .filter(activity -> StringUtils.hasText(activity.getPlatformCode()))
                .collect(Collectors.groupingBy(activity -> normalizePlatformCode(activity.getPlatformCode()), Collectors.collectingAndThen(
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
        String platformCode = normalizePlatformCode(activity.getPlatformCode());
        CpsPlatformDO platform = platformMap.get(platformCode);
        PlatformMeta fallback = fallbackMeta(platformCode);
        return CpsRebateActivityCenterRespVO.Card.builder()
                .id(activity.getId())
                .activityName(activity.getActivityName())
                .activityType(activity.getActivityType())
                .platformCode(platformCode)
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

    private String normalizePlatformCode(String platformCode) {
        return HaodankuActivityVendorClient.normalizeActivityPlatformCode(platformCode);
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
