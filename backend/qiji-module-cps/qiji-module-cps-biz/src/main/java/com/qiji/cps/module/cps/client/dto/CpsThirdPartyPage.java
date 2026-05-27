package com.qiji.cps.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 第三方接口统一分页结果。
 */
@Data
@Builder
public class CpsThirdPartyPage<T> {

    private CpsThirdPartyApiCategory category;

    private List<T> list;

    private Long total;

    private Integer pageNo;

    private Integer pageSize;

    private String nextPageId;

    private Map<String, Object> extraFields;

    private String rawPayload;

}
