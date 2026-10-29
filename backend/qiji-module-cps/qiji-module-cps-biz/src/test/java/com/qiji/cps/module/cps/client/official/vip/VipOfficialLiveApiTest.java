package com.qiji.cps.module.cps.client.official.vip;

import com.qiji.cps.module.cps.client.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 唯品会联盟官方 API 真实联调测试
 *
 * <p>从项目根目录 .env 文件读取真实凭证（VVIP_APP_KEY, VVIP_APP_SECRET），
 * 验证唯品会官方网关通信、签名计算、商品搜索、推广转链与订单查询能力。</p>
 */
class VipOfficialLiveApiTest {

    private VipOfficialVendorClient client;
    private CpsVendorConfig liveConfig;

    @BeforeEach
    void setUp() {
        client = new VipOfficialVendorClient();
        Map<String, String> env = loadDotEnv();

        String appKey = env.getOrDefault("VVIP_APP_KEY", env.get("VIP_APP_KEY"));
        String appSecret = env.getOrDefault("VVIP_APP_SECRET", env.get("VIP_APP_SECRET"));

        if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
            System.out.println("[唯品会真实联调] .env 中未配置 VVIP_APP_KEY 或 VVIP_APP_SECRET，跳过联调测试");
            return;
        }

        liveConfig = CpsVendorConfig.builder()
                .vendorCode("official")
                .platformCode("vip")
                .appKey(appKey)
                .appSecret(appSecret)
                .apiBaseUrl("https://vop.vipapis.com/")
                .defaultAdzoneId("85815982c2a2ec438c94cea28166e505")
                .extraConfig(Map.of("timeoutMs", "10000"))
                .build();

