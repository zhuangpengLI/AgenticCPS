package com.qiji.cps.module.cps.client.official.vip;

import com.qiji.cps.module.cps.client.dto.*;
import org.junit.jupiter.api.Test;

public class VipOfficialRealCallTest {

    @Test
    void testRealCall() {
        VipOfficialVendorClient client = new VipOfficialVendorClient();
        CpsVendorConfig config = CpsVendorConfig.builder()
                .appKey("7d3535ab")
                .appSecret("A166F49822B0025E74C5AD3002E27651")
                .defaultAdzoneId("85815982c2a2ec438c94cea28166e505")
                .apiBaseUrl("https://vop.vipapis.com/")
                .build();

        System.out.println("=== Testing testConnection ===");
        boolean conn = client.testConnection(config);
        System.out.println("testConnection result: " + conn);

        System.out.println("=== Testing searchGoods(手机) ===");
        CpsGoodsSearchRequest req = new CpsGoodsSearchRequest();
        req.setKeyword("手机");
        req.setPageNo(1);
        req.setPageSize(20);

        try {
            CpsGoodsSearchResult res = client.searchGoods(req, config);
            System.out.println("Search total: " + res.getTotal());
            System.out.println("Search list size: " + (res.getList() != null ? res.getList().size() : 0));
            if (res.getList() != null && !res.getList().isEmpty()) {
                System.out.println("First item: " + res.getList().get(0).getTitle());
            }
        } catch (Exception e) {
            System.out.println("Search goods threw exception:");
            e.printStackTrace();
        }

        System.out.println("=== Testing searchGoods(今日精选 - empty keyword) ===");
        CpsGoodsSearchRequest req2 = new CpsGoodsSearchRequest();
        req2.setKeyword("今日精选");
        req2.setPageNo(1);
        req2.setPageSize(20);

        try {
            CpsGoodsSearchResult res2 = client.searchGoods(req2, config);
            System.out.println("Search2 total: " + res2.getTotal());
            System.out.println("Search2 list size: " + (res2.getList() != null ? res2.getList().size() : 0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
