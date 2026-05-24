package com.qiji.cps.module.cps.service.selection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.ai.dal.dataobject.model.AiModelDO;
import com.qiji.cps.module.ai.enums.model.AiModelTypeEnum;
import com.qiji.cps.module.ai.enums.model.AiPlatformEnum;
import com.qiji.cps.module.ai.service.model.AiModelService;
import com.qiji.cps.module.ai.util.AiUtils;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareGoodsRespVO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 选品 AI 推荐的稳定降级实现：规则评分决定排序，LLM 只允许补充文案。
 */
@Service
@Slf4j
public class CpsSelectionAiRecommendService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private AiModelService aiModelService;

    public List<RecommendedGoods> recommend(CpsSelectionThemeDO theme, List<CpsGoodsSquareGoodsRespVO> candidates,
                                            Integer limit) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        CpsSelectionRule rule = parseRule(theme == null ? null : theme.getRuleJson());
        int size = limit == null || limit <= 0 ? candidates.size() : Math.min(limit, candidates.size());
        List<RecommendedGoods> ranked = candidates.stream()
                .map(goods -> new RecommendedGoods(goods, score(goods, rule), buildReason(theme, goods, rule)))
                .sorted(Comparator.comparing(RecommendedGoods::getRecommendScore).reversed())
                .limit(size)
                .toList();
        Map<String, String> llmReasons = buildLlmReasons(theme, ranked);
        if (!llmReasons.isEmpty()) {
            ranked.forEach(item -> {
                String goodsId = item.getGoods() == null ? null : item.getGoods().getGoodsId();
                String reason = goodsId == null ? null : llmReasons.get(goodsId);
                if (StringUtils.hasText(reason)) {
                    item.setRecommendReason(reason);
                }
            });
        }
        return ranked;
    }

    private CpsSelectionRule parseRule(String ruleJson) {
        if (!StringUtils.hasText(ruleJson)) {
            return new CpsSelectionRule();
        }
        try {
            return objectMapper.readValue(ruleJson, CpsSelectionRule.class);
        } catch (Exception ignored) {
            return new CpsSelectionRule();
        }
    }

    private BigDecimal score(CpsGoodsSquareGoodsRespVO goods, CpsSelectionRule rule) {
        BigDecimal score = ZERO;
        score = score.add(value(goods.getCommissionAmount()).multiply(new BigDecimal("3.0")));
        score = score.add(value(goods.getCommissionRate()).multiply(new BigDecimal("1.2")));
        score = score.add(value(goods.getCouponPrice()).multiply(new BigDecimal("1.5")));
        score = score.add(BigDecimal.valueOf(goods.getMonthSales() == null ? 0 : Math.min(goods.getMonthSales(), 100000L))
                .divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP));
        if (StringUtils.hasText(goods.getActivityTag()) && rule.getActivityTags() != null
                && rule.getActivityTags().stream().anyMatch(tag -> goods.getActivityTag().contains(tag))) {
            score = score.add(new BigDecimal("20"));
        }
        if (rule.getPlatformWeights() != null && StringUtils.hasText(goods.getPlatformCode())) {
            score = score.add(value(rule.getPlatformWeights().get(goods.getPlatformCode())));
        }
        if (rule.getOnlyCoupon() != null && rule.getOnlyCoupon() && value(goods.getCouponPrice()).compareTo(ZERO) > 0) {
            score = score.add(new BigDecimal("8"));
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildReason(CpsSelectionThemeDO theme, CpsGoodsSquareGoodsRespVO goods, CpsSelectionRule rule) {
        StringBuilder reason = new StringBuilder();
        if (StringUtils.hasText(theme == null ? null : theme.getThemeName())) {
            reason.append("匹配").append(theme.getThemeName()).append("主题");
        } else {
            reason.append("匹配选品主题");
        }
        if (value(goods.getCommissionAmount()).compareTo(ZERO) > 0) {
            reason.append("，预估佣金").append(goods.getCommissionAmount()).append("元");
        }
        if (value(goods.getCouponPrice()).compareTo(ZERO) > 0) {
            reason.append("，优惠券").append(goods.getCouponPrice()).append("元");
        }
        if (goods.getMonthSales() != null && goods.getMonthSales() > 0) {
            reason.append("，近30天销量").append(goods.getMonthSales());
        }
        if (StringUtils.hasText(goods.getActivityTag())) {
            reason.append("，活动标签").append(goods.getActivityTag());
        }
        return reason.toString();
    }

    private Map<String, String> buildLlmReasons(CpsSelectionThemeDO theme, List<RecommendedGoods> ranked) {
        if (aiModelService == null || ranked.isEmpty()) {
            return Map.of();
        }
        try {
            AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
            ChatModel chatModel = aiModelService.getChatModel(model.getId());
            ChatResponse response = chatModel.call(buildPrompt(theme, ranked, model));
            String content = response == null || response.getResult() == null || response.getResult().getOutput() == null
                    ? null : response.getResult().getOutput().getText();
            if (!StringUtils.hasText(content)) {
                return Map.of();
            }
            return objectMapper.readValue(extractJsonObject(content), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception ex) {
            log.info("[buildLlmReasons][选品库 LLM 文案增强不可用，使用规则推荐文案] reason={}", ex.getMessage());
            return Map.of();
        }
    }

    private Prompt buildPrompt(CpsSelectionThemeDO theme, List<RecommendedGoods> ranked, AiModelDO model) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("你是 CPS 选品运营助手。只能基于输入商品事实生成中文推荐理由，禁止修改商品 ID、价格、佣金、销量等事实字段。输出必须是 JSON 对象，key 为 goodsId，value 为 30 字以内推荐理由。"));
        messages.add(new UserMessage(buildLlmUserMessage(theme, ranked)));
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
        ChatOptions options = AiUtils.buildChatOptions(platform, model.getModel(), model.getTemperature(), model.getMaxTokens());
        return new Prompt(messages, options);
    }

    private String buildLlmUserMessage(CpsSelectionThemeDO theme, List<RecommendedGoods> ranked) {
        StringBuilder content = new StringBuilder();
        content.append("主题：").append(theme == null ? "选品主题" : Objects.toString(theme.getThemeName(), "选品主题")).append('\n');
        content.append("商品：\n");
        ranked.forEach(item -> {
            CpsGoodsSquareGoodsRespVO goods = item.getGoods();
            content.append("- goodsId=").append(goods.getGoodsId())
                    .append(", title=").append(goods.getTitle())
                    .append(", platform=").append(goods.getPlatformCode())
                    .append(", price=").append(goods.getActualPrice())
                    .append(", coupon=").append(goods.getCouponPrice())
                    .append(", commission=").append(goods.getCommissionAmount())
                    .append(", sales=").append(goods.getMonthSales())
                    .append(", activity=").append(goods.getActivityTag())
                    .append('\n');
        });
        return content.toString();
    }

    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedGoods {
        private CpsGoodsSquareGoodsRespVO goods;
        private BigDecimal recommendScore;
        private String recommendReason;
    }
}
