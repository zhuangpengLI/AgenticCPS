package com.qiji.cps.module.cps.service.goods.coupon;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolPageReqVO;
import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.couponpool.vo.CpsCouponPoolUsableReqVO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsCouponPoolDO;
import com.qiji.cps.module.cps.dal.mysql.goods.CpsCouponPoolMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

@Service
@Validated
public class CpsCouponPoolServiceImpl implements CpsCouponPoolService {

    private static final String STATUS_VALID = "VALID";

    @Resource
    private CpsCouponPoolMapper couponPoolMapper;

    private Supplier<LocalDateTime> nowSupplier = LocalDateTime::now;

    @Override
    public Long saveCoupon(CpsCouponPoolSaveReqVO reqVO) {
        CpsCouponPoolDO coupon = BeanUtils.toBean(reqVO, CpsCouponPoolDO.class);
        if (coupon.getStatus() == null) {
            coupon.setStatus(STATUS_VALID);
        }
        if (coupon.getId() == null) {
            couponPoolMapper.insert(coupon);
        } else {
            couponPoolMapper.updateById(coupon);
        }
        return coupon.getId();
    }

    @Override
    public PageResult<CpsCouponPoolDO> getCouponPage(CpsCouponPoolPageReqVO reqVO) {
        return couponPoolMapper.selectPage(reqVO);
    }

    @Override
    public List<CpsCouponPoolDO> listUsableCoupons(CpsCouponPoolUsableReqVO reqVO) {
        LocalDateTime now = nowSupplier.get();
        List<CpsCouponPoolDO> coupons = couponPoolMapper.selectListByGoods(
                reqVO.getPlatformCode(), reqVO.getVendorCode(), reqVO.getExternalGoodsId(), reqVO.getGoodsSign());
        if (coupons == null) {
            return List.of();
        }
        return coupons
                .stream()
                .filter(coupon -> isUsable(coupon, now))
                .toList();
    }

    void setClockForTest(Supplier<LocalDateTime> nowSupplier) {
        this.nowSupplier = nowSupplier;
    }

    private boolean isUsable(CpsCouponPoolDO coupon, LocalDateTime now) {
        if (!STATUS_VALID.equals(coupon.getStatus())) {
            return false;
        }
        if (coupon.getStartTime() != null && coupon.getStartTime().isAfter(now)) {
            return false;
        }
        if (coupon.getEndTime() != null && coupon.getEndTime().isBefore(now)) {
            return false;
        }
        return coupon.getStockRemain() == null || coupon.getStockRemain() > 0;
    }
}
