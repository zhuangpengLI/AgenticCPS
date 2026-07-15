package com.qiji.cps.module.cps.service.goods.coupon;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolPageReqVO;
import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolUsableReqVO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsCouponPoolDO;
import jakarta.validation.Valid;

import java.util.List;

public interface CpsCouponPoolService {

    Long saveCoupon(@Valid CpsCouponPoolSaveReqVO reqVO);

    PageResult<CpsCouponPoolDO> getCouponPage(@Valid CpsCouponPoolPageReqVO reqVO);

    List<CpsCouponPoolDO> listUsableCoupons(@Valid CpsCouponPoolUsableReqVO reqVO);
}
