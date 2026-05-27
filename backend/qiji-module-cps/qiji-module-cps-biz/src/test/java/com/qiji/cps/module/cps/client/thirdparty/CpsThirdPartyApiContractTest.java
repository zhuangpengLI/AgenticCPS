package com.qiji.cps.module.cps.client.thirdparty;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyApiCategory;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CpsThirdPartyApiContractTest {

    @Test
    @DisplayName("standard DTOs keep normalized fields plus vendor extension payload")
    void standardDtos_keepExtraFieldsAndRawPayload() {
        CpsGoodsItem goods = CpsGoodsItem.builder()
                .goodsId("1001")
                .title("标准商品")
                .extraFields(Map.of("brand_id", 32, "source_act_id", "40"))
                .rawPayload("{\"brand_id\":32}")
                .build();
        CpsPromotionLinkResult link = CpsPromotionLinkResult.builder()
                .shortUrl("https://short.example/abc")
                .extraFields(Map.of("sid", "member-1"))
                .rawPayload("{\"sid\":\"member-1\"}")
                .build();
        CpsOrderDTO order = CpsOrderDTO.builder()
                .platformOrderId("O-1")
                .extraFields(Map.of("status_desc", "已付款"))
                .rawPayload("{\"status_desc\":\"已付款\"}")
                .build();

        assertEquals(32, goods.getExtraFields().get("brand_id"));
        assertEquals("member-1", link.getExtraFields().get("sid"));
        assertEquals("已付款", order.getExtraFields().get("status_desc"));
        assertEquals("{\"brand_id\":32}", goods.getRawPayload());
    }

    @Test
    @DisplayName("third-party category and page contracts express activity pull separately from goods and orders")
    void thirdPartyContracts_supportCategorizedPages() {
        CpsThirdPartyActivity activity = CpsThirdPartyActivity.builder()
                .sourceType("jutuike")
                .externalActivityId("jtk:7")
                .activityName("美团外卖")
                .platformCode("meituan")
                .billingType("CPS")
                .extraFields(Map.of("settlement_time", "T+1"))
                .build();

        CpsThirdPartyPage<CpsThirdPartyActivity> page = CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                .list(List.of(activity))
                .pageNo(1)
                .pageSize(20)
                .total(1L)
                .nextPageId("2")
                .build();

        assertEquals(CpsThirdPartyApiCategory.ACTIVITY_PULL, page.getCategory());
        assertEquals("jtk:7", page.getList().get(0).getExternalActivityId());
        assertEquals("T+1", page.getList().get(0).getExtraFields().get("settlement_time"));
    }
}
