package com.qiji.cps.module.cps.service.membergoods;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsIdentityReqVO;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsRecordPageReqVO;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsRecordRespVO;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsRecordSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.membergoods.CpsMemberGoodsRecordDO;
import com.qiji.cps.module.cps.dal.mysql.membergoods.CpsMemberGoodsRecordMapper;
import com.qiji.cps.module.cps.enums.membergoods.CpsMemberGoodsRecordTypeEnum;
import com.qiji.cps.module.cps.service.rebate.asset.CpsMoneyConverter;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@Validated
public class CpsMemberGoodsRecordServiceImpl implements CpsMemberGoodsRecordService {

    static final int MAX_BROWSE_HISTORY_COUNT = 100;

    @Resource
    private CpsMemberGoodsRecordMapper recordMapper;

    @Resource
    private CpsMoneyConverter moneyConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordBrowse(Long memberId, AppCpsMemberGoodsRecordSaveReqVO reqVO) {
        upsertSnapshot(memberId, CpsMemberGoodsRecordTypeEnum.BROWSE, reqVO);
        pruneBrowseHistory(memberId);
    }

    @Override
    public PageResult<AppCpsMemberGoodsRecordRespVO> getBrowsePage(
            Long memberId, AppCpsMemberGoodsRecordPageReqVO reqVO) {
        return getMemberPage(memberId, CpsMemberGoodsRecordTypeEnum.BROWSE, reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanBrowseHistory(Long memberId) {
        recordMapper.deleteMemberRecords(memberId, CpsMemberGoodsRecordTypeEnum.BROWSE.getType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFavorite(Long memberId, AppCpsMemberGoodsRecordSaveReqVO reqVO) {
        upsertSnapshot(memberId, CpsMemberGoodsRecordTypeEnum.FAVORITE, reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFavorite(Long memberId, AppCpsMemberGoodsIdentityReqVO reqVO) {
        String identityKey = buildIdentityKey(reqVO.getPlatformCode(), reqVO.getGoodsId(), reqVO.getGoodsSign());
        recordMapper.deleteActiveByIdentity(memberId, CpsMemberGoodsRecordTypeEnum.FAVORITE.getType(), identityKey);
    }

    @Override
    public PageResult<AppCpsMemberGoodsRecordRespVO> getFavoritePage(
            Long memberId, AppCpsMemberGoodsRecordPageReqVO reqVO) {
        return getMemberPage(memberId, CpsMemberGoodsRecordTypeEnum.FAVORITE, reqVO);
    }

    private void upsertSnapshot(Long memberId, CpsMemberGoodsRecordTypeEnum recordType,
                                AppCpsMemberGoodsRecordSaveReqVO reqVO) {
        String platformCode = normalize(reqVO.getPlatformCode()).toLowerCase(Locale.ROOT);
        String goodsId = normalize(reqVO.getGoodsId());
        String goodsSign = normalize(reqVO.getGoodsSign());
        String identityKey = buildIdentityKey(platformCode, goodsId, goodsSign);
        CpsMemberGoodsRecordDO existing = recordMapper.selectActiveByIdentity(
                memberId, recordType.getType(), identityKey);
        CpsMemberGoodsRecordDO snapshot = buildSnapshot(memberId, recordType.getType(), platformCode,
                goodsId, goodsSign, identityKey, reqVO);
        if (existing != null) {
            snapshot.setId(existing.getId());
            recordMapper.updateById(snapshot);
            return;
        }
        try {
            recordMapper.insert(snapshot);
        } catch (DuplicateKeyException ex) {
            // 并发重复收藏/浏览时由数据库唯一键兜底，随后刷新已存在快照。
            CpsMemberGoodsRecordDO concurrent = recordMapper.selectActiveByIdentity(
                    memberId, recordType.getType(), identityKey);
            if (concurrent == null) {
                throw ex;
            }
            snapshot.setId(concurrent.getId());
            recordMapper.updateById(snapshot);
        }
    }

    private CpsMemberGoodsRecordDO buildSnapshot(Long memberId, String recordType, String platformCode,
                                                   String goodsId, String goodsSign, String identityKey,
                                                   AppCpsMemberGoodsRecordSaveReqVO reqVO) {
        return CpsMemberGoodsRecordDO.builder()
                .memberId(memberId)
                .recordType(recordType)
                .platformCode(platformCode)
                .goodsId(goodsId)
                .goodsSign(goodsSign)
                .identityKey(identityKey)
                .title(reqVO.getTitle())
                .mainPic(reqVO.getMainPic())
                .originalPriceCent(nullableYuanToCent(reqVO.getOriginalPrice()))
                .actualPriceCent(nullableYuanToCent(reqVO.getActualPrice()))
                .couponPriceCent(nullableYuanToCent(reqVO.getCouponPrice()))
                .estimateRebateAmountCent(nullableYuanToCent(reqVO.getEstimateRebateAmount()))
                .monthSales(reqVO.getMonthSales())
                .shopName(reqVO.getShopName())
                .build();
    }

    private void pruneBrowseHistory(Long memberId) {
        List<CpsMemberGoodsRecordDO> records = recordMapper.selectMemberList(
                memberId, CpsMemberGoodsRecordTypeEnum.BROWSE.getType());
        if (records.size() <= MAX_BROWSE_HISTORY_COUNT) {
            return;
        }
        records.subList(MAX_BROWSE_HISTORY_COUNT, records.size())
                .forEach(record -> recordMapper.deleteById(record.getId()));
    }

    private PageResult<AppCpsMemberGoodsRecordRespVO> getMemberPage(
            Long memberId, CpsMemberGoodsRecordTypeEnum recordType, AppCpsMemberGoodsRecordPageReqVO reqVO) {
        String platformCode = StringUtils.hasText(reqVO.getPlatformCode())
                ? normalize(reqVO.getPlatformCode()).toLowerCase(Locale.ROOT) : null;
        PageResult<CpsMemberGoodsRecordDO> page = recordMapper.selectMemberPage(
                reqVO, memberId, recordType.getType(), platformCode);
        List<AppCpsMemberGoodsRecordRespVO> list = page.getList().stream().map(this::toRespVO).toList();
        return new PageResult<>(list, page.getTotal());
    }

    private AppCpsMemberGoodsRecordRespVO toRespVO(CpsMemberGoodsRecordDO record) {
        AppCpsMemberGoodsRecordRespVO respVO = new AppCpsMemberGoodsRecordRespVO();
        respVO.setId(record.getId());
        respVO.setPlatformCode(record.getPlatformCode());
        respVO.setGoodsId(record.getGoodsId());
        respVO.setGoodsSign(record.getGoodsSign());
        respVO.setTitle(record.getTitle());
        respVO.setMainPic(record.getMainPic());
        respVO.setOriginalPrice(moneyConverter.centToYuan(record.getOriginalPriceCent()));
        respVO.setActualPrice(moneyConverter.centToYuan(record.getActualPriceCent()));
        respVO.setCouponPrice(moneyConverter.centToYuan(record.getCouponPriceCent()));
        respVO.setEstimateRebateAmount(moneyConverter.centToYuan(record.getEstimateRebateAmountCent()));
        respVO.setMonthSales(record.getMonthSales());
        respVO.setShopName(record.getShopName());
        respVO.setCreateTime(record.getCreateTime());
        respVO.setUpdateTime(record.getUpdateTime());
        return respVO;
    }

    private Long nullableYuanToCent(BigDecimal amountYuan) {
        return amountYuan == null ? null : moneyConverter.yuanToCent(amountYuan);
    }

    private String buildIdentityKey(String platformCode, String goodsId, String goodsSign) {
        String identity = normalize(platformCode).toLowerCase(Locale.ROOT) + "\n"
                + normalize(goodsId) + "\n" + normalize(goodsSign);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
