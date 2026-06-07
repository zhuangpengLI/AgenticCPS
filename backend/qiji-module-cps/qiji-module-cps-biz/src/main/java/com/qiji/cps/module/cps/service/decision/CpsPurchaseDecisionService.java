package com.qiji.cps.module.cps.service.decision;

public interface CpsPurchaseDecisionService {

    CpsPurchaseDecisionResponse decide(CpsPurchaseDecisionRequest request, Long trustedMemberId);
}
