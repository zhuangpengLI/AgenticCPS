package com.qiji.cps.module.pay.convert.wallet;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.pay.controller.admin.wallet.vo.wallet.PayWalletRespVO;
import com.qiji.cps.module.pay.controller.app.wallet.vo.wallet.AppPayWalletRespVO;
import com.qiji.cps.module.pay.dal.dataobject.wallet.PayWalletDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PayWalletConvert {

    PayWalletConvert INSTANCE = Mappers.getMapper(PayWalletConvert.class);

    AppPayWalletRespVO convert(PayWalletDO bean);

    PayWalletRespVO convert02(PayWalletDO bean);

    PageResult<PayWalletRespVO> convertPage(PageResult<PayWalletDO> page);

}