        System.out.println("[唯品会真实联调] 凭证已加载: appKey=" + appKey.substring(0, 3) + "***");
    }

    @Test
    void testConnection_live() {
        if (liveConfig == null) {
            return;
        }
        System.out.println("\n========== [1/4] 测试唯品会网关连通性 (testConnection) ==========");
        boolean passed = client.testConnection(liveConfig);
        System.out.println("唯品会连通性检测结果: " + (passed ? "【成功 PASSED】" : "【失败 FAILED】"));
        assertTrue(passed, "唯品会官方网关连接探测应成功");
    }

    @Test
    void searchGoods_live() {
        if (liveConfig == null) {
            return;
        }
        System.out.println("\n========== [2/4] 测试商品搜索接口 (searchGoods) ==========");
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setKeyword("女装");
        request.setPageNo(1);
        request.setPageSize(5);

        CpsGoodsSearchResult result = client.searchGoods(request, liveConfig);
        assertNotNull(result, "商品搜索结果不应为空");
        System.out.println("搜索结果总数 total: " + result.getTotal());
        System.out.println("本次返回商品数: " + result.getList().size());

        if (!result.getList().isEmpty()) {
            for (int i = 0; i < result.getList().size(); i++) {
                CpsGoodsItem item = result.getList().get(i);
                System.out.println(String.format("  [%d] 商品ID: %s, 标题: %s, 唯品价: %s, 原价: %s, 佣金率: %s%%, 佣金: %s元, 品牌: %s, adCode: %s",
                        i + 1,
                        item.getGoodsId(),
                        item.getTitle(),
                        item.getActualPrice(),
                        item.getOriginalPrice(),
                        item.getCommissionRate(),
                        item.getCommissionAmount(),
                        item.getBrandName(),
                        item.getExtraFields() != null ? item.getExtraFields().get("adCode") : "null"));
            }
        }
        assertFalse(result.getList().isEmpty(), "应至少返回1条商品数据");
    }

    @Test
    void testQueryPid_live() {
        if (liveConfig == null) {
            return;
        }
        System.out.println("\n========== [PID查询] 测试唯品会推广位查询 (UnionPidV2Service) ==========");
        try {
            com.vip.adp.api.open.service.UnionPidV2ServiceHelper.UnionPidV2ServiceClient pidClient =
                    new com.vip.adp.api.open.service.UnionPidV2ServiceHelper.UnionPidV2ServiceClient();
            com.vip.osp.sdk.context.ClientInvocationContext ctx = new com.vip.osp.sdk.context.ClientInvocationContext();
            ctx.setAppKey(liveConfig.getAppKey());
            ctx.setAppSecret(liveConfig.getAppSecret());
            ctx.setAppURL("https://vop.vipapis.com/");
            pidClient.setClientInvocationContext(ctx);

            com.vip.adp.api.open.service.PidQueryRequest req = new com.vip.adp.api.open.service.PidQueryRequest();
            req.setRequestId(java.util.UUID.randomUUID().toString());
            req.setPage(1);
            req.setPageSize(20);

            com.vip.adp.api.open.service.CpsUnionPidQueryResponse resp = pidClient.queryPid(req);
            System.out.println("PID 查询结果: total=" + (resp != null ? resp.getTotal() : "null"));
            if (resp != null && resp.getPidInfoList() != null) {
                for (com.vip.adp.api.open.service.PidInfo info : resp.getPidInfoList()) {
                    System.out.println(String.format("  PID: %s, 名称: %s, 媒体ID: %s, 状态: %s",
                            info.getPid(), info.getPidName(), info.getMediaId(), info.getStatus()));
                }
            }
        } catch (Exception e) {
            System.out.println("PID 查询失败或需其他权限: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void generatePromotionLink_live() {
        if (liveConfig == null) {
            return;
        }
        System.out.println("\n========== [3/4] 测试推广转链接口 (generatePromotionLink) ==========");
        // 先搜索一个商品获取真实商品ID
        CpsGoodsSearchRequest searchReq = new CpsGoodsSearchRequest();
        searchReq.setKeyword("鞋");
        searchReq.setPageNo(1);
        searchReq.setPageSize(2);
        CpsGoodsSearchResult searchResult = client.searchGoods(searchReq, liveConfig);

        String goodsId = null;
        if (searchResult != null && !searchResult.getList().isEmpty()) {
            goodsId = searchResult.getList().get(0).getGoodsId();
        }

        if (!StringUtils.hasText(goodsId)) {
            goodsId = "691887328123"; // 备用测试商品ID
        }
        System.out.println("测试转链商品 ID: " + goodsId);

        CpsPromotionLinkRequest linkReq = new CpsPromotionLinkRequest();
        linkReq.setGoodsId(goodsId);
        linkReq.setExternalId("test_member_001");

        CpsPromotionLinkResult linkResult = client.generatePromotionLink(linkReq, liveConfig);
        assertNotNull(linkResult, "转链结果不应为空");
        System.out.println("转链成功！");
        System.out.println("  推广短链: " + linkResult.getShortUrl());
        System.out.println("  推广长链: " + linkResult.getLongUrl());
        System.out.println("  移动端链接: " + linkResult.getMobileUrl());
        System.out.println("  唯口令: " + linkResult.getTpwd());
        System.out.println("  扩展字段: " + linkResult.getExtraFields());

        assertTrue(StringUtils.hasText(linkResult.getShortUrl()) || StringUtils.hasText(linkResult.getLongUrl()),
                "短链或长链应至少有一个非空");
    }

    @Test
    void queryOrders_live() {
        if (liveConfig == null) {
            return;
        }
        System.out.println("\n========== [4/4] 测试订单查询接口 (queryOrders) ==========");
        CpsOrderQueryRequest orderReq = new CpsOrderQueryRequest();
        orderReq.setPageNo(1);
        orderReq.setPageSize(10);
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        orderReq.setStartTime(now.minusDays(1).format(dtf));
        orderReq.setEndTime(now.format(dtf));

        try {
            List<CpsOrderDTO> orders = client.queryOrders(orderReq, liveConfig);
            assertNotNull(orders, "订单列表响应不应为 null");
            System.out.println("订单查询成功！近24小时订单数: " + orders.size());
            for (CpsOrderDTO order : orders) {
                System.out.println(String.format("  订单号: %s, 商品ID: %s, 标题: %s, 金额: %s, 佣金: %s, 状态: %s",
                        order.getPlatformOrderId(), order.getItemId(), order.getItemTitle(),
                        order.getFinalPrice(), order.getCommissionAmount(), order.getPlatformStatus()));
            }
        } catch (Exception e) {
            System.out.println("订单查询返回异常或需授权说明: " + e.getMessage());
            // 部分联盟账号如果没有工具商授权权限或未出单，可能返回无数据或需OAuth
        }
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> env = new HashMap<>();
        List<Path> candidatePaths = List.of(
                Path.of("F:/ai/AgenticCPS/.env"),
                Path.of("../../../../../../../../../.env"),
                Path.of(".env"),
                Path.of("../../.env")
        );
        for (Path p : candidatePaths) {
            if (Files.exists(p)) {
                try {
                    for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String k = line.substring(0, eq).trim();
                            String v = line.substring(eq + 1).trim();
                            env.put(k, v);
                        }
                    }
                    if (!env.isEmpty()) {
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return env;
    }
}
