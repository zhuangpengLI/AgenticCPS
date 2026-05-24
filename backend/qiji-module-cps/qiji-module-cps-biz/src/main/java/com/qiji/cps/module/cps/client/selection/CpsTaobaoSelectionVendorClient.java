package com.qiji.cps.module.cps.client.selection;

import com.qiji.cps.module.cps.client.dto.CpsGoodsSelectionMeta;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;

public interface CpsTaobaoSelectionVendorClient {

    CpsGoodsSelectionMeta getSelectionMeta(CpsVendorConfig config);

}
