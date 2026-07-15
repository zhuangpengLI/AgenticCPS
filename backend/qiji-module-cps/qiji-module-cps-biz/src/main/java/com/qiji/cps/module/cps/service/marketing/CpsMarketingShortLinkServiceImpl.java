package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingShortLinkCreateReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingShortLinkPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingShortLinkDO;
import com.qiji.cps.module.cps.dal.mysql.marketing.CpsMarketingShortLinkMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
@Validated
public class CpsMarketingShortLinkServiceImpl implements CpsMarketingShortLinkService {

    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;
    private static final int SHORT_CODE_LENGTH = 12;
    private static final char[] SHORT_CODE_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();

    @Resource
    private CpsMarketingShortLinkMapper shortLinkMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    private Supplier<LocalDateTime> nowSupplier = LocalDateTime::now;

    @Override
    public CpsMarketingShortLinkDO createShortLink(CpsMarketingShortLinkCreateReqVO reqVO) {
        String memberAttributionHash = hashIfPresent(reqVO.getMemberAttributionKey());
        String requestHash = buildRequestHash(reqVO, memberAttributionHash);
        CpsMarketingShortLinkDO existing = shortLinkMapper.selectByRequestHash(requestHash);
        if (existing != null) {
            return existing;
        }

        CpsMarketingShortLinkDO shortLink = CpsMarketingShortLinkDO.builder()
                .shortCode(generateUniqueShortCode())
                .targetUrl(reqVO.getTargetUrl())
                .platformCode(reqVO.getPlatformCode())
                .vendorCode(reqVO.getVendorCode())
                .transferRecordId(reqVO.getTransferRecordId())
                .campaignId(reqVO.getCampaignId())
                .creativeId(reqVO.getCreativeId())
                .channelCode(reqVO.getChannelCode())
                .memberAttributionHash(memberAttributionHash)
                .requestHash(requestHash)
                .status(STATUS_ENABLED)
                .expireTime(reqVO.getExpireTime())
                .accessCount(0L)
                .build();
        shortLinkMapper.insert(shortLink);
        return shortLink;
    }

    @Override
    public PageResult<CpsMarketingShortLinkDO> getShortLinkPage(CpsMarketingShortLinkPageReqVO reqVO) {
        return shortLinkMapper.selectPage(reqVO);
    }

    @Override
    public String resolveTargetUrl(String shortCode) {
        if (!StringUtils.hasText(shortCode)) {
            return null;
        }
        CpsMarketingShortLinkDO shortLink = shortLinkMapper.selectByShortCode(shortCode);
        LocalDateTime now = nowSupplier.get();
        if (!isRedirectable(shortLink, now)) {
            return null;
        }
        shortLinkMapper.updateById(CpsMarketingShortLinkDO.builder()
                .id(shortLink.getId())
                .accessCount(defaultAccessCount(shortLink) + 1)
                .lastAccessTime(now)
                .build());
        return shortLink.getTargetUrl();
    }

    void setClockForTest(Supplier<LocalDateTime> nowSupplier) {
        this.nowSupplier = nowSupplier;
    }

    private String generateUniqueShortCode() {
        for (int i = 0; i < 8; i++) {
            String shortCode = generateShortCode();
            if (shortLinkMapper.selectByShortCode(shortCode) == null) {
                return shortCode;
            }
        }
        throw new IllegalStateException("Unable to allocate marketing short code");
    }

    private String generateShortCode() {
        StringBuilder builder = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            builder.append(SHORT_CODE_ALPHABET[secureRandom.nextInt(SHORT_CODE_ALPHABET.length)]);
        }
        return builder.toString();
    }

    private String buildRequestHash(CpsMarketingShortLinkCreateReqVO reqVO, String memberAttributionHash) {
        return sha256Hex(String.join("\u001F",
                blankToEmpty(reqVO.getTargetUrl()),
                blankToEmpty(reqVO.getPlatformCode()),
                blankToEmpty(reqVO.getVendorCode()),
                reqVO.getTransferRecordId() == null ? "" : reqVO.getTransferRecordId().toString(),
                blankToEmpty(reqVO.getCampaignId()),
                blankToEmpty(reqVO.getCreativeId()),
                blankToEmpty(reqVO.getChannelCode()),
                blankToEmpty(memberAttributionHash),
                reqVO.getExpireTime() == null ? "" : reqVO.getExpireTime().toString()));
    }

    private String hashIfPresent(String value) {
        return StringUtils.hasText(value) ? sha256Hex(value) : null;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private boolean isRedirectable(CpsMarketingShortLinkDO shortLink, LocalDateTime now) {
        if (shortLink == null || !StringUtils.hasText(shortLink.getTargetUrl())) {
            return false;
        }
        if (shortLink.getStatus() == null || shortLink.getStatus() == STATUS_DISABLED) {
            return false;
        }
        return shortLink.getExpireTime() == null || !shortLink.getExpireTime().isBefore(now);
    }

    private Long defaultAccessCount(CpsMarketingShortLinkDO shortLink) {
        return shortLink.getAccessCount() == null ? 0L : shortLink.getAccessCount();
    }

    private String blankToEmpty(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
