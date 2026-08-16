package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsCouponInfoVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsCouponInfo;
import com.qiji.cps.module.cps.client.dto.CpsContentParseRequest;
import com.qiji.cps.module.cps.client.dto.CpsContentParseResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsBatchTransferReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsBatchTransferRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsCashGiftPlanReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsCashGiftPlanRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsCouponQueryReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsCouponQueryRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsOwnershipCheckReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsOwnershipCheckRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareGoodsRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchRespVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.member.dal.dataobject.user.MemberUserDO;
import com.qiji.cps.module.member.service.user.MemberUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Validated
public class CpsGoodsToolboxServiceImpl implements CpsGoodsToolboxService {

    private static final int MAX_BATCH_SIZE = 20;
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String CHECK_MATCH = "MATCH";
    private static final String CHECK_MISMATCH = "MISMATCH";
    private static final String CHECK_NOT_FOUND = "NOT_FOUND";
    private static final String PLATFORM_TAOBAO = "taobao";
    private static final String VENDOR_DATAOKE = "dataoke";

    @Resource
    private CpsGoodsRebateQueryService goodsRebateQueryService;

    @Resource
    private CpsGoodsSquareService goodsSquareService;

    @Resource
    private CpsTransferRecordMapper transferRecordMapper;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @Resource
    private MemberUserService memberUserService;

    @Override
    public CpsGoodsParseRespVO parseContent(CpsGoodsParseReqVO reqVO) {
        return platformClientFactory.withVendorCode(reqVO.getVendorCode(), () -> doParseContent(reqVO));
    }

    private CpsGoodsParseRespVO doParseContent(CpsGoodsParseReqVO reqVO) {
        CpsContentParseResult localResult = CpsContentParser.parse(reqVO.getPlatformCode(), reqVO.getOriginalContent());
        if (Boolean.TRUE.equals(localResult.getSupported())) {
            return toParseResp(reqVO.getPlatformCode(), localResult, "local");
        }

        CpsContentParseResult platformResult = parseByPlatform(reqVO);
        if (Boolean.TRUE.equals(platformResult.getSupported())) {
            return toParseResp(reqVO.getPlatformCode(), platformResult, "platform");
        }
        return toParseResp(reqVO.getPlatformCode(), platformResult, "platform");
    }

    @Override
    public CpsGoodsBatchTransferRespVO batchTransfer(CpsGoodsBatchTransferReqVO reqVO) {
        List<IndexedContent> contents = normalizeContents(reqVO.getOriginalContents());
        if (contents.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("每次最多支持 20 条内容批量转链");
        }

        List<CpsGoodsBatchTransferRespVO.Item> items = new ArrayList<>(contents.size());
        int successCount = 0;
        for (IndexedContent content : contents) {
            CpsGoodsRebateQueryReqVO singleReqVO = new CpsGoodsRebateQueryReqVO();
            singleReqVO.setPlatformCode(reqVO.getPlatformCode());
            singleReqVO.setOriginalContent(content.value());
            singleReqVO.setMemberId(reqVO.getMemberId());
            singleReqVO.setVendorCode(reqVO.getVendorCode());
            singleReqVO.setAdzoneId(reqVO.getAdzoneId());

            CpsGoodsRebateQueryRespVO singleResp = goodsRebateQueryService.queryRebate(singleReqVO);
            boolean success = singleResp != null && STATUS_SUCCESS.equals(singleResp.getParseStatus());
            if (success) {
                successCount++;
            }
            items.add(toBatchItem(content, singleResp));
        }

        return CpsGoodsBatchTransferRespVO.builder()
                .items(items)
                .successCount(successCount)
                .failureCount(items.size() - successCount)
                .build();
    }

