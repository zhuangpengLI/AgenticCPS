package com.qiji.cps.module.cps.service.didi;

import com.qiji.cps.module.cps.controller.openapi.didi.vo.DidiOrderCallbackReqVO;
import com.qiji.cps.module.cps.controller.openapi.didi.vo.DidiRewardCallbackReqVO;

public interface DidiCallbackService {
    boolean handleOrder(String appKey, String timestamp, String sign, String rawBody, DidiOrderCallbackReqVO request);
    boolean handleReward(String appKey, String timestamp, String sign, String rawBody, DidiRewardCallbackReqVO request);
}
