package com.qiji.cps.module.cps.service.order;

import java.util.List;

public interface CpsOrderClaimService {

    CpsOrderClaimResult claim(CpsOrderClaimCommand command);

    CpsOrderClaimResult review(CpsOrderClaimReviewCommand command);

    List<CpsOrderClaimResult> getMemberClaims(Long memberId, int limit);
}
