package com.qiji.cps.module.cps.client.haina;

import com.qiji.cps.module.cps.client.haina.dto.HainaDecisionEvidence;
import com.qiji.cps.module.cps.client.haina.dto.HainaDecisionRequest;

public interface HainaDecisionClient {

    HainaDecisionEvidence collectEvidence(HainaDecisionRequest request);
}
