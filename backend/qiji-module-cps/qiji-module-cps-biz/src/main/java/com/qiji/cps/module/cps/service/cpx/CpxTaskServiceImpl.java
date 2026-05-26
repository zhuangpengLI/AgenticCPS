package com.qiji.cps.module.cps.service.cpx;

import com.qiji.cps.framework.common.util.object.BeanUtils;
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
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxSettlementRecordDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTaskDO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxTrackingLinkDO;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxArticleMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxConversionMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxEventMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxPlatformProfileMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxSettlementRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxTaskMapper;
import com.qiji.cps.module.cps.dal.mysql.cpx.CpxTrackingLinkMapper;
import com.qiji.cps.module.cps.enums.CpxPromotionMethodEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
public class CpxTaskServiceImpl implements CpxTaskService {

    private static final DateTimeFormatter NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int DEFAULT_LIMIT = 20;

    @Resource
    private CpxTaskMapper taskMapper;
    @Resource
    private CpxTrackingLinkMapper trackingLinkMapper;
    @Resource
    private CpxEventMapper eventMapper;
    @Resource
    private CpxConversionMapper conversionMapper;
    @Resource
    private CpxArticleMapper articleMapper;
    @Resource
    private CpxPlatformProfileMapper platformProfileMapper;
    @Resource
    private CpxSettlementRecordMapper settlementRecordMapper;

    @Override
    public Long createTask(CpxTaskSaveReqVO createReqVO) {
        CpxPromotionMethodEnum method = CpxPromotionMethodEnum.of(createReqVO.getPromotionMethod());
        CpxTaskDO task = BeanUtils.toBean(createReqVO, CpxTaskDO.class);
        task.setTaskNo(generateNo("CPX"));
        task.setPromotionMethod(method.name());
        task.setStatus(createReqVO.getStatus() == null ? CpxTaskConstants.STATUS_DRAFT : createReqVO.getStatus());
        task.setPriority(createReqVO.getPriority() == null ? defaultPriority(method) : createReqVO.getPriority());
        task.setMemberRewardEnabled(defaultMemberRewardEnabled(method, createReqVO.getMemberRewardEnabled()));
        taskMapper.insert(task);
        return task.getId();
    }

    @Override
    public void updateTask(CpxTaskSaveReqVO updateReqVO) {
        CpxPromotionMethodEnum method = CpxPromotionMethodEnum.of(updateReqVO.getPromotionMethod());
        CpxTaskDO task = BeanUtils.toBean(updateReqVO, CpxTaskDO.class);
        task.setPromotionMethod(method.name());
        task.setMemberRewardEnabled(defaultMemberRewardEnabled(method, updateReqVO.getMemberRewardEnabled()));
        taskMapper.updateById(task);
    }

    @Override
    public CpxTaskDO getTask(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public List<CpxTaskDO> listAdminTasks(String keyword, String promotionMethod, Integer limit) {
        String method = StringUtils.hasText(promotionMethod) ? CpxPromotionMethodEnum.of(promotionMethod).name() : null;
        return taskMapper.selectAdminList(keyword, method).stream()
                .sorted(Comparator.comparingInt(this::cpsFirstPriority)
                        .thenComparing(task -> task.getPriority() == null ? Integer.MAX_VALUE : task.getPriority())
                        .thenComparing(task -> task.getId() == null ? 0L : task.getId(), Comparator.reverseOrder()))
                .limit(normalizeLimit(limit))
                .toList();
    }

    @Override
    public List<CpxTaskDO> listPublishedTasks(String keyword, String promotionMethod, Integer limit) {
        String method = StringUtils.hasText(promotionMethod) ? CpxPromotionMethodEnum.of(promotionMethod).name() : null;
        return taskMapper.selectPublishedList(keyword, method).stream()
                .sorted(Comparator.comparingInt(this::cpsFirstPriority)
                        .thenComparing(task -> task.getPriority() == null ? Integer.MAX_VALUE : task.getPriority())
                        .thenComparing(task -> task.getId() == null ? 0L : task.getId(), Comparator.reverseOrder()))
                .limit(normalizeLimit(limit))
                .toList();
    }

    @Override
    public CpxTrackingLinkDO generateTrackingLink(AppCpxTrackingLinkCreateReqVO createReqVO, Long trustedMemberId) {
        CpxTaskDO task = taskMapper.selectById(createReqVO.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("CPX task not found: " + createReqVO.getTaskId());
        }
        String trackingId = generateNo("CPX");
        String targetUrl = StringUtils.hasText(task.getLandingUrl())
                ? task.getLandingUrl()
                : "https://cpx.agentic.local/tasks/" + task.getId();
        String trackingUrl = appendTrackingId(targetUrl, trackingId);
        CpxTrackingLinkDO link = CpxTrackingLinkDO.builder()
                .trackingId(trackingId)
                .taskId(task.getId())
                .offerId(createReqVO.getOfferId())
                .materialId(createReqVO.getMaterialId())
                .platformCode(task.getPlatformCode())
                .promotionMethod(task.getPromotionMethod())
                .memberId(trustedMemberId)
                .adzoneId(createReqVO.getAdzoneId())
                .channelCode(createReqVO.getChannelCode())
                .targetUrl(targetUrl)
                .trackingUrl(trackingUrl)
                .status(CpxTaskConstants.STATUS_ONLINE)
                .build();
        trackingLinkMapper.insert(link);
        return link;
    }

    @Override
    public CpxEventDO recordEvent(CpxEventCreateReqVO createReqVO) {
        String idempotencyKey = buildIdempotencyKey(createReqVO);
        CpxEventDO existing = eventMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing;
        }
        CpxEventDO event = BeanUtils.toBean(createReqVO, CpxEventDO.class);
        event.setEventId(generateNo("EVT"));
        event.setPromotionMethod(CpxPromotionMethodEnum.of(createReqVO.getPromotionMethod()).name());
        event.setEventType(normalizeUpper(createReqVO.getEventType()));
        event.setIdempotencyKey(idempotencyKey);
        event.setEventTime(createReqVO.getEventTime() == null ? LocalDateTime.now() : createReqVO.getEventTime());
        event.setValidFlag(true);
        event.setStatus(CpxTaskConstants.STATUS_ONLINE);
        eventMapper.insert(event);
        return event;
    }

