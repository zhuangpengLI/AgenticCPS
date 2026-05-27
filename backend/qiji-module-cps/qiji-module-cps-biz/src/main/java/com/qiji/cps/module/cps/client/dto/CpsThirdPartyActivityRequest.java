package com.qiji.cps.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 第三方活动拉取请求。
 */
@Data
@Builder(toBuilder = true)
public class CpsThirdPartyActivityRequest {

    private String vendorCode;

    private String platformCode;

    private String keyword;

    private Integer pageNo;

    private Integer pageSize;

    private String categoryName;

}
