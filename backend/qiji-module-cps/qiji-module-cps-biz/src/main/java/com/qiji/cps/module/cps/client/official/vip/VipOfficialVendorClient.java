package com.qiji.cps.module.cps.client.official.vip;

import com.fasterxml.jackson.databind.JsonNode;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorConfigField;
import com.qiji.cps.module.cps.client.CpsVendorConfigSchema;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.CpsVendorGovernancePolicy;
import com.qiji.cps.module.cps.client.common.AbstractOfficialVendorClient;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.vip.adp.api.open.service.*;
import com.vip.osp.sdk.context.ClientInvocationContext;
import com.vip.osp.sdk.exception.OspException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 唯品会联盟官方 API 供应商客户端（基于唯品会官方 Java SDK 2.0）
 *
 * <p>对接唯品会开放平台（VOP - vop.vip.com / vop.vipapis.com）：</p>
 * <ul>
 *   <li>商品服务：{@link UnionGoodsV2Service} (query / goodsListV2 / getByGoodsIdsV2)</li>
 *   <li>转链服务：{@link UnionUrlV2Service} (genByGoodsId / genByVIPUrl)</li>
 *   <li>订单服务：{@link UnionOrderV2Service} (orderList / orderListWithOauth)</li>
 *   <li>通信协议：唯品会官方 OSP / REST 协议，自动处理系统参数排序与 HMAC-MD5 签名</li>
 * </ul>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class VipOfficialVendorClient extends AbstractOfficialVendorClient {

    private static final String DEFAULT_API_BASE_URL = "https://vop.vipapis.com/";
    private static final String DEFAULT_OPEN_ID = "default_open_id";
    private static final String DEFAULT_CHAN_TAG = "default_pid";
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(SHANGHAI_ZONE);

    private UnionGoodsV2Service customGoodsService;
    private UnionUrlV2Service customUrlService;
    private UnionOrderV2Service customOrderService;
    private UnionPidV2Service customPidService;

    public VipOfficialVendorClient() {
    }

    /** 单元测试及依赖注入构造器 */
    public VipOfficialVendorClient(UnionGoodsV2Service customGoodsService,
                                  UnionUrlV2Service customUrlService,
                                  UnionOrderV2Service customOrderService) {
        this(customGoodsService, customUrlService, customOrderService, null);
    }

    public VipOfficialVendorClient(UnionGoodsV2Service customGoodsService,
                                  UnionUrlV2Service customUrlService,
                                  UnionOrderV2Service customOrderService,
                                  UnionPidV2Service customPidService) {
        this.customGoodsService = customGoodsService;
        this.customUrlService = customUrlService;
        this.customOrderService = customOrderService;
        this.customPidService = customPidService;
    }

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.VIP.getCode();
    }

    @Override
    public CpsVendorGovernancePolicy getGovernancePolicy() {
        return CpsVendorGovernancePolicy.builder()
                .timeoutMillis(10000)
                .rateLimitPerMinute(120)
                .circuitBreakerOpenMillis(10000)
                .metricsEnabled(true)
                .build();
    }

    @Override
    public Set<CpsVendorCapability> getCapabilities() {
        return EnumSet.of(
                CpsVendorCapability.GOODS_SEARCH,
                CpsVendorCapability.PROMOTION_LINK,
                CpsVendorCapability.ORDER_QUERY,
                CpsVendorCapability.CONNECTION_TEST
        );
    }

    @Override
    public CpsVendorConfigSchema getConfigSchema() {
        return new CpsVendorConfigSchema(List.of(
                CpsVendorConfigField.required("appKey", true),
                CpsVendorConfigField.required("appSecret", true),
                CpsVendorConfigField.optional("authToken", true),
                CpsVendorConfigField.optional("defaultAdzoneId", false),
                CpsVendorConfigField.optional("apiBaseUrl", false),
                CpsVendorConfigField.optional("timeoutMs", false)
        ));
    }

    @Override
    protected String getSearchApiPath() {
        return "com.vip.adp.api.open.service.UnionGoodsV2Service";
    }

    @Override
    protected String getPromotionLinkApiPath() {
        return "com.vip.adp.api.open.service.UnionUrlV2Service";
    }

    @Override
    protected String getOrderQueryApiPath() {
        return "com.vip.adp.api.open.service.UnionOrderV2Service";
    }

    @Override
    protected String getTestConnectionApiPath() {
        return "com.vip.adp.api.open.service.UnionGoodsV2Service";
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        if (config == null || !StringUtils.hasText(config.getAppKey()) || !StringUtils.hasText(config.getAppSecret())) {
            log.warn("[唯品会联盟官方] 缺少配置凭证: platformCode={}, appKey={}", getPlatformCode(), config != null ? config.getAppKey() : null);
            return buildEmptyResult(request);
        }

        try {
            UnionGoodsV2Service goodsService = createGoodsService(config);
            boolean hasOauth = StringUtils.hasText(config.getAuthToken());

            String chanTag = firstNonBlank(request.getAdzoneId(), config.getDefaultAdzoneId(), DEFAULT_CHAN_TAG);
            String openId = firstNonBlank(request.getExternalId(), DEFAULT_OPEN_ID);
            int page = request.getPageNo() != null && request.getPageNo() > 0 ? request.getPageNo() : 1;
            int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 20;

            String rawKeyword = request.getKeyword() != null ? request.getKeyword().trim() : "";
            String searchField = request.getSearchField() != null ? request.getSearchField().trim().toLowerCase() : "";

            // 1. 尝试以商品 ID 或商品链接直查（精准命中）
            String directGoodsId = resolveDirectGoodsId(searchField, rawKeyword);
            if (StringUtils.hasText(directGoodsId)) {
                List<GoodsInfo> directList = queryGoodsByIds(goodsService, hasOauth, List.of(directGoodsId), chanTag, openId);
                if (directList != null && !directList.isEmpty()) {
                    List<CpsGoodsItem> items = filterAndMapGoods(directList, request);
                    return CpsGoodsSearchResult.builder()
                            .list(items)
                            .total((long) items.size())
                            .pageNo(1)
                            .pageSize(pageSize)
                            .build();
                }
            }

            // 2. 判断是否属于默认检索（空或 "今日精选"）
            boolean isDefaultSearch = !StringUtils.hasText(rawKeyword) || "今日精选".equalsIgnoreCase(rawKeyword);

            List<GoodsInfo> goodsInfoList;
            Long total = null;

            if (isDefaultSearch) {
                // 如果选择或传入了具体类目（且不是 '0' / '全部'），按类目关键词检索
                String category = request.getCategoryId();
                if (StringUtils.hasText(category) && !"0".equals(category.trim())) {
                    QueryGoodsRequest queryReq = buildQueryGoodsRequest(category.trim(), page, pageSize, chanTag, openId, request);
                    GoodsInfoResponse response = hasOauth ? goodsService.queryWithOauth(queryReq) : goodsService.query(queryReq);
                    goodsInfoList = response != null ? response.getGoodsInfoList() : null;
                    if (response != null && response.getTotal() != null) {
                        total = response.getTotal().longValue();
                    }
                } else {
                    // 无关键词且无类目，拉取唯品会官方特卖精选推荐流（channelType=0）
                    GoodsInfoRequestV2 listReq = new GoodsInfoRequestV2();
                    listReq.setChannelType(0);
                    listReq.setPageSize(pageSize);
                    listReq.setOffset((long) (page - 1) * pageSize);
                    listReq.setRequestId(UUID.randomUUID().toString());
                    listReq.setChanTag(chanTag);
                    listReq.setOpenId(openId);
                    listReq.setRealCall(true);
                    listReq.setQueryCpsInfo(1);
                    listReq.setQueryStock(true);
                    listReq.setQueryActivity(true);
                    mapSortFieldV2(request.getSortType(), listReq);

                    GoodsInfoResponseV2 response = hasOauth ? goodsService.goodsListV2WithOauth(listReq) : goodsService.goodsListV2(listReq);
                    goodsInfoList = response != null ? response.getGoodsInfoList() : null;
                    if (goodsInfoList != null) {
                        total = Boolean.TRUE.equals(response != null ? response.getLastPage() : null)
                                ? (long) goodsInfoList.size() : 1000L;
                    }
                }
            } else {
                // 用户输入了明确关键词检索
                QueryGoodsRequest queryReq = buildQueryGoodsRequest(rawKeyword, page, pageSize, chanTag, openId, request);
                GoodsInfoResponse response = hasOauth ? goodsService.queryWithOauth(queryReq) : goodsService.query(queryReq);
                goodsInfoList = response != null ? response.getGoodsInfoList() : null;
                if (response != null && response.getTotal() != null) {
                    total = response.getTotal().longValue();
                }
            }

            if (goodsInfoList == null) {
                return buildEmptyResult(request);
            }

            List<CpsGoodsItem> items = filterAndMapGoods(goodsInfoList, request);
            long totalCount = total != null ? total : items.size();
            return CpsGoodsSearchResult.builder()
                    .list(items)
                    .total(totalCount)
                    .pageNo(page)
                    .pageSize(pageSize)
                    .build();

        } catch (OspException e) {
            log.error("[唯品会联盟官方] 商品搜索失败: returnCode={}, returnMessage={}", e.getReturnCode(), e.getReturnMessage(), e);
            throw CpsVendorException.upstreamRejected(getVendorCode(), getPlatformCode(), CpsVendorCapability.GOODS_SEARCH,
                    e.getReturnCode(), e.getReturnMessage());
        } catch (Exception e) {
            log.error("[唯品会联盟官方] 商品搜索异常: {}", e.getMessage(), e);
            return buildEmptyResult(request);
        }
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        if (config == null || !StringUtils.hasText(config.getAppKey()) || !StringUtils.hasText(config.getAppSecret())) {
            throw CpsVendorException.upstreamRejected(getVendorCode(), getPlatformCode(), CpsVendorCapability.PROMOTION_LINK,
                    "CONFIG_ERROR", "唯品会联盟官方 appKey 或 appSecret 未配置");
        }

        try {
            UnionUrlV2Service urlService = createUrlService(config);
            String chanTag = firstNonBlank(request.getAdzoneId(), config.getDefaultAdzoneId(), DEFAULT_CHAN_TAG);
            String openId = firstNonBlank(request.getExternalId(), request.getRelationId(), DEFAULT_OPEN_ID);
            String requestId = UUID.randomUUID().toString();
            String statParam = firstNonBlank(request.getExternalId(), request.getRelationId());
            boolean hasOauth = StringUtils.hasText(config.getAuthToken());
            String adCode = resolveAdCode(request, config, chanTag, openId, hasOauth);

            UrlGenResponse response = null;
            if (StringUtils.hasText(request.getGoodsId())) {
                List<String> goodsIdList = List.of(request.getGoodsId().trim());
                UrlGenByGoodsIdRequest extraReq = new UrlGenByGoodsIdRequest();
                extraReq.setOpenId(openId);
                extraReq.setRealCall(true);
                if (StringUtils.hasText(adCode)) {
                    extraReq.setAdCode(adCode);
                }

                response = hasOauth
                        ? urlService.genByGoodsIdWithOauth(goodsIdList, chanTag, requestId, statParam, true, true, true, extraReq)
                        : urlService.genByGoodsId(goodsIdList, chanTag, requestId, statParam, true, true, true, extraReq);
            } else if (StringUtils.hasText(request.getItemLink())) {
                List<String> urlList = List.of(request.getItemLink().trim());
                UrlGenRequest extraReq = new UrlGenRequest();
                extraReq.setOpenId(openId);
                extraReq.setRealCall(true);
                if (StringUtils.hasText(adCode)) {
                    extraReq.setAdCode(adCode);
                }

                response = hasOauth
                        ? urlService.genByVIPUrlWithOauth(urlList, chanTag, requestId, statParam, extraReq)
                        : urlService.genByVIPUrl(urlList, chanTag, requestId, statParam, extraReq);
            }

            if (response == null || response.getUrlInfoList() == null || response.getUrlInfoList().isEmpty()) {
                log.warn("[唯品会联盟官方] 转链未返回有效链接数据: request={}", request);
                return null;
            }

            UrlInfo urlInfo = response.getUrlInfoList().get(0);
            Map<String, Object> extra = new LinkedHashMap<>();
            if (StringUtils.hasText(urlInfo.getUlUrl())) {
                extra.put("ulUrl", urlInfo.getUlUrl());
            }
            if (StringUtils.hasText(urlInfo.getVipWxUrl())) {
                extra.put("vipWxUrl", urlInfo.getVipWxUrl());
            }
            if (StringUtils.hasText(urlInfo.getVipWxCode())) {
                extra.put("vipWxCode", urlInfo.getVipWxCode());
            }
            if (StringUtils.hasText(urlInfo.getVipQuickAppUrl())) {
                extra.put("vipQuickAppUrl", urlInfo.getVipQuickAppUrl());
            }
            if (StringUtils.hasText(urlInfo.getVipZfbUrl())) {
                extra.put("vipZfbUrl", urlInfo.getVipZfbUrl());
            }

            return CpsPromotionLinkResult.builder()
                    .shortUrl(urlInfo.getUrl())
                    .longUrl(firstNonBlank(urlInfo.getLongUrl(), urlInfo.getUrl()))
                    .mobileUrl(urlInfo.getDeeplinkUrl())
                    .tpwd(urlInfo.getOnlyCommand())
                    .extraFields(extra)
                    .rawPayload(toJsonString(urlInfo))
                    .build();

        } catch (OspException e) {
            log.error("[唯品会联盟官方] 转链失败: returnCode={}, returnMessage={}", e.getReturnCode(), e.getReturnMessage(), e);
            throw CpsVendorException.upstreamRejected(getVendorCode(), getPlatformCode(), CpsVendorCapability.PROMOTION_LINK,
                    e.getReturnCode(), e.getReturnMessage());
        } catch (Exception e) {
            log.error("[唯品会联盟官方] 转链异常: {}", e.getMessage(), e);
            throw CpsVendorException.upstreamRejected(getVendorCode(), getPlatformCode(), CpsVendorCapability.PROMOTION_LINK,
                    "CALL_EXCEPTION", e.getMessage());
        }
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request, CpsVendorConfig config) {
        if (config == null || !StringUtils.hasText(config.getAppKey()) || !StringUtils.hasText(config.getAppSecret())) {
            throw CpsVendorException.upstreamRejected(getVendorCode(), getPlatformCode(), CpsVendorCapability.ORDER_QUERY,
                    "CONFIG_ERROR", "唯品会联盟官方 appKey 或 appSecret 未配置");
        }

        try {
            UnionOrderV2Service orderService = createOrderService(config);
            OrderQueryModel queryModel = new OrderQueryModel();
            queryModel.setPage(request.getPageNo() != null && request.getPageNo() > 0 ? request.getPageNo() : 1);
            queryModel.setPageSize(request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 20);
            queryModel.setRequestId(UUID.randomUUID().toString());

            String chanTag = config.getDefaultAdzoneId();
            if (StringUtils.hasText(chanTag)) {
                queryModel.setChanTag(chanTag);
            }
            if (StringUtils.hasText(request.getStartTime())) {
                queryModel.setOrderTimeStart(toEpochMillis(request.getStartTime()));
            }
            if (StringUtils.hasText(request.getEndTime())) {
                queryModel.setOrderTimeEnd(toEpochMillis(request.getEndTime()));
            }
            if (request.getOrderStatus() != null) {
                queryModel.setStatus(mapQueryStatus(request.getOrderStatus()));
            }

            boolean hasOauth = StringUtils.hasText(config.getAuthToken());
            OrderResponse response = hasOauth
                    ? orderService.orderListWithOauth(queryModel)
                    : orderService.orderList(queryModel);

            if (response == null || response.getOrderInfoList() == null) {
                return Collections.emptyList();
            }

            List<CpsOrderDTO> orders = new ArrayList<>();
            for (OrderInfo orderInfo : response.getOrderInfoList()) {
                if (orderInfo == null) {
                    continue;
                }
                if (orderInfo.getDetailList() != null && !orderInfo.getDetailList().isEmpty()) {
                    for (OrderDetailInfo detail : orderInfo.getDetailList()) {
                        orders.add(toOrderDTO(orderInfo, detail));
                    }
                } else {
                    orders.add(toOrderDTO(orderInfo, null));
                }
            }
            return orders;

        } catch (OspException e) {
            log.error("[唯品会联盟官方] 订单查询失败: returnCode={}, returnMessage={}", e.getReturnCode(), e.getReturnMessage(), e);
            throw CpsVendorException.upstreamRejected(getVendorCode(), getPlatformCode(), CpsVendorCapability.ORDER_QUERY,
                    e.getReturnCode(), e.getReturnMessage());
        } catch (Exception e) {
            log.error("[唯品会联盟官方] 订单查询异常: {}", e.getMessage(), e);
            throw CpsVendorException.upstreamRejected(getVendorCode(), getPlatformCode(), CpsVendorCapability.ORDER_QUERY,
                    "CALL_EXCEPTION", e.getMessage());
        }
    }

    @Override
    public CpsOrderPageResult queryOrderPage(CpsOrderQueryRequest request, CpsVendorConfig config) {
        List<CpsOrderDTO> orders = queryOrders(request, config);
        int pageNo = request.getPageNo() != null && request.getPageNo() > 0 ? request.getPageNo() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 20;
        boolean hasMore = orders.size() >= pageSize;
        return CpsOrderPageResult.page(orders, hasMore ? pageNo + 1 : null, hasMore);
    }

    @Override
    public boolean testConnection(CpsVendorConfig config) {
        if (config == null || !StringUtils.hasText(config.getAppKey()) || !StringUtils.hasText(config.getAppSecret())) {
            log.warn("[唯品会联盟官方] 连接测试缺少必要配置参数");
            return false;
        }

        try {
            UnionGoodsV2Service goodsService = createGoodsService(config);
            QueryGoodsRequest req = new QueryGoodsRequest();
            req.setKeyword("手机");
            req.setPage(1);
            req.setPageSize(1);
            req.setRequestId(UUID.randomUUID().toString());
            req.setChanTag(firstNonBlank(config.getDefaultAdzoneId(), DEFAULT_CHAN_TAG));
            req.setOpenId(DEFAULT_OPEN_ID);
            req.setRealCall(true);

            GoodsInfoResponse response = StringUtils.hasText(config.getAuthToken())
                    ? goodsService.queryWithOauth(req)
                    : goodsService.query(req);

            return response != null;
        } catch (Exception e) {
            log.warn("[唯品会联盟官方] 官方连接测试未通过: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 模板方法实现与适配 ====================

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword() != null ? request.getKeyword() : "");
        params.put("page", request.getPageNo() != null ? request.getPageNo() : 1);
        params.put("pageSize", request.getPageSize() != null ? request.getPageSize() : 20);
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        return buildEmptyResult(request);
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("goodsId", request.getGoodsId() != null ? request.getGoodsId() : "");
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        return null;
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (request.getStartTime() != null) params.put("startTime", request.getStartTime());
        if (request.getEndTime() != null) params.put("endTime", request.getEndTime());
        params.put("page", request.getPageNo() != null ? request.getPageNo() : 1);
        params.put("pageSize", request.getPageSize() != null ? request.getPageSize() : 20);
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        return Collections.emptyList();
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        return Map.of("keyword", "手机", "page", 1, "pageSize", 1);
    }

    @Override
    protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        log.warn("[唯品会联盟官方] executeRequest 模板方法被直接调用: path={}", path);
        return null;
    }

    @Override
    protected boolean isSuccessResponse(JsonNode root) {
        return root != null && root.path("returnCode").asInt(-1) == 0;
    }

    // ==================== 官方 SDK 客户端工厂 ====================

    protected UnionGoodsV2Service createGoodsService(CpsVendorConfig config) {
        if (customGoodsService != null) {
            return customGoodsService;
        }
        UnionGoodsV2ServiceHelper.UnionGoodsV2ServiceClient client = new UnionGoodsV2ServiceHelper.UnionGoodsV2ServiceClient();
        client.setClientInvocationContext(createInvocationContext(config));
        return client;
    }

    protected UnionUrlV2Service createUrlService(CpsVendorConfig config) {
        if (customUrlService != null) {
            return customUrlService;
        }
        UnionUrlV2ServiceHelper.UnionUrlV2ServiceClient client = new UnionUrlV2ServiceHelper.UnionUrlV2ServiceClient();
        client.setClientInvocationContext(createInvocationContext(config));
        return client;
    }

    protected UnionOrderV2Service createOrderService(CpsVendorConfig config) {
        if (customOrderService != null) {
            return customOrderService;
        }
        UnionOrderV2ServiceHelper.UnionOrderV2ServiceClient client = new UnionOrderV2ServiceHelper.UnionOrderV2ServiceClient();
        client.setClientInvocationContext(createInvocationContext(config));
        return client;
    }

    protected UnionPidV2Service createPidService(CpsVendorConfig config) {
        if (customPidService != null) {
            return customPidService;
        }
        UnionPidV2ServiceHelper.UnionPidV2ServiceClient client = new UnionPidV2ServiceHelper.UnionPidV2ServiceClient();
        client.setClientInvocationContext(createInvocationContext(config));
        return client;
    }

    private String resolveAdCode(CpsPromotionLinkRequest request, CpsVendorConfig config,
                                 String chanTag, String openId, boolean hasOauth) {
        // 1. 若为商品转链且包含 goodsId，调用官方 getByGoodsIdsV2 接口获取该商品最新的专属物料 adCode
        if (StringUtils.hasText(request.getGoodsId())) {
            try {
                UnionGoodsV2Service goodsService = createGoodsService(config);
                GetByGoodsIdsRequest idsReq = new GetByGoodsIdsRequest();
                idsReq.setGoodsIds(List.of(request.getGoodsId().trim()));
                idsReq.setChanTag(chanTag);
                idsReq.setOpenId(openId);
                idsReq.setRealCall(true);
                idsReq.setQueryCpsInfo(1);
                idsReq.setRequestId(UUID.randomUUID().toString());

                List<GoodsInfo> list = hasOauth
                        ? goodsService.getByGoodsIdsV2WithOauth(idsReq)
                        : goodsService.getByGoodsIdsV2(idsReq);
                if (list != null && !list.isEmpty()) {
                    GoodsInfo goods = list.get(0);
                    if (goods != null && StringUtils.hasText(goods.getAdCode())) {
                        log.info("[唯品会联盟官方] 获取商品专属物料 adCode 成功: goodsId={}, adCode={}", request.getGoodsId(), goods.getAdCode());
                        return goods.getAdCode().trim();
                    }
                }
            } catch (Exception e) {
                log.warn("[唯品会联盟官方] 获取商品物料 adCode 异常: goodsId={}, err={}", request.getGoodsId(), e.getMessage());
            }
        }

        // 2. 若传入了 itemLink 且带有商品ID特征，尝试提取 goodsId 并获取 adCode
        if (StringUtils.hasText(request.getItemLink())) {
            String extractedGoodsId = extractGoodsIdFromLink(request.getItemLink());
            if (StringUtils.hasText(extractedGoodsId)) {
                try {
                    UnionGoodsV2Service goodsService = createGoodsService(config);
                    GetByGoodsIdsRequest idsReq = new GetByGoodsIdsRequest();
                    idsReq.setGoodsIds(List.of(extractedGoodsId.trim()));
                    idsReq.setChanTag(chanTag);
                    idsReq.setOpenId(openId);
                    idsReq.setRealCall(true);
                    idsReq.setQueryCpsInfo(1);
                    idsReq.setRequestId(UUID.randomUUID().toString());

                    List<GoodsInfo> list = hasOauth
                            ? goodsService.getByGoodsIdsV2WithOauth(idsReq)
                            : goodsService.getByGoodsIdsV2(idsReq);
                    if (list != null && !list.isEmpty()) {
                        GoodsInfo goods = list.get(0);
                        if (goods != null && StringUtils.hasText(goods.getAdCode())) {
                            log.info("[唯品会联盟官方] 从链接商品获取物料 adCode 成功: goodsId={}, adCode={}", extractedGoodsId, goods.getAdCode());
                            return goods.getAdCode().trim();
                        }
                    }
                } catch (Exception e) {
                    log.warn("[唯品会联盟官方] 从链接提取商品物料 adCode 失败: url={}, err={}", request.getItemLink(), e.getMessage());
                }
            }
        }

        // 3. 兜底尝试通过商品搜索接口获取一个通用有效 adCode
        try {
            UnionGoodsV2Service goodsService = createGoodsService(config);
            QueryGoodsRequest queryReq = new QueryGoodsRequest();
            queryReq.setKeyword("女装");
            queryReq.setPage(1);
            queryReq.setPageSize(1);
            queryReq.setRequestId(UUID.randomUUID().toString());
            queryReq.setChanTag(chanTag);
            queryReq.setOpenId(openId);
            queryReq.setRealCall(true);
            queryReq.setQueryCpsInfo(1);
            GoodsInfoResponse resp = hasOauth ? goodsService.queryWithOauth(queryReq) : goodsService.query(queryReq);
            if (resp != null && resp.getGoodsInfoList() != null && !resp.getGoodsInfoList().isEmpty()) {
                String code = resp.getGoodsInfoList().get(0).getAdCode();
                if (StringUtils.hasText(code)) {
                    log.info("[唯品会联盟官方] 兜底获取通用物料 adCode 成功: adCode={}", code);
                    return code.trim();
                }
            }
        } catch (Exception e) {
            log.warn("[唯品会联盟官方] 兜底获取通用物料 adCode 异常: {}", e.getMessage());
        }

        // 4. 若传入了 adzoneId，使用其作为最后保底
        String fallback = firstNonBlank(request.getAdzoneId(), config != null ? config.getDefaultAdzoneId() : null);
        if (StringUtils.hasText(fallback) && !"default_pid".equalsIgnoreCase(fallback.trim())) {
            return fallback.trim();
        }

        return null;
    }

    private String extractGoodsIdFromLink(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:detail-|goodsId=)(\\d+)").matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    protected ClientInvocationContext createInvocationContext(CpsVendorConfig config) {
        ClientInvocationContext ctx = new ClientInvocationContext();
        ctx.setAppKey(config.getAppKey());
        ctx.setAppSecret(config.getAppSecret());
        if (StringUtils.hasText(config.getAuthToken())) {
            ctx.setAccessToken(config.getAuthToken());
        }
        String appUrl = StringUtils.hasText(config.getApiBaseUrl())
                ? config.getApiBaseUrl()
                : DEFAULT_API_BASE_URL;
        if (!appUrl.endsWith("/")) {
            appUrl += "/";
        }
        ctx.setAppURL(appUrl);
        int timeout = resolveTimeout(config);
        ctx.setReadTimeOut(timeout);
        ctx.setConnectTimeOut(Math.min(timeout, 5000));
        return ctx;
    }

    private int resolveTimeout(CpsVendorConfig config) {
        if (config != null && config.getExtraConfig() != null) {
            String timeout = config.getExtraConfig().get("timeoutMs");
            if (StringUtils.hasText(timeout)) {
                try {
                    return Integer.parseInt(timeout.trim());
                } catch (Exception ignored) {
                }
            }
        }
        return 10000;
    }

    // ==================== 数据映射辅助方法 ====================

    private CpsGoodsItem toGoodsItem(GoodsInfo info) {
        BigDecimal originalPrice = parseDecimal(info.getMarketPrice());
        BigDecimal actualPrice = parseDecimal(info.getVipPrice());
        BigDecimal commissionRate = parseDecimal(info.getCommissionRate());
        BigDecimal commissionAmount = parseDecimal(info.getCommission());

        BigDecimal couponPrice = null;
        if (info.getExclusiveCoupon() != null && StringUtils.hasText(info.getExclusiveCoupon().getBuy())) {
            couponPrice = parseDecimal(info.getExclusiveCoupon().getBuy());
        } else if (info.getCouponInfo() != null && StringUtils.hasText(info.getCouponInfo().getFav())) {
            couponPrice = parseDecimal(info.getCouponInfo().getFav());
        }

        Map<String, Object> extra = new LinkedHashMap<>();
        if (StringUtils.hasText(info.getAdCode())) {
            extra.put("adCode", info.getAdCode());
        }
        if (info.getGoodsDetailPictures() != null && !info.getGoodsDetailPictures().isEmpty()) {
            extra.put("detailPics", info.getGoodsDetailPictures());
        }
        if (StringUtils.hasText(info.getCategoryName())) {
            extra.put("categoryName", info.getCategoryName());
        }

        return CpsGoodsItem.builder()
                .goodsId(info.getGoodsId())
                .platformCode(getPlatformCode())
                .vendorCode(getVendorCode())
                .title(info.getGoodsName())
                .sellingPoint(info.getGoodsDesc())
                .categoryName(info.getCategoryName())
                .mainPic(firstNonBlank(info.getGoodsMainPicture(), info.getGoodsThumbUrl()))
                .originalPrice(originalPrice)
                .actualPrice(actualPrice != null ? actualPrice : originalPrice)
                .couponPrice(couponPrice)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .brandName(info.getBrandName())
                .monthSales(parseSales(info.getProductSales(), info.getGoodsSoldNumDesc()))
                .itemLink(info.getDestUrl())
                .extraFields(extra)
                .rawPayload(toJsonString(info))
                .build();
    }

    private CpsOrderDTO toOrderDTO(OrderInfo orderInfo, OrderDetailInfo detail) {
        Integer platformStatus = mapOrderStatus(orderInfo, detail);
        boolean isRefund = (orderInfo.getStatus() != null && orderInfo.getStatus() == 0)
                || (detail != null && detail.getAfterSaleInfo() != null && !detail.getAfterSaleInfo().isEmpty());

        BigDecimal itemPrice = detail != null ? parseDecimal(detail.getGoodsFinalPrice()) : parseDecimal(orderInfo.getTotalCost());
        BigDecimal finalPrice = detail != null ? parseDecimal(detail.getGoodsFinalPrice()) : parseDecimal(orderInfo.getTotalCost());
        BigDecimal commissionRate = detail != null ? parseDecimal(detail.getCommissionRate()) : null;
        BigDecimal commissionAmount = detail != null
                ? parseDecimal(detail.getCommission(), detail.getCommissionTotalCost())
                : parseDecimal(orderInfo.getCommission());

        return CpsOrderDTO.builder()
                .platformOrderId(orderInfo.getOrderSn())
                .parentOrderId(orderInfo.getParentSn())
                .platformCode(getPlatformCode())
                .vendorCode(getVendorCode())
                .itemId(detail != null ? detail.getGoodsId() : null)
                .itemTitle(detail != null ? detail.getGoodsName() : null)
                .itemPic(detail != null ? detail.getGoodsThumb() : null)
                .itemPrice(itemPrice)
                .finalPrice(finalPrice)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .quantity(detail != null && detail.getGoodsCount() != null ? detail.getGoodsCount() : 1)
                .platformStatus(platformStatus)
                .orderTime(formatEpochMilli(orderInfo.getOrderTime()))
                .settleTime(formatEpochMilli(orderInfo.getSettledTime()))
                .adzoneId(firstNonBlank(orderInfo.getChannelTag(), orderInfo.getPid()))
                .externalId(firstNonBlank(orderInfo.getStatParam(), orderInfo.getOpenId()))
                .refundTag(isRefund ? 1 : 0)
                .extraFields(extractOrderExtra(orderInfo, detail))
                .rawPayload(toJsonString(orderInfo))
                .build();
    }

    private Integer mapOrderStatus(OrderInfo orderInfo, OrderDetailInfo detail) {
        if (orderInfo == null) {
            return null;
        }
        if (detail != null && detail.getAfterSaleInfo() != null && !detail.getAfterSaleInfo().isEmpty()) {
            return -1;
        }
        Short status = detail != null && detail.getStatus() != null ? detail.getStatus() : orderInfo.getStatus();
        if (status != null) {
            if (status == 0) {
                return -1;
            }
            if (status == 2) {
                return 4;
            }
        }
        if (orderInfo.getSettled() != null && orderInfo.getSettled() == 1) {
            return 4;
        }
        String subStatus = orderInfo.getOrderSubStatusName();
        if (StringUtils.hasText(subStatus)) {
            if (subStatus.contains("退") || subStatus.contains("失效") || subStatus.contains("不合格")) {
                return -1;
            }
            if (subStatus.contains("结算")) {
                return 4;
            }
            if (subStatus.contains("收") || subStatus.contains("签")) {
                return 2;
            }
        }
        return 1;
    }

    private Short mapQueryStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 4 -> (short) 2; // 已结算
            case 1, 2 -> (short) 1; // 待结算 / 已付款 / 已收货
            case -1, 6 -> (short) 0; // 不合格 / 失效 / 退款
            default -> status.shortValue();
        };
    }

    private Map<String, Object> extractOrderExtra(OrderInfo order, OrderDetailInfo detail) {
        Map<String, Object> extra = new LinkedHashMap<>();
        if (StringUtils.hasText(order.getOrderSubStatusName())) {
            extra.put("orderSubStatusName", order.getOrderSubStatusName());
        }
        if (order.getStatus() != null) {
            extra.put("officialStatus", String.valueOf(order.getStatus()));
        }
        if (order.getSettled() != null) {
            extra.put("settled", String.valueOf(order.getSettled()));
        }
        if (order.getLastUpdateTime() != null) {
            extra.put("lastUpdateTime", formatEpochMilli(order.getLastUpdateTime()));
        }
        if (detail != null) {
            if (StringUtils.hasText(detail.getBrandStoreName())) {
                extra.put("shopName", detail.getBrandStoreName());
            }
            if (StringUtils.hasText(detail.getSizeId())) {
                extra.put("sizeId", detail.getSizeId());
            }
            if (detail.getStatus() != null) {
                extra.put("detailStatus", String.valueOf(detail.getStatus()));
            }
        }
        return extra;
    }

    private String extractGoodsIdFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:detail-\\d+-|detail-|goodsId=)(\\d+)").matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        java.util.regex.Matcher numMatcher = java.util.regex.Pattern.compile("(\\d{8,25})").matcher(url);
        if (numMatcher.find()) {
            return numMatcher.group(1);
        }
        return null;
    }

    private String resolveDirectGoodsId(String searchField, String rawKeyword) {
        if (!StringUtils.hasText(rawKeyword)) {
            return null;
        }
        if ("goods_id".equals(searchField)) {
            return rawKeyword;
        }
        if ("link".equals(searchField) || rawKeyword.contains("vip.com") || rawKeyword.contains("detail-")) {
            String extracted = extractGoodsIdFromUrl(rawKeyword);
            return StringUtils.hasText(extracted) ? extracted : rawKeyword;
        }
        if (rawKeyword.matches("^\\d{8,25}$")) {
            return rawKeyword;
        }
        return null;
    }

    private List<GoodsInfo> queryGoodsByIds(UnionGoodsV2Service goodsService, boolean hasOauth,
                                            List<String> goodsIds, String chanTag, String openId) throws OspException {
        GetByGoodsIdsRequest idsReq = new GetByGoodsIdsRequest();
        idsReq.setGoodsIds(goodsIds);
        idsReq.setRequestId(UUID.randomUUID().toString());
        idsReq.setChanTag(chanTag);
        idsReq.setOpenId(openId);
        idsReq.setRealCall(true);
        idsReq.setQueryDetail(true);
        idsReq.setQueryCpsInfo(1);
        idsReq.setQueryStock(true);
        return hasOauth ? goodsService.getByGoodsIdsV2WithOauth(idsReq) : goodsService.getByGoodsIdsV2(idsReq);
    }

    private QueryGoodsRequest buildQueryGoodsRequest(String keyword, int page, int pageSize,
                                                     String chanTag, String openId, CpsGoodsSearchRequest request) {
        QueryGoodsRequest queryReq = new QueryGoodsRequest();
        queryReq.setKeyword(keyword);
        queryReq.setPage(page);
        queryReq.setPageSize(pageSize);
        queryReq.setRequestId(UUID.randomUUID().toString());
        queryReq.setChanTag(chanTag);
        queryReq.setOpenId(openId);
        queryReq.setRealCall(true);
        queryReq.setQueryCpsInfo(1);
        queryReq.setQueryStock(true);
        queryReq.setQueryActivity(true);
        if (request.getPriceLowerLimit() != null) {
            queryReq.setPriceStart(request.getPriceLowerLimit().toPlainString());
        }
        if (request.getPriceUpperLimit() != null) {
            queryReq.setPriceEnd(request.getPriceUpperLimit().toPlainString());
        }
        mapSortField(request.getSortType(), queryReq);
        return queryReq;
    }

    private List<CpsGoodsItem> filterAndMapGoods(List<GoodsInfo> goodsInfoList, CpsGoodsSearchRequest request) {
        List<CpsGoodsItem> items = new ArrayList<>();
        for (GoodsInfo info : goodsInfoList) {
            if (info == null) {
                continue;
            }
            CpsGoodsItem item = toGoodsItem(info);
            if (request.getPriceLowerLimit() != null && item.getActualPrice() != null
                    && item.getActualPrice().compareTo(request.getPriceLowerLimit()) < 0) {
                continue;
            }
            if (request.getPriceUpperLimit() != null && item.getActualPrice() != null
                    && item.getActualPrice().compareTo(request.getPriceUpperLimit()) > 0) {
                continue;
            }
            if (request.getMinCommissionRate() != null && item.getCommissionRate() != null
                    && item.getCommissionRate().compareTo(request.getMinCommissionRate()) < 0) {
                continue;
            }
            if (request.getMinMonthSales() != null && item.getMonthSales() != null
                    && item.getMonthSales() < request.getMinMonthSales()) {
                continue;
            }
            if (Integer.valueOf(1).equals(request.getHasCoupon())) {
                if (item.getCouponPrice() == null || item.getCouponPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
            }
            if (Boolean.TRUE.equals(request.getBrandOnly()) && !StringUtils.hasText(item.getBrandName())) {
                continue;
            }
            items.add(item);
        }
        return items;
    }

    private void mapSortField(Integer sortType, QueryGoodsRequest req) {
        if (sortType == null) {
            return;
        }
        switch (sortType) {
            case 1 -> { // 月热销
                req.setFieldName("SALES");
                req.setOrder(1);
            }
            case 2 -> { // 券后价升序
                req.setFieldName("PRICE");
                req.setOrder(0);
            }
            case 3 -> { // 券后价降序
                req.setFieldName("PRICE");
                req.setOrder(1);
            }
            case 4 -> { // 佣金比例降序
                req.setFieldName("COMMISSION");
                req.setOrder(1);
            }
            case 5 -> { // 领券量/折扣
                req.setFieldName("DISCOUNT");
                req.setOrder(0);
            }
            case 6 -> { // 最新上架
                req.setFieldName("COMMISSION");
                req.setOrder(1);
            }
            default -> {
            }
        }
    }

    private void mapSortFieldV2(Integer sortType, GoodsInfoRequestV2 req) {
        if (sortType == null) {
            return;
        }
        switch (sortType) {
            case 1 -> {
                req.setFieldName("SALES");
                req.setOrder(1);
            }
            case 2 -> {
                req.setFieldName("PRICE");
                req.setOrder(0);
            }
            case 3 -> {
                req.setFieldName("PRICE");
                req.setOrder(1);
            }
            case 4 -> {
                req.setFieldName("COMMISSION");
                req.setOrder(1);
            }
            case 5 -> {
                req.setFieldName("DISCOUNT");
                req.setOrder(0);
            }
            default -> {
            }
        }
    }

    private Long parseSales(String productSales, String soldDesc) {
        if (StringUtils.hasText(productSales)) {
            try {
                String digits = productSales.replaceAll("[^0-9]", "");
                if (StringUtils.hasText(digits)) {
                    return Long.parseLong(digits);
                }
            } catch (Exception ignored) {
            }
        }
        if (StringUtils.hasText(soldDesc)) {
            try {
                String digits = soldDesc.replaceAll("[^0-9]", "");
                if (StringUtils.hasText(digits)) {
                    return Long.parseLong(digits);
                }
            } catch (Exception ignored) {
            }
        }
        return 0L;
    }

    private BigDecimal parseDecimal(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                try {
                    return new BigDecimal(v.trim());
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private Long toEpochMillis(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(SHANGHAI_ZONE).toInstant().toEpochMilli();
        } catch (Exception ignored) {
            try {
                long numeric = Long.parseLong(value.trim());
                return numeric < 10000000000L ? numeric * 1000L : numeric;
            } catch (Exception e) {
                return null;
            }
        }
    }

    private String formatEpochMilli(Long epochMillis) {
        if (epochMillis == null || epochMillis <= 0) {
            return null;
        }
        long ms = epochMillis < 10000000000L ? epochMillis * 1000L : epochMillis;
        return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(ms));
    }

    private String toJsonString(Object obj) {
        if (obj == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return null;
    }

}
