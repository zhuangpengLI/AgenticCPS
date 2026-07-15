package com.qiji.cps.module.pay.api.transfer;

import com.qiji.cps.module.pay.api.transfer.dto.PayTransferCreateReqDTO;
import com.qiji.cps.module.pay.api.transfer.dto.PayTransferCreateRespDTO;
import com.qiji.cps.module.pay.api.transfer.dto.PayTransferRespDTO;
import jakarta.validation.Valid;

/**
 * 转账单 API 接口
 *
 * @author jason
 */
public interface PayTransferApi {

    /**
     * 创建转账单
     *
     * @param reqDTO 创建请求
     * @return 创建结果
     */
    PayTransferCreateRespDTO createTransfer(@Valid PayTransferCreateReqDTO reqDTO);

    /**
     * 获得转账单
     *
     * @param id 转账单编号
     * @return 转账单
     */
    PayTransferRespDTO getTransfer(Long id);

    /**
     * 按支付应用和商户转账单号查询既有转账单，用于业务系统在本地绑定失败后的幂等恢复。
     */
    PayTransferRespDTO getTransfer(String appKey, String merchantTransferId);

}
