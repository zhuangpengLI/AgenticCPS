package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsContentParseRequest;
import com.qiji.cps.module.cps.client.dto.CpsContentParseResult;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsBatchTransferReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsBatchTransferRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsParseRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryRespVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Service
@Validated
public class CpsGoodsToolboxServiceImpl implements CpsGoodsToolboxService {

    private static final int MAX_BATCH_SIZE = 20;
    private static final String STATUS_SUCCESS = "SUCCESS";

    @Resource
    private CpsGoodsRebateQueryService goodsRebateQueryService;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @Override
    public CpsGoodsParseRespVO parseContent(CpsGoodsParseReqVO reqVO) {
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

    private CpsGoodsParseRespVO toParseResp(String platformCode, CpsContentParseResult result, String parseSource) {
        return CpsGoodsParseRespVO.builder()
                .platformCode(platformCode)
                .supported(Boolean.TRUE.equals(result.getSupported()))
                .goodsId(result.getGoodsId())
                .goodsSign(result.getGoodsSign())
                .itemLink(result.getItemLink())
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

    private record IndexedContent(int index, String value) {
    }

}
