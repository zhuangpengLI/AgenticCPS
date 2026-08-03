package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsContentParseRequest;
import com.qiji.cps.module.cps.client.dto.CpsContentParseResult;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsRebateQueryRespVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * CPS 商品返利查询 Service 实现.
 *
 * @author CPS System
 */
@Slf4j
@Service
@Validated
public class CpsGoodsRebateQueryServiceImpl implements CpsGoodsRebateQueryService {

    @Resource
    private CpsGoodsService goodsService;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @Resource
    private CpsTransferRecordMapper transferRecordMapper;

    @Override
    public CpsGoodsRebateQueryRespVO queryRebate(CpsGoodsRebateQueryReqVO reqVO) {
        return platformClientFactory.withVendorCode(reqVO.getVendorCode(), () -> doQueryRebate(reqVO));
    }

    private CpsGoodsRebateQueryRespVO doQueryRebate(CpsGoodsRebateQueryReqVO reqVO) {
        CpsContentParseResult parseResult = parseContent(reqVO);
        if (!Boolean.TRUE.equals(parseResult.getSupported())) {
            return buildFailure("PARSE_FAILED", parseResult.getFailureReason());
        }
        if (!StringUtils.hasText(parseResult.getGoodsId()) && !StringUtils.hasText(parseResult.getGoodsSign())) {
            return buildFailure("PARSE_FAILED", "未能从商品内容中识别商品ID");
        }

        String usedAdzoneId = goodsService.resolvePromotionAdzoneId(
                reqVO.getPlatformCode(), reqVO.getMemberId(), reqVO.getAdzoneId());
        CpsPromotionLinkResult linkResult = goodsService.generatePromotionLink(
                reqVO.getPlatformCode(),
                parseResult.getGoodsId(),
                parseResult.getGoodsSign(),
                reqVO.getMemberId(),
                usedAdzoneId,
                reqVO.getVendorCode(),
                reqVO.getOriginalContent());
        if (linkResult == null) {
            return buildFailure("LINK_FAILED", "商品不可转链或供应商暂不可用，请稍后重试");
        }

        CpsTransferRecordDO record = buildTransferRecord(reqVO, parseResult, linkResult, usedAdzoneId);
        transferRecordMapper.insert(record);

        CpsGoodsRebateQueryRespVO response = new CpsGoodsRebateQueryRespVO();
        response.setParseStatus("SUCCESS");
        response.setParseMessage("解析成功");
        response.setGoods(buildGoods(reqVO.getPlatformCode(), parseResult, linkResult));
        response.setRebate(buildRebate(linkResult, usedAdzoneId,
                platformClientFactory.resolveActiveVendorCode(reqVO.getPlatformCode())));
        response.setLinks(buildLinks(linkResult));
        response.setTransferRecordId(record.getId());
        return response;
    }

    private CpsContentParseResult parseContent(CpsGoodsRebateQueryReqVO reqVO) {
        CpsContentParseResult result = CpsContentParser.parse(reqVO.getPlatformCode(), reqVO.getOriginalContent());
        if (Boolean.TRUE.equals(result.getSupported())) {
            return result;
        }
        try {
            CpsPlatformClient client = platformClientFactory.getRequiredClient(reqVO.getPlatformCode());
            CpsContentParseRequest parseRequest = new CpsContentParseRequest();
            parseRequest.setPlatformCode(reqVO.getPlatformCode());
            parseRequest.setOriginalContent(reqVO.getOriginalContent());
            CpsContentParseResult platformResult = client.parseContent(parseRequest);
            return platformResult == null ? result : platformResult;
        } catch (Exception e) {
            log.warn("[CpsGoodsRebateQuery] 平台 {} 解析内容失败: {}", reqVO.getPlatformCode(), e.getMessage());
            return result;
        }
    }

    private CpsGoodsRebateQueryRespVO buildFailure(String status, String message) {
        CpsGoodsRebateQueryRespVO response = new CpsGoodsRebateQueryRespVO();
        response.setParseStatus(status);
        response.setParseMessage(StringUtils.hasText(message) ? message : "处理失败，请稍后重试");
        return response;
    }

    private CpsTransferRecordDO buildTransferRecord(CpsGoodsRebateQueryReqVO reqVO,
                                                    CpsContentParseResult parseResult,
                                                    CpsPromotionLinkResult linkResult,
                                                    String usedAdzoneId) {
        return CpsTransferRecordDO.builder()
                .memberId(reqVO.getMemberId())
                .platformCode(reqVO.getPlatformCode())
                .originalContent(reqVO.getOriginalContent())
                .itemId(firstNonBlank(parseResult.getGoodsId(), parseResult.getGoodsSign()))
                .itemTitle(parseResult.getTitle())
                .promotionUrl(firstNonBlank(linkResult.getShortUrl(), linkResult.getMobileUrl(), linkResult.getLongUrl()))
                .taoCommand(linkResult.getTpwd())
                .adzoneId(usedAdzoneId)
                .status(1)
                .build();
    }

    private CpsGoodsRebateQueryRespVO.Goods buildGoods(String platformCode,
                                                       CpsContentParseResult parseResult,
                                                       CpsPromotionLinkResult linkResult) {
        CpsGoodsRebateQueryRespVO.Goods goods = new CpsGoodsRebateQueryRespVO.Goods();
        goods.setPlatformCode(platformCode);
        goods.setGoodsId(parseResult.getGoodsId());
        goods.setGoodsSign(parseResult.getGoodsSign());
        goods.setItemLink(parseResult.getItemLink());
        goods.setTitle(parseResult.getTitle());
        goods.setActualPrice(linkResult.getActualPrice());
        goods.setCouponInfo(linkResult.getCouponInfo());
        return goods;
    }

    private CpsGoodsRebateQueryRespVO.Rebate buildRebate(CpsPromotionLinkResult linkResult, String usedAdzoneId,
                                                         String usedVendorCode) {
        CpsGoodsRebateQueryRespVO.Rebate rebate = new CpsGoodsRebateQueryRespVO.Rebate();
        rebate.setCommissionRate(linkResult.getCommissionRate());
        rebate.setCommissionAmount(defaultAmount(linkResult.getCommissionAmount()));
        rebate.setEstimateRebateAmount(defaultAmount(linkResult.getCommissionAmount()));
        rebate.setUsedAdzoneId(usedAdzoneId);
        rebate.setUsedVendorCode(usedVendorCode);
        return rebate;
    }

    private CpsGoodsRebateQueryRespVO.Links buildLinks(CpsPromotionLinkResult linkResult) {
        CpsGoodsRebateQueryRespVO.Links links = new CpsGoodsRebateQueryRespVO.Links();
        links.setShortUrl(linkResult.getShortUrl());
        links.setLongUrl(linkResult.getLongUrl());
        links.setTpwd(linkResult.getTpwd());
        links.setMobileUrl(linkResult.getMobileUrl());
        return links;
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

}
