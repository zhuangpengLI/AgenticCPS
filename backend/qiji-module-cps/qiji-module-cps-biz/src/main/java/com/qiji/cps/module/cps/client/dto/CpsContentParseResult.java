package com.qiji.cps.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

/**
 * CPS 商品内容解析结果 DTO.
 *
 * @author CPS System
 */
@Data
@Builder
public class CpsContentParseResult {

    /**
     * 是否解析成功或当前内容是否被支持.
     */
    private Boolean supported;

    /**
     * 平台商品 ID.
     */
    private String goodsId;

    /**
     * 平台商品 goodsSign.
     */
    private String goodsSign;

    /**
     * 商品原始链接.
     */
    private String itemLink;

    /**
     * 商品标题.
     */
    private String title;

    /**
     * 失败编码.
     */
    private String failureCode;

    /**
     * 失败原因.
     */
    private String failureReason;

    public static CpsContentParseResult unsupported(String failureCode, String failureReason) {
        return CpsContentParseResult.builder()
                .supported(false)
                .failureCode(failureCode)
                .failureReason(failureReason)
                .build();
    }

}