    @Override
    public Long createArticle(CpxArticleSaveReqVO createReqVO) {
        CpxArticleDO article = BeanUtils.toBean(createReqVO, CpxArticleDO.class);
        article.setPromotionMethod(StringUtils.hasText(createReqVO.getPromotionMethod())
                ? CpxPromotionMethodEnum.of(createReqVO.getPromotionMethod()).name()
                : null);
        article.setStatus(createReqVO.getStatus() == null ? CpxTaskConstants.STATUS_DRAFT : createReqVO.getStatus());
        article.setPublishTime(createReqVO.getPublishTime() == null ? LocalDateTime.now() : createReqVO.getPublishTime());
        articleMapper.insert(article);
        return article.getId();
    }

    @Override
    public void updateArticle(CpxArticleSaveReqVO updateReqVO) {
        CpxArticleDO article = BeanUtils.toBean(updateReqVO, CpxArticleDO.class);
        article.setPromotionMethod(StringUtils.hasText(updateReqVO.getPromotionMethod())
                ? CpxPromotionMethodEnum.of(updateReqVO.getPromotionMethod()).name()
                : null);
        articleMapper.updateById(article);
    }

    @Override
    public CpxArticleDO getArticle(Long id) {
        return articleMapper.selectById(id);
    }

    @Override
    public List<CpxArticleDO> listAdminArticles(String keyword, String category, String promotionMethod, Integer limit) {
        String method = StringUtils.hasText(promotionMethod) ? CpxPromotionMethodEnum.of(promotionMethod).name() : null;
        return articleMapper.selectAdminList(keyword, category, method).stream()
                .limit(normalizeLimit(limit))
                .toList();
    }

    @Override
    public List<CpxArticleDO> searchArticles(String keyword, String category, String promotionMethod, Integer limit) {
        String method = StringUtils.hasText(promotionMethod) ? CpxPromotionMethodEnum.of(promotionMethod).name() : null;
        return articleMapper.selectPublishedList(keyword, category, method).stream()
                .limit(normalizeLimit(limit))
                .toList();
    }

    @Override
    public Long createPlatformProfile(CpxPlatformProfileSaveReqVO createReqVO) {
        CpxPlatformProfileDO profile = BeanUtils.toBean(createReqVO, CpxPlatformProfileDO.class);
        profile.setStatus(createReqVO.getStatus() == null ? CpxTaskConstants.STATUS_ONLINE : createReqVO.getStatus());
        platformProfileMapper.insert(profile);
        return profile.getId();
    }

    @Override
    public void updatePlatformProfile(CpxPlatformProfileSaveReqVO updateReqVO) {
        CpxPlatformProfileDO profile = BeanUtils.toBean(updateReqVO, CpxPlatformProfileDO.class);
        platformProfileMapper.updateById(profile);
    }

