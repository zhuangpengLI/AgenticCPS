package com.qiji.cps.module.cps.client.selection;

import com.qiji.cps.module.cps.client.dto.CpsGoodsSelectionOption;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;

import java.util.List;

public interface CpsSearchAssistVendorClient {

    List<CpsGoodsSelectionOption> getHotKeywords(Integer type, CpsVendorConfig config);

    List<CpsGoodsSelectionOption> suggestKeywords(String keyword, Integer type, CpsVendorConfig config);

}