    @Override
    public CpsGoodsOwnershipCheckRespVO checkOwnership(CpsGoodsOwnershipCheckReqVO reqVO) {
        CpsContentParseResult parseResult = CpsContentParser.parse(reqVO.getPlatformCode(), reqVO.getOriginalContent());
        CpsTransferRecordDO record = findTransferRecord(reqVO, parseResult);
        if (record == null) {
            return CpsGoodsOwnershipCheckRespVO.builder()
                    .checkStatus(CHECK_NOT_FOUND)
                    .message("未找到匹配的转链记录")
                    .ownershipResult("未找到归属记录")
                    .platformCode(reqVO.getPlatformCode())
                    .itemId(parseResult.getGoodsId())
                    .mismatches(List.of("transferRecord"))
                    .build();
        }

        List<String> mismatches = new ArrayList<>();
        if (reqVO.getMemberId() != null && !Objects.equals(reqVO.getMemberId(), record.getMemberId())) {
            mismatches.add("memberId");
        }
        if (StringUtils.hasText(reqVO.getAdzoneId()) && !Objects.equals(reqVO.getAdzoneId(), record.getAdzoneId())) {
            mismatches.add("adzoneId");
        }
        if (StringUtils.hasText(reqVO.getPlatformCode()) && !Objects.equals(reqVO.getPlatformCode(), record.getPlatformCode())) {
            mismatches.add("platformCode");
        }
        MemberUserDO recordMember = record.getMemberId() == null ? null : memberUserService.getUser(record.getMemberId());

        return CpsGoodsOwnershipCheckRespVO.builder()
                .checkStatus(mismatches.isEmpty() ? CHECK_MATCH : CHECK_MISMATCH)
                .message(mismatches.isEmpty() ? "归属匹配" : "归属存在不一致项")
                .ownershipResult(resolveOwnershipResult(reqVO, mismatches))
                .platformCode(record.getPlatformCode())
                .itemId(record.getItemId())
                .itemTitle(record.getItemTitle())
                .transferRecordId(record.getId())
                .recordMemberId(record.getMemberId())
                .recordMemberNickname(recordMember == null ? null : recordMember.getNickname())
                .recordMemberMobile(recordMember == null ? null : recordMember.getMobile())
                .recordAdzoneId(record.getAdzoneId())
                .pid(record.getAdzoneId())
                .promotionUrl(record.getPromotionUrl())
                .taoCommand(record.getTaoCommand())
                .recordStatus(record.getStatus())
                .createTime(record.getCreateTime())
                .mismatches(mismatches)
                .build();
    }

    private String resolveOwnershipResult(CpsGoodsOwnershipCheckReqVO reqVO, List<String> mismatches) {
        if (mismatches.isEmpty()) {
            return reqVO.getMemberId() != null || StringUtils.hasText(reqVO.getAdzoneId())
                    ? "是您的淘口令" : "已找到归属记录";
        }
        return "不是您的淘口令";
    }

    @Override
    public CpsGoodsCouponQueryRespVO queryCoupons(CpsGoodsCouponQueryReqVO reqVO) {
        CpsGoodsCouponQueryRespVO couponInfoResp = queryVendorCouponInfo(reqVO);
        if (couponInfoResp != null) {
            return couponInfoResp;
        }

        CpsGoodsSquareSearchReqVO searchReqVO = new CpsGoodsSquareSearchReqVO();
        searchReqVO.setPlatformCode(reqVO.getPlatformCode());
        searchReqVO.setVendorCode(reqVO.getVendorCode());
        searchReqVO.setKeyword(resolveCouponKeyword(reqVO));
        searchReqVO.setPageNo(reqVO.getPageNo());
        searchReqVO.setPageSize(reqVO.getPageSize());
        searchReqVO.setHasCoupon(1);
        searchReqVO.setCouponAmountMin(reqVO.getCouponAmountMin());

        CpsGoodsSquareSearchRespVO searchResp = goodsSquareService.searchGoods(searchReqVO);
        long total = searchResp.getTotal() == null ? 0L : searchResp.getTotal();
        return CpsGoodsCouponQueryRespVO.builder()
                .platformCode(reqVO.getPlatformCode())
                .vendorCode(reqVO.getVendorCode())
                .keyword(searchReqVO.getKeyword())
                .list(searchResp.getList())
                .total(total)
                .pageNo(searchResp.getPageNo())
                .pageSize(searchResp.getPageSize())
                .summary(total > 0 ? "已找到 " + total + " 个有券商品" : "未找到符合条件的优惠券商品")
                .build();
    }