    @Override
    public CpxPlatformProfileDO getPlatformProfile(Long id) {
        return platformProfileMapper.selectById(id);
    }

    @Override
    public List<CpxPlatformProfileDO> listPlatformProfiles() {
        return platformProfileMapper.selectAdminList();
    }

    @Override
    public List<CpxPlatformProfileDO> listEnabledPlatformProfiles() {
        return platformProfileMapper.selectEnabledList();
    }

    @Override
    public List<CpxConversionDO> listMemberConversions(Long memberId, Integer limit) {
        return conversionMapper.selectListByMemberId(memberId).stream()
                .limit(normalizeLimit(limit))
                .toList();
    }

    @Override
    public CpxDashboardRespVO getDashboardSummary() {
        List<CpxTaskDO> tasks = taskMapper.selectAdminList(null, null);
        List<CpxEventDO> events = eventMapper.selectList();
        List<CpxConversionDO> conversions = conversionMapper.selectList();
        List<CpxSettlementRecordDO> settlements = settlementRecordMapper.selectList();

        CpxDashboardRespVO summary = new CpxDashboardRespVO();
        summary.setTaskCount(tasks.size());
        summary.setOnlineTaskCount((int) tasks.stream()
                .filter(task -> CpxTaskConstants.STATUS_ONLINE == safeStatus(task.getStatus()))
                .count());
        summary.setTaskCountByMethod(tasks.stream()
                .collect(Collectors.groupingBy(task -> normalizeMethod(task.getPromotionMethod()),
                        LinkedHashMap::new, Collectors.counting())));
        summary.setImpressionCount(countValidEvents(events, CpxTaskConstants.EVENT_IMPRESSION));
        summary.setClickCount(countValidEvents(events, CpxTaskConstants.EVENT_CLICK));
        summary.setLeadCount(countValidEvents(events, CpxTaskConstants.EVENT_LEAD));
        summary.setActionCount(countValidEvents(events, CpxTaskConstants.EVENT_ACTION));
        summary.setConversionCount(conversions.size());
        summary.setSettlementCount(settlements.size());
        summary.setSettlementAmount(sumSettlementAmount(settlements, true));
        summary.setRewardAmount(sumSettlementAmount(settlements, false));
        return summary;
    }

    private boolean defaultMemberRewardEnabled(CpxPromotionMethodEnum method, Boolean configured) {
        if (configured != null) {
            return configured;
        }
        return method.isCpsPrimary();
    }

    private int defaultPriority(CpxPromotionMethodEnum method) {
        return method.isCpsPrimary() ? 1 : 20;
    }

    private int cpsFirstPriority(CpxTaskDO task) {
        return CpxPromotionMethodEnum.CPS.name().equals(task.getPromotionMethod()) ? 0 : 1;
    }

    private int safeStatus(Integer status) {
        return status == null ? CpxTaskConstants.STATUS_DRAFT : status;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, 100);
    }

    private String buildIdempotencyKey(CpxEventCreateReqVO createReqVO) {
        if (StringUtils.hasText(createReqVO.getIdempotencyKey())) {
            return createReqVO.getIdempotencyKey();
        }
        return String.join(":",
                "tenant",
                createReqVO.getPlatformCode(),
                CpxPromotionMethodEnum.of(createReqVO.getPromotionMethod()).name(),
                createReqVO.getSourceEventId());
    }

    private String generateNo(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return prefix + LocalDateTime.now().format(NO_TIME_FORMATTER) + suffix;
    }

    private String appendTrackingId(String targetUrl, String trackingId) {
        String separator = targetUrl.contains("?") ? "&" : "?";
        return targetUrl + separator + "tracking_id=" + URLEncoder.encode(trackingId, StandardCharsets.UTF_8);
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeMethod(String promotionMethod) {
        return StringUtils.hasText(promotionMethod)
                ? CpxPromotionMethodEnum.of(promotionMethod).name()
                : CpxPromotionMethodEnum.MIXED.name();
    }

    private Integer countValidEvents(List<CpxEventDO> events, String eventType) {
        return (int) events.stream()
                .filter(event -> Boolean.TRUE.equals(event.getValidFlag()))
                .filter(event -> eventType.equals(normalizeUpper(event.getEventType())))
                .count();
    }

    private Integer sumSettlementAmount(List<CpxSettlementRecordDO> settlements, boolean platformAmount) {
        return settlements.stream()
                .map(record -> platformAmount ? record.getAmount() : record.getRewardAmount())
                .filter(amount -> amount != null)
                .reduce(0, Integer::sum);
    }
}
