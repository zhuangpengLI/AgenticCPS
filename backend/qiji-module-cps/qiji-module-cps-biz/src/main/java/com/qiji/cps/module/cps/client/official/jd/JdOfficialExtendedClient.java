package com.qiji.cps.module.cps.client.official.jd;

import com.fasterxml.jackson.databind.JsonNode;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;

import java.util.Map;

/** 京东联盟中暂未绑定本地业务实体的营销及效果数据接口。 */
public interface JdOfficialExtendedClient {

    JsonNode createGiftCoupon(Map<String, Object> params, CpsVendorConfig config);

    JsonNode stopGiftCoupon(Map<String, Object> params, CpsVendorConfig config);

    JsonNode queryGiftCouponEffect(Map<String, Object> params, CpsVendorConfig config);

    JsonNode queryRedPacketEffect(Map<String, Object> params, CpsVendorConfig config);

    JsonNode queryPromotionEffect(Map<String, Object> params, CpsVendorConfig config);
}
