package com.qiji.cps.module.cps.service.exchange;

import com.qiji.cps.module.cps.controller.openapi.rebate.vo.OpenApiCpsRebateBalanceRespVO;
import com.qiji.cps.module.cps.controller.openapi.rebate.vo.OpenApiCpsRebateFreezeReqVO;
import com.qiji.cps.module.cps.controller.openapi.rebate.vo.OpenApiCpsRebateFreezeRespVO;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangePreviewRespDTO;

import java.math.BigDecimal;

public interface CpsRebateTokenExchangeService {

    OpenApiCpsRebateBalanceRespVO getBalance(Long memberId);

    OpenApiCpsRebateFreezeRespVO freeze(OpenApiCpsRebateFreezeReqVO request);

    void unfreeze(String freezeId, String reason);

    void confirmDeduct(String freezeId, String exchangeOrderId);

    CpsAitokenExchangePreviewRespDTO preview(Long memberId, BigDecimal amount);

    CpsRebateTokenExchangeOrderDO submit(Long memberId, BigDecimal amount, String idempotencyKey);

    CpsRebateTokenExchangeOrderDO getExchangeOrder(String exchangeOrderNo);
}
