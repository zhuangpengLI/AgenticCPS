package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.dto.CpsContentParseResult;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CPS 商品链接/商品 ID 轻量解析器.
 *
 * <p>口令还原依赖平台/供应商能力，当前解析器只负责稳定的 URL 和 ID 解析。</p>
 *
 * @author CPS System
 */
public final class CpsContentParser {

    private static final String COMMAND_UNSUPPORTED_MESSAGE = "暂不支持该渠道口令自动解析，请粘贴商品链接或商品ID";
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s，。；；,]+");
    private static final Pattern JD_ITEM_PATH = Pattern.compile("/(\\d{5,})\\.html");
    private static final Pattern DOUYIN_PRODUCT_PATH = Pattern.compile("/(?:product|item)/(\\d{5,})");
    private static final Pattern PLAIN_ID = Pattern.compile("[A-Za-z0-9_-]{4,}");
    private static final String TAOBAO_ACCURATE_RETURN_PATH = "mos.m.taobao.com/union/accurate-return";
    private static final String TAOBAO_QUAN_DETAIL_PATH = "uland.taobao.com/quan/detail";

    private CpsContentParser() {
    }

    public static CpsContentParseResult parse(String platformCode, String originalContent) {
        if (!StringUtils.hasText(originalContent)) {
            return CpsContentParseResult.unsupported("EMPTY_CONTENT", "请输入商品链接、商品ID或口令");
        }
        String content = originalContent.trim();
        String url = extractUrl(content);
        if (StringUtils.hasText(url)) {
            return parseUrl(platformCode, url, null);
        }
        if (looksLikeCommand(content)) {
            return CpsContentParseResult.unsupported("COMMAND_UNSUPPORTED", COMMAND_UNSUPPORTED_MESSAGE);
        }
        if (PLAIN_ID.matcher(content).matches()) {
            return buildPlainIdResult(platformCode, content);
        }
        return CpsContentParseResult.unsupported("UNRECOGNIZED_CONTENT", "无法识别商品内容，请粘贴商品链接或商品ID");
    }

    private static String extractUrl(String content) {
        Matcher matcher = URL_PATTERN.matcher(content);
        return matcher.find() ? trimTrailingPunctuation(matcher.group()) : null;
    }

    private static String trimTrailingPunctuation(String url) {
        String result = url;
        while (result.endsWith(".") || result.endsWith(")") || result.endsWith("】") || result.endsWith("]")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static CpsContentParseResult parseUrl(String platformCode, String url, String sourceLink) {
        Map<String, String> params = parseQueryParams(url);
        if ("taobao".equals(platformCode) && url.contains(TAOBAO_ACCURATE_RETURN_PATH)
                && StringUtils.hasText(params.get("targetUrl"))) {
            CpsContentParseResult targetResult = parseUrl(platformCode, params.get("targetUrl"), url);
            if (Boolean.TRUE.equals(targetResult.getSupported())) {
                return targetResult;
            }
        }
        String goodsId = switch (platformCode) {
            case "taobao" -> firstNonBlank(params.get("id"), params.get("itemId"), params.get("item_id"));
            case "jd" -> firstNonBlank(params.get("sku"), params.get("skuId"), params.get("sku_id"), match(url, JD_ITEM_PATH));
            case "pdd" -> firstNonBlank(params.get("goods_id"), params.get("goodsId"), params.get("goods_sign"), params.get("goodsSign"));
            case "douyin" -> firstNonBlank(params.get("id"), params.get("item_id"), params.get("product_id"), match(url, DOUYIN_PRODUCT_PATH));
            default -> firstNonBlank(params.get("id"), params.get("item_id"), params.get("goods_id"));
        };
        String couponLink = isTaobaoCouponUrl(platformCode, url) ? url : null;
        if (!StringUtils.hasText(goodsId) && !StringUtils.hasText(couponLink)) {
            return CpsContentParseResult.unsupported("URL_PARSE_FAILED", "未能从商品链接中识别商品ID");
        }
        CpsContentParseResult.CpsContentParseResultBuilder builder = CpsContentParseResult.builder()
                .supported(true)
                .goodsId(goodsId)
                .itemLink(StringUtils.hasText(goodsId) ? url : null)
                .couponLink(couponLink)
                .sourceLink(sourceLink);
        if ("pdd".equals(platformCode)) {
            builder.goodsSign(goodsId);
        }
        return builder.build();
    }

    private static boolean isTaobaoCouponUrl(String platformCode, String url) {
        return "taobao".equals(platformCode) && url.contains(TAOBAO_QUAN_DETAIL_PATH);
    }

    private static CpsContentParseResult buildPlainIdResult(String platformCode, String content) {
        CpsContentParseResult.CpsContentParseResultBuilder builder = CpsContentParseResult.builder()
                .supported(true)
                .goodsId(content);
        if ("pdd".equals(platformCode)) {
            builder.goodsSign(content);
        }
        return builder.build();
    }

    private static Map<String, String> parseQueryParams(String url) {
        Map<String, String> params = new HashMap<>();
        int queryIndex = url.indexOf('?');
        if (queryIndex < 0 || queryIndex + 1 >= url.length()) {
            return params;
        }
        String query = url.substring(queryIndex + 1);
        int fragmentIndex = query.indexOf('#');
        if (fragmentIndex >= 0) {
            query = query.substring(0, fragmentIndex);
        }
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = decode(pair.substring(0, separator));
            String value = decode(pair.substring(separator + 1));
            params.put(key, value);
        }
        return params;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ignored) {
            return value;
        }
    }

    private static boolean looksLikeCommand(String content) {
        return content.contains("￥") || content.contains("¥") || content.contains("复制") || content.contains("口令");
    }

    private static String match(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

}