    private CpsGoodsCouponQueryRespVO queryVendorCouponInfo(CpsGoodsCouponQueryReqVO reqVO) {
        String vendorCode = StringUtils.hasText(reqVO.getVendorCode()) ? reqVO.getVendorCode()
                : platformClientFactory.resolveActiveVendorCode(reqVO.getPlatformCode());
        if (!StringUtils.hasText(vendorCode)) {
            return null;
        }
        CpsApiVendorClient vendorClient = platformClientFactory.getVendorClient(vendorCode, reqVO.getPlatformCode());
        if (!(vendorClient instanceof CpsCouponInfoVendorClient couponInfoVendorClient)) {
            return null;
        }
        CpsVendorConfig config = platformClientFactory.getVendorConfig(vendorCode, reqVO.getPlatformCode());
        CpsCouponInfo couponInfo = couponInfoVendorClient.queryCouponInfo(reqVO.getQueryText(), config);
        if (couponInfo == null || couponInfo.getCouponAmount() == null) {
            return null;
        }
        CpsGoodsSquareGoodsRespVO goods = toCouponGoods(reqVO, vendorCode, couponInfo);
        return CpsGoodsCouponQueryRespVO.builder()
                .platformCode(reqVO.getPlatformCode())
                .vendorCode(vendorCode)
                .keyword(reqVO.getQueryText())
                .list(List.of(goods))
                .total(1L)
                .pageNo(reqVO.getPageNo())
                .pageSize(reqVO.getPageSize())
                .summary("已找到 1 个优惠券")
                .build();
    }

    private CpsGoodsSquareGoodsRespVO toCouponGoods(CpsGoodsCouponQueryReqVO reqVO, String vendorCode,
                                                    CpsCouponInfo couponInfo) {
        CpsGoodsSquareGoodsRespVO goods = new CpsGoodsSquareGoodsRespVO();
        goods.setGoodsId(firstText(couponInfo.getCouponId(), reqVO.getQueryText()));
        goods.setPlatformCode(reqVO.getPlatformCode());
        goods.setVendorCode(vendorCode);
        goods.setTitle(reqVO.getPlatformCode() + " 优惠券 "
                + couponInfo.getCouponAmount().stripTrailingZeros().toPlainString() + " 元");
        goods.setCouponPrice(couponInfo.getCouponAmount());
        goods.setCouponConditions(couponInfo.getCouponConditions());
        goods.setCouponTotalNum(couponInfo.getCouponTotalNum());
        goods.setCouponRemainNum(couponInfo.getCouponRemainNum());
        goods.setCouponReceiveNum(couponInfo.getCouponReceiveNum());
        goods.setCouponStartTime(couponInfo.getCouponStartTime());
        goods.setItemLink(couponInfo.getCouponLink());
        goods.setSource(vendorCode + ":coupon-info");
        goods.setActivityTag(buildCouponActivityTag(couponInfo));
        goods.setCouponEndTime(couponInfo.getCouponEndTime());
        goods.setSellingPoint(buildCouponSellingPoint(couponInfo));
        return goods;
    }

    private String buildCouponActivityTag(CpsCouponInfo couponInfo) {
        if (couponInfo.getCouponRemainNum() == null) {
            return "优惠券";
        }
        return "剩余" + couponInfo.getCouponRemainNum() + "张";
    }

    private String buildCouponSellingPoint(CpsCouponInfo couponInfo) {
        List<String> parts = new ArrayList<>();
        if (couponInfo.getCouponConditions() != null) {
            parts.add("满" + couponInfo.getCouponConditions().stripTrailingZeros().toPlainString() + "可用");
        }
        if (couponInfo.getCouponReceiveNum() != null) {
            parts.add("已领" + couponInfo.getCouponReceiveNum() + "张");
        }
        if (couponInfo.getCouponStartTime() != null) {
            parts.add("开始" + couponInfo.getCouponStartTime());
        }
        return String.join("，", parts);
    }

