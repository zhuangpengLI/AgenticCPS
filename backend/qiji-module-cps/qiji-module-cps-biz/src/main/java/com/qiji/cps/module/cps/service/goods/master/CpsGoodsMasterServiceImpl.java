package com.qiji.cps.module.cps.service.goods.master;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsMasterPageReqVO;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsPriceSnapshotPageReqVO;
import com.qiji.cps.module.cps.controller.admin.goodsmaster.vo.CpsGoodsSourceMappingPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsMasterDO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsPriceSnapshotDO;
import com.qiji.cps.module.cps.dal.dataobject.goods.CpsGoodsSourceMappingDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.mysql.goods.CpsGoodsMasterMapper;
import com.qiji.cps.module.cps.dal.mysql.goods.CpsGoodsPriceSnapshotMapper;
import com.qiji.cps.module.cps.dal.mysql.goods.CpsGoodsSourceMappingMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.SELECTION_THEME_ITEM_NOT_EXISTS;

@Service
@Validated
public class CpsGoodsMasterServiceImpl implements CpsGoodsMasterService {

    private static final int STATUS_ENABLED = 1;
    private static final int MASTER_CODE_MAX_LENGTH = 64;

    @Resource
    private CpsSelectionThemeItemMapper selectionThemeItemMapper;

    @Resource
    private CpsGoodsMasterMapper goodsMasterMapper;

    @Resource
    private CpsGoodsSourceMappingMapper sourceMappingMapper;

    @Resource
    private CpsGoodsPriceSnapshotMapper priceSnapshotMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importSelectionItem(Long selectionItemId) {
        CpsSelectionThemeItemDO item = selectionThemeItemMapper.selectById(selectionItemId);
        if (item == null) {
            throw exception(SELECTION_THEME_ITEM_NOT_EXISTS);
        }

        CpsGoodsSourceMappingDO mapping = sourceMappingMapper.selectBySourceKey(
                item.getPlatformCode(), item.getVendorCode(), item.getGoodsId(), item.getGoodsSign());
        if (mapping == null) {
            CpsGoodsMasterDO master = buildMaster(item);
            goodsMasterMapper.insert(master);
            mapping = buildMapping(item, master.getId());
            sourceMappingMapper.insert(mapping);
        } else {
            mapping.setSourceTitle(item.getTitle());
            mapping.setSourceMainPic(item.getMainPic());
            mapping.setItemLink(item.getItemLink());
            mapping.setRawData(item.getRawData());
            mapping.setLastSnapshotTime(resolveSnapshotTime(item));
            sourceMappingMapper.updateById(mapping);
        }

        priceSnapshotMapper.insert(buildSnapshot(item, mapping));
        return mapping.getMasterId();
    }

    @Override
    public CpsGoodsMasterDO getGoodsMaster(Long id) {
        return goodsMasterMapper.selectById(id);
    }

    @Override
    public PageResult<CpsGoodsMasterDO> getGoodsMasterPage(CpsGoodsMasterPageReqVO reqVO) {
        return goodsMasterMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<CpsGoodsSourceMappingDO> getSourceMappingPage(CpsGoodsSourceMappingPageReqVO reqVO) {
        return sourceMappingMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<CpsGoodsPriceSnapshotDO> getPriceSnapshotPage(CpsGoodsPriceSnapshotPageReqVO reqVO) {
        return priceSnapshotMapper.selectPage(reqVO);
    }

    private CpsGoodsMasterDO buildMaster(CpsSelectionThemeItemDO item) {
        return CpsGoodsMasterDO.builder()
                .masterCode(buildMasterCode(item))
                .standardTitle(item.getTitle())
                .brandName(item.getBrandName())
                .categoryName(item.getCategoryName())
                .mainPic(item.getMainPic())
                .status(STATUS_ENABLED)
                .remark("selection-theme-item:" + item.getId())
                .build();
    }

    private CpsGoodsSourceMappingDO buildMapping(CpsSelectionThemeItemDO item, Long masterId) {
        return CpsGoodsSourceMappingDO.builder()
                .masterId(masterId)
                .platformCode(item.getPlatformCode())
                .vendorCode(item.getVendorCode())
                .externalGoodsId(item.getGoodsId())
                .goodsSign(item.getGoodsSign())
                .itemLink(item.getItemLink())
                .sourceTitle(item.getTitle())
                .sourceMainPic(item.getMainPic())
                .status(STATUS_ENABLED)
                .lastSnapshotTime(resolveSnapshotTime(item))
                .rawData(item.getRawData())
                .build();
    }

    private CpsGoodsPriceSnapshotDO buildSnapshot(CpsSelectionThemeItemDO item, CpsGoodsSourceMappingDO mapping) {
        return CpsGoodsPriceSnapshotDO.builder()
                .masterId(mapping.getMasterId())
                .sourceMappingId(mapping.getId())
                .platformCode(item.getPlatformCode())
                .vendorCode(item.getVendorCode())
                .externalGoodsId(item.getGoodsId())
                .goodsSign(item.getGoodsSign())
                .originalPrice(toCent(item.getOriginalPrice()))
                .actualPrice(toCent(item.getActualPrice()))
                .couponPrice(toCent(item.getCouponPrice()))
                .commissionRate(item.getCommissionRate())
                .commissionAmount(toCent(item.getCommissionAmount()))
                .monthSales(item.getMonthSales())
                .shopName(item.getShopName())
                .activityTag(item.getActivityTag())
                .snapshotTime(resolveSnapshotTime(item))
                .rawData(item.getRawData())
                .build();
    }

    private LocalDateTime resolveSnapshotTime(CpsSelectionThemeItemDO item) {
        return item.getSnapshotTime() != null ? item.getSnapshotTime() : LocalDateTime.now();
    }

    private Integer toCent(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private String buildMasterCode(CpsSelectionThemeItemDO item) {
        String raw = String.join("_",
                fallback(item.getPlatformCode(), "platform"),
                fallback(item.getVendorCode(), "vendor"),
                fallback(item.getGoodsId(), "goods"),
                fallback(item.getGoodsSign(), "none"));
        String code = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        code = code.replaceAll("^_+|_+$", "");
        if (code.length() > MASTER_CODE_MAX_LENGTH) {
            return code.substring(0, MASTER_CODE_MAX_LENGTH);
        }
        return code;
    }

    private String fallback(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
