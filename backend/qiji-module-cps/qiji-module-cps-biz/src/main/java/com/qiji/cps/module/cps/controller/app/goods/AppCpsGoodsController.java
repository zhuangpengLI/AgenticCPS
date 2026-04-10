package com.qiji.cps.module.cps.controller.app.goods;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.controller.app.goods.vo.*;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

/**
 * 用户 APP - CPS商品搜索与转链
 *
 * @author CPS System
 */
@Tag(name = "用户 APP - CPS商品搜索与转链")
@RestController
@RequestMapping("/cps/goods")
@Validated
public class AppCpsGoodsController {

    @Resource
    private CpsGoodsService cpsGoodsService;

    @GetMapping("/search")
    @Operation(summary = "搜索商品（单平台）")
    public CommonResult<AppCpsGoodsSearchResult> searchGoods(@Valid AppCpsGoodsSearchReqVO reqVO) {
        if (reqVO.getPlatformCode() == null) {
            // 聚合所有平台（比价模式）
            CpsGoodsSearchRequest request = buildSearchRequest(reqVO);
            List<CpsGoodsItem> items = cpsGoodsService.searchGoodsAllPlatforms(request);
            List<AppCpsGoodsRespVO> voList = items.stream()
                    .map(this::toGoodsRespVO)
                    .collect(Collectors.toList());
            return success(AppCpsGoodsSearchResult.builder()
                    .list(voList)
                    .total((long) voList.size())
                    .build());
        } else {
            // 单平台搜索
            CpsGoodsSearchRequest request = buildSearchRequest(reqVO);
            CpsGoodsSearchResult result = cpsGoodsService.searchGoods(reqVO.getPlatformCode(), request);
            List<AppCpsGoodsRespVO> voList = result.getList().stream()
                    .map(this::toGoodsRespVO)
                    .collect(Collectors.toList());
            return success(AppCpsGoodsSearchResult.builder()
                    .list(voList)
                    .total(result.getTotal())
                    .pageNo(result.getPageNo())
                    .pageSize(result.getPageSize())
                    .nextPageId(result.getNextPageId())
                    .build());
        }
    }

    @PostMapping("/link")
    @Operation(summary = "生成推广链接（转链）")
    public CommonResult<AppCpsLinkRespVO> generateLink(@Valid @RequestBody AppCpsLinkReqVO reqVO) {
        Long memberId = SecurityFrameworkUtils.getLoginUserId();
        CpsPromotionLinkResult result = cpsGoodsService.generatePromotionLink(
                reqVO.getPlatformCode(),
                reqVO.getGoodsId(),
                reqVO.getGoodsSign(),
                memberId,
                reqVO.getAdzoneId()
        );
        if (result == null) {
            return success(null);
        }
        AppCpsLinkRespVO vo = new AppCpsLinkRespVO();
        vo.setShortUrl(result.getShortUrl());
        vo.setLongUrl(result.getLongUrl());
        vo.setTpwd(result.getTpwd());
        vo.setMobileUrl(result.getMobileUrl());
        vo.setActualPrice(result.getActualPrice());
        vo.setCommissionRate(result.getCommissionRate());
        vo.setCouponInfo(result.getCouponInfo());
        return success(vo);
    }

    // ==================== 私有方法 ====================

    private CpsGoodsSearchRequest buildSearchRequest(AppCpsGoodsSearchReqVO reqVO) {
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setKeyword(reqVO.getKeyword());
        request.setPageNo(reqVO.getPageNo());
        request.setPageSize(reqVO.getPageSize());
        request.setSortType(reqVO.getSortType());
        request.setPriceLowerLimit(reqVO.getPriceLowerLimit());
        request.setPriceUpperLimit(reqVO.getPriceUpperLimit());
        request.setHasCoupon(reqVO.getHasCoupon());
        return request;
    }

    private AppCpsGoodsRespVO toGoodsRespVO(CpsGoodsItem item) {
        AppCpsGoodsRespVO vo = new AppCpsGoodsRespVO();
        vo.setGoodsId(item.getGoodsId());
        vo.setGoodsSign(item.getGoodsSign());
        vo.setPlatformCode(item.getPlatformCode());
        vo.setTitle(item.getTitle());
        vo.setMainPic(item.getMainPic());
        vo.setOriginalPrice(item.getOriginalPrice());
        vo.setActualPrice(item.getActualPrice());
        vo.setCouponPrice(item.getCouponPrice());
        vo.setCommissionRate(item.getCommissionRate());
        vo.setMonthSales(item.getMonthSales());
        vo.setShopName(item.getShopName());
        vo.setShopType(item.getShopType());
        vo.setItemLink(item.getItemLink());
        vo.setBrandName(item.getBrandName());
        return vo;
    }

}