    @Override
    public CpsGoodsCashGiftPlanRespVO planCashGift(CpsGoodsCashGiftPlanReqVO reqVO) {
        BigDecimal requiredBudget = reqVO.getGiftAmount()
                .multiply(BigDecimal.valueOf(reqVO.getTotalQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal budgetAmount = reqVO.getBudgetAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal budgetGap = requiredBudget.subtract(budgetAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        boolean budgetEnough = budgetGap.compareTo(BigDecimal.ZERO) == 0;

        List<String> warnings = new ArrayList<>();
        if (!budgetEnough) {
            warnings.add("预算不足，需补足 " + budgetGap + " 元");
        }
        if (reqVO.getStartTime() != null && reqVO.getEndTime() != null
                && !reqVO.getEndTime().isAfter(reqVO.getStartTime())) {
            warnings.add("结束时间必须晚于开始时间");
        }
        if (!"taobao".equals(reqVO.getPlatformCode())) {
            warnings.add("淘礼金真实发放仅适用于淘宝，当前仅生成运营计划");
        }

        return CpsGoodsCashGiftPlanRespVO.builder()
                .planStatus(warnings.isEmpty() ? "READY" : "RISK")
                .message("当前为计划配置，不调用真实淘礼金发放接口")
                .templateCode(reqVO.getTemplateCode())
                .campaignName(reqVO.getCampaignName())
                .budgetAmount(budgetAmount)
                .giftAmount(reqVO.getGiftAmount().setScale(2, RoundingMode.HALF_UP))
                .totalQuantity(reqVO.getTotalQuantity())
                .budgetGap(budgetGap)
                .budgetEnough(budgetEnough)
                .promotionContent(buildCashGiftCopy(reqVO, requiredBudget))
                .checklist(buildCashGiftChecklist())
                .warnings(warnings)
                .templates(cashGiftTemplates())
                .build();
    }

    private CpsContentParseResult parseByPlatform(CpsGoodsParseReqVO reqVO) {
        try {
            CpsPlatformClient client = platformClientFactory.getRequiredClient(reqVO.getPlatformCode());
            CpsContentParseRequest request = new CpsContentParseRequest();
            request.setPlatformCode(reqVO.getPlatformCode());
            request.setOriginalContent(reqVO.getOriginalContent());
            CpsContentParseResult result = client.parseContent(request);
            return result == null ? CpsContentParseResult.unsupported("PARSE_FAILED", "平台未返回解析结果") : result;
        } catch (Exception e) {
            return CpsContentParseResult.unsupported("PARSE_FAILED",
                    StringUtils.hasText(e.getMessage()) ? e.getMessage() : "平台解析失败，请稍后重试");
        }
    }

    private CpsTransferRecordDO findTransferRecord(CpsGoodsOwnershipCheckReqVO reqVO, CpsContentParseResult parseResult) {
        LambdaQueryWrapperX<CpsTransferRecordDO> wrapper = new LambdaQueryWrapperX<CpsTransferRecordDO>();
        if (reqVO.getTransferRecordId() != null) {
            wrapper.eq(CpsTransferRecordDO::getId, reqVO.getTransferRecordId());
        } else if (Boolean.TRUE.equals(parseResult.getSupported()) && StringUtils.hasText(parseResult.getGoodsId())) {
            wrapper.eq(CpsTransferRecordDO::getPlatformCode, reqVO.getPlatformCode());
            wrapper.eq(CpsTransferRecordDO::getItemId, parseResult.getGoodsId());
        } else if (StringUtils.hasText(reqVO.getOriginalContent())) {
            List<String> keywords = buildOwnershipKeywords(reqVO.getOriginalContent());
            wrapper.eq(CpsTransferRecordDO::getPlatformCode, reqVO.getPlatformCode());
            wrapper.and(query -> {
                for (int i = 0; i < keywords.size(); i++) {
                    if (i > 0) {
                        query.or();
                    }
                    String keyword = keywords.get(i);
                    query.like(CpsTransferRecordDO::getOriginalContent, keyword)
                            .or().like(CpsTransferRecordDO::getTaoCommand, keyword)
                            .or().like(CpsTransferRecordDO::getPromotionUrl, keyword);
                }
            });
        }
        wrapper.orderByDesc(CpsTransferRecordDO::getId);
        List<CpsTransferRecordDO> records = transferRecordMapper.selectList(wrapper);
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    private List<String> buildOwnershipKeywords(String originalContent) {
        String content = originalContent.trim();
        List<String> keywords = new ArrayList<>();
        keywords.add(content);
        addCommandKeyword(keywords, content, '￥');
        addCommandKeyword(keywords, content, '¥');
        return keywords;
    }

    private void addCommandKeyword(List<String> keywords, String content, char marker) {
        int start = content.indexOf(marker);
        if (start < 0) {
            return;
        }
        int end = content.indexOf(marker, start + 1);
        if (end <= start) {
            return;
        }
        String command = content.substring(start, end + 1);
        if (StringUtils.hasText(command) && !keywords.contains(command)) {
            keywords.add(command);
        }
    }

    private String resolveCouponKeyword(CpsGoodsCouponQueryReqVO reqVO) {
        CpsContentParseResult parseResult = CpsContentParser.parse(reqVO.getPlatformCode(), reqVO.getQueryText());
        if (Boolean.TRUE.equals(parseResult.getSupported())) {
            return firstText(parseResult.getGoodsId(), parseResult.getGoodsSign(), parseResult.getTitle(), reqVO.getQueryText());
        }
        return reqVO.getQueryText().trim();
    }

    private CpsGoodsParseRespVO toParseResp(String platformCode, CpsContentParseResult result, String parseSource) {
        return CpsGoodsParseRespVO.builder()
                .platformCode(platformCode)
                .supported(Boolean.TRUE.equals(result.getSupported()))
                .goodsId(result.getGoodsId())
                .goodsSign(result.getGoodsSign())
                .itemLink(result.getItemLink())
                .couponLink(result.getCouponLink())
                .sourceLink(result.getSourceLink())
                .title(result.getTitle())
                .parseSource(parseSource)
                .failureCode(result.getFailureCode())
                .failureReason(result.getFailureReason())
                .build();
    }

    private List<IndexedContent> normalizeContents(List<String> originalContents) {
        List<IndexedContent> contents = new ArrayList<>();
        if (originalContents == null) {
            return contents;
        }
        for (int i = 0; i < originalContents.size(); i++) {
            String value = originalContents.get(i);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            contents.add(new IndexedContent(i, value.trim()));
        }
        return contents;
    }

    private CpsGoodsBatchTransferRespVO.Item toBatchItem(IndexedContent content, CpsGoodsRebateQueryRespVO response) {
        if (response == null) {
            return CpsGoodsBatchTransferRespVO.Item.builder()
                    .inputIndex(content.index())
                    .originalContent(content.value())
                    .status("LINK_FAILED")
                    .message("转链失败，请稍后重试")
                    .build();
        }
        return CpsGoodsBatchTransferRespVO.Item.builder()
                .inputIndex(content.index())
                .originalContent(content.value())
                .status(response.getParseStatus())
                .message(response.getParseMessage())
                .goods(response.getGoods())
                .rebate(response.getRebate())
                .links(response.getLinks())
                .transferRecordId(response.getTransferRecordId())
                .promotionContent(buildPromotionContent(response))
                .build();
    }

    private String buildPromotionContent(CpsGoodsRebateQueryRespVO response) {
        if (!STATUS_SUCCESS.equals(response.getParseStatus()) || response.getLinks() == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        if (response.getGoods() != null && StringUtils.hasText(response.getGoods().getTitle())) {
            builder.append(response.getGoods().getTitle()).append('\n');
        }
        if (response.getGoods() != null && StringUtils.hasText(response.getGoods().getCouponInfo())) {
            builder.append(response.getGoods().getCouponInfo()).append('\n');
        }
        String url = firstText(response.getLinks().getShortUrl(), response.getLinks().getMobileUrl(), response.getLinks().getLongUrl());
        if (StringUtils.hasText(url)) {
            builder.append(url).append('\n');
        }
        if (StringUtils.hasText(response.getLinks().getTpwd())) {
            builder.append(response.getLinks().getTpwd());
        }
        return builder.toString().trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String buildCashGiftCopy(CpsGoodsCashGiftPlanReqVO reqVO, BigDecimal requiredBudget) {
        StringBuilder builder = new StringBuilder();
        builder.append(reqVO.getCampaignName()).append('\n');
        if (StringUtils.hasText(reqVO.getTitle())) {
            builder.append(reqVO.getTitle()).append('\n');
        }
        builder.append("限量 ").append(reqVO.getTotalQuantity()).append(" 份，每份 ")
                .append(reqVO.getGiftAmount().setScale(2, RoundingMode.HALF_UP)).append(" 元补贴").append('\n');
        builder.append("预计预算 ").append(requiredBudget).append(" 元，先到先得。");
        return builder.toString();
    }

    private List<String> buildCashGiftChecklist() {
        return List.of(
                "确认淘宝联盟账号和推广位已授权",
                "确认商品佣金、券后价和库存仍有效",
                "确认预算、份数、每人限领和活动时间",
                "真实发放前需接入官方或供应商淘礼金创建接口",
                "上线后按转链记录和订单归因复盘效果");
    }

    private List<CpsGoodsCashGiftPlanRespVO.Template> cashGiftTemplates() {
        return List.of(
                CpsGoodsCashGiftPlanRespVO.Template.builder()
                        .code("new-user")
                        .name("新人首单补贴")
                        .scene("拉新首购")
                        .suggestion("小额多份，建议每人限领 1 份")
                        .build(),
                CpsGoodsCashGiftPlanRespVO.Template.builder()
                        .code("flash-sale")
                        .name("限时爆品冲量")
                        .scene("短时间提升转化")
                        .suggestion("预算集中投放，活动时间控制在 2-4 小时")
                        .build(),
                CpsGoodsCashGiftPlanRespVO.Template.builder()
                        .code("private-domain")
                        .name("私域社群专享")
                        .scene("微信群、社群分发")
                        .suggestion("搭配口令和短链文案，控制每人限领份数")
                        .build());
    }

    private record IndexedContent(int index, String value) {
    }

}
