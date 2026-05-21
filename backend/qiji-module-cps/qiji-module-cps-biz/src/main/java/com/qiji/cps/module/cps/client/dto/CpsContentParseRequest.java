package com.qiji.cps.module.cps.client.dto;

import lombok.Data;

/**
 * CPS 商品内容解析请求 DTO.
 *
 * @author CPS System
 */
@Data
public class CpsContentParseRequest {

    /**
     * 平台编码.
     */
    private String platformCode;

    /**
     * 原始商品链接、商品 ID 或口令.
     */
    private String originalContent;

}
